package com.suvojeet.suvmusic.crash

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.JsonObject
import com.suvojeet.suvmusic.data.SessionManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.acra.ReportField
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Sends redacted crash diagnostics through the same support endpoint used by
 * the in-app feedback and bug-report forms. If the device is offline or the
 * endpoint is unavailable, the existing share flow remains available.
 */
class CrashReportSender : ReportSender {

    companion object {
        private const val TELEGRAM_USERNAME = "suvojeet_sengupta"
        private const val FEEDBACK_URL = "https://feedback.suvojeetsengupta.in/api/feedback"
        // Keep the crash payload below common reverse-proxy request limits. The
        // complete report remains available through the attached local file.
        private const val MAX_DIAGNOSTICS_CHARS = 32_000
    }

    override fun send(context: Context, errorContent: CrashReportData) {
        val reportFile = writeCrashReportFile(context, errorContent)
        val sentDirectly = runCatching {
            sendToFeedbackEndpoint(context, errorContent, reportFile)
        }.getOrDefault(false)

        if (!sentDirectly) {
            shareCrashReport(context, reportFile)
        }
    }

    private fun sendToFeedbackEndpoint(
        context: Context,
        data: CrashReportData,
        reportFile: File
    ): Boolean {
        val identity = runCatching {
            val sessionManager = SessionManager(context.applicationContext)
            val account = sessionManager.getStoredAccounts().firstOrNull()
            val name = sessionManager.getCachedUserName() ?: account?.name
            name to account?.email
        }.getOrDefault(null to null)

        val diagnostics = redactSensitive(reportFile.readText().takeLast(MAX_DIAGNOSTICS_CHARS))
        val json = JsonObject().apply {
            addProperty("appName", "SuvMusic")
            addProperty("appPackage", context.packageName)
            addProperty("appVersion", data.getString(ReportField.APP_VERSION_NAME) ?: "unknown")
            addProperty("rating", 0)
            addProperty("category", "crash")
            addProperty("message", "Automatic crash report submitted by SuvMusic.")
            identity.first?.takeIf { it.isNotBlank() }?.let { addProperty("userName", it) }
            identity.second?.takeIf { it.isNotBlank() }?.let { addProperty("userEmail", it) }
            addProperty("deviceBrand", data.getString(ReportField.BRAND) ?: "unknown")
            addProperty("deviceModel", data.getString(ReportField.PHONE_MODEL) ?: "unknown")
            addProperty("osVersion", data.getString(ReportField.ANDROID_VERSION) ?: "unknown")
            addProperty("sdkVersion", android.os.Build.VERSION.SDK_INT)
            addProperty("diagnostics", diagnostics)
        }

        val request = Request.Builder()
            .url(FEEDBACK_URL)
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build()

        return client.newCall(request).execute().use { response -> response.isSuccessful }
    }

    private fun shareCrashReport(context: Context, reportFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            reportFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "SuvMusic crash report could not be sent automatically. Please share the attached report with support."
            )
            putExtra(Intent.EXTRA_SUBJECT, "SuvMusic Crash Report")
            clipData = ClipData.newRawUri("SuvMusic crash report", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Share crash report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    /**
     * Mask secret-looking values before diagnostics leave the device.
     */
    private fun redactSensitive(text: String): String {
        val pattern = Regex("(?i)(api[_-]?key|authorization|auth[_-]?token|token|secret|password|passwd|cookie|set-cookie|bearer)([\\\"'\\s:=]+)([^\\s\\\"',}&;]+)")
        return text.lineSequence().joinToString("\n") { line ->
            pattern.replace(line) { match ->
                "${match.groupValues[1]}${match.groupValues[2]}***REDACTED***"
            }
        }
    }

    private fun writeCrashReportFile(
        context: Context,
        data: CrashReportData
    ): File {
        val dir = File(context.cacheDir, "crash_logs").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "suvmusic_crash_$timestamp.txt")

        file.bufferedWriter().use { writer ->
            writer.appendLine("═══════════════════════════════════════")
            writer.appendLine("  SuvMusic Crash Report")
            writer.appendLine("  $timestamp")
            writer.appendLine("═══════════════════════════════════════")
            writer.appendLine()

            section(writer, "App Info") {
                line("Package", data.getString(ReportField.PACKAGE_NAME))
                line("Version", data.getString(ReportField.APP_VERSION_NAME))
                line("Version Code", data.getString(ReportField.APP_VERSION_CODE))
            }

            section(writer, "Device Info") {
                line("Brand", data.getString(ReportField.BRAND))
                line("Phone Model", data.getString(ReportField.PHONE_MODEL))
                line("Product", data.getString(ReportField.PRODUCT))
                line("Android Version", data.getString(ReportField.ANDROID_VERSION))
                line("Build", data.getString(ReportField.BUILD))
                line("Total Mem", data.getString(ReportField.TOTAL_MEM_SIZE))
                line("Available Mem", data.getString(ReportField.AVAILABLE_MEM_SIZE))
            }

            section(writer, "Crash Details") {
                line("Crash Date", data.getString(ReportField.USER_CRASH_DATE))
            }
            writer.appendLine()
            writer.appendLine("── Stack Trace ──")
            writer.appendLine(redactSensitive(data.getString(ReportField.STACK_TRACE) ?: "N/A"))
            writer.appendLine()

            data.getString(ReportField.LOGCAT)?.takeIf { it.isNotBlank() }?.let { logcat ->
                writer.appendLine("── Logcat ──")
                writer.appendLine(redactSensitive(logcat))
            }

            writer.appendLine()
            writer.appendLine("═══════════════════════════════════════")
            writer.appendLine("  This report was prepared for SuvMusic support.")
            writer.appendLine("  Telegram fallback: @$TELEGRAM_USERNAME")
            writer.appendLine("═══════════════════════════════════════")
        }

        return file
    }

    private inline fun section(
        writer: java.io.BufferedWriter,
        title: String,
        block: SectionScope.() -> Unit
    ) {
        writer.appendLine("── $title ──")
        SectionScope(writer).block()
        writer.appendLine()
    }

    private class SectionScope(private val writer: java.io.BufferedWriter) {
        fun line(label: String, value: String?) {
            writer.appendLine("  $label: ${value ?: "N/A"}")
        }
    }
}
