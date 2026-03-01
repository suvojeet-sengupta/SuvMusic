package com.suvojeet.suvmusic.crash

import android.content.Context
import org.acra.config.CoreConfiguration
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory

/**
 * Factory that provides ACRA with our custom [CrashReportSender].
 * Registered via META-INF/services so ACRA discovers it automatically.
 */
class CrashReportSenderFactory : ReportSenderFactory {
    override fun create(context: Context, config: CoreConfiguration): ReportSender {
        return CrashReportSender()
    }

    override fun enabled(config: CoreConfiguration): Boolean = true
}
