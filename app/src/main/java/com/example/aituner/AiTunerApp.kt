package com.example.aituner

import android.app.Application
import android.content.Intent
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.security.Security
import java.text.SimpleDateFormat
import java.util.*

@HiltAndroidApp
class AiTunerApp : Application() {

    companion object {
        var crashFile: File? = null
            private set
        var debugFile: File? = null
            private set

        fun readLastCrash(): String? {
            return crashFile?.let {
                if (it.exists()) it.readText().takeLast(8000) else null
            }
        }

        /** 写入调试日志 */
        fun logDebug(tag: String, msg: String) {
            try {
                val line = "${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())} [$tag] $msg\n"
                debugFile?.appendText(line)
            } catch (_: Exception) {}
        }

        /** 读取调试日志 */
        fun readDebugLog(): String? {
            return debugFile?.let {
                if (it.exists()) it.readText().takeLast(6000) else null
            }
        }

        /** 清空调试日志 */
        fun clearDebugLog() {
            debugFile?.writeText("")
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Install Conscrypt for modern TLS on all Android versions
        try {
            java.security.Security.insertProviderAt(
                org.conscrypt.Conscrypt.newProvider(), 1
            )
        } catch (_: Exception) {}

        crashFile = File(filesDir, "crash_log.txt")
        debugFile = File(filesDir, "debug_log.txt")
        debugFile?.writeText("") // 每次启动清空

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                sw.write(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date()))
                sw.write("\n")
                throwable.printStackTrace(PrintWriter(sw))
                crashFile?.writeText(sw.toString())
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
