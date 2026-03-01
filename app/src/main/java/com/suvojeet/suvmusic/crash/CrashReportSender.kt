package com.suvojeet.suvmusic.crash

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.acra.ReportField
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom ACRA sender that saves the crash report to a file and opens
 * a share intent so the user can send it via Telegram or download it.
 */
class CrashReportSender : ReportSender {

    companion object {
        private const val TELEGRAM_USERNAME = "suvojeet_sengupta"
        private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
    }

    override fun send(context: Context, errorContent: CrashReportData) {
        val reportFile = writeCrashReportFile(context, errorContent)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            reportFile
        )

        val shareIntent = buildShareIntent(uri)
        val chooserIntent = Intent.createChooser(shareIntent, "Share crash log via…").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
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

            // App info
            section(writer, "App Info") {
                line("Package", data.getString(ReportField.PACKAGE_NAME))
                line("Version", data.getString(ReportField.APP_VERSION_NAME))
                line("Version Code", data.getString(ReportField.APP_VERSION_CODE))
            }

            // Device info
            section(writer, "Device Info") {
                line("Brand", data.getString(ReportField.BRAND))
                line("Phone Model", data.getString(ReportField.PHONE_MODEL))
                line("Product", data.getString(ReportField.PRODUCT))
                line("Android Version", data.getString(ReportField.ANDROID_VERSION))
                line("Build", data.getString(ReportField.BUILD))
                line("Total Mem", data.getString(ReportField.TOTAL_MEM_SIZE))
                line("Available Mem", data.getString(ReportField.AVAILABLE_MEM_SIZE))
            }

            // Crash details
            section(writer, "Crash Details") {
                line("Crash Date", data.getString(ReportField.USER_CRASH_DATE))
            }
            writer.appendLine()
            writer.appendLine("── Stack Trace ──")
            writer.appendLine(data.getString(ReportField.STACK_TRACE) ?: "N/A")
            writer.appendLine()

            // Logcat
            val logcat = data.getString(ReportField.LOGCAT)
            if (!logcat.isNullOrBlank()) {
                writer.appendLine("── Logcat ──")
                writer.appendLine(logcat)
            }

            writer.appendLine()
            writer.appendLine("═══════════════════════════════════════")
            writer.appendLine("  Please describe what you were doing")
            writer.appendLine("  when the app crashed, to help fix it.")
            writer.appendLine("  Telegram: @$TELEGRAM_USERNAME")
            writer.appendLine("═══════════════════════════════════════")
        }

        return file
    }

    private fun buildShareIntent(fileUri: Uri): Intent {
        val text = buildString {
            appendLine("SuvMusic Crash Report 🐛")
            appendLine("Please see the attached crash log.")
            appendLine()
            appendLine("Telegram: @$TELEGRAM_USERNAME")
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "SuvMusic Crash Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Try Telegram first; falls back to chooser if not installed
            try {
                `package` = TELEGRAM_PACKAGE
            } catch (_: Exception) {
                `package` = null
            }
        }
    }

    // ── helpers ──

    private inline fun section(
        writer: java.io.BufferedWriter,
        title: String,
        block: SectionScope.() -> Unit
    ) {
        writer.appendLine("── $title ──")
        SectionScope(writer).block()
        writer.appendLine()
    }

    private class SectionScope(private val w: java.io.BufferedWriter) {
        fun line(label: String, value: String?) {
            w.appendLine("  $label: ${value ?: "N/A"}")
        }
    }
}
