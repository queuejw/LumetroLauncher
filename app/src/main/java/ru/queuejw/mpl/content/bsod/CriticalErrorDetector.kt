package ru.queuejw.mpl.content.bsod

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.system.exitProcess

class CriticalErrorDetector(val context: Context) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e("BSOD", "Detected critical error. See: ${e.stackTraceToString()}")
        startCriticalErrorActivity(createCriticalErrorIntent(e), context)
    }

    private fun startCriticalErrorActivity(intent: Intent, context: Context) {
        Log.d("BSOD", "Launch critical error activity")
        context.startActivity(intent)
        exitProcess(1)
    }

    private fun createCriticalErrorIntent(e: Throwable): Intent {
        val intent = Intent(context, CriticalErrorActivity::class.java)
        intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("stacktrace", e.stackTraceToString())
        }
        return intent
    }
}