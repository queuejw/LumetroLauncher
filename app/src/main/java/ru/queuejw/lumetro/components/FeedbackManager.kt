package ru.queuejw.lumetro.components

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.db.error.ErrorDatabase
import ru.queuejw.lumetro.model.ErrorEntity

class FeedbackManager {

    private var db: ErrorDatabase? = null
    private val tag = "FeedbackManager"

    private fun createDatabase(context: Context) {
        db = ErrorDatabase.getErrorData(context)
    }

    fun sendEmail(string: String, context: Context) {
        val text = "${context.getString(R.string.feedback_email_placeholder)}\n\n$string"
        val selectorIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
        }
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf("dimon6018t@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Lumetro Feedback")
            putExtra(Intent.EXTRA_TEXT, text)
            selector = selectorIntent
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(
                    emailIntent,
                    context.getString(R.string.feedback_email_tip)
                )
            )
        }.onFailure {
            Log.w("Feedback", it)
        }
    }

    suspend fun saveErrorDetails(string: String, context: Context) {
        Log.d(tag, "Saving error details...")
        db ?: {
            Log.d(tag, "Database is null, creating a new object")
            createDatabase(context)
        }
        db?.apply {
            Log.d(tag, "Inserting item")
            val item = ErrorEntity(details = string)
            getErrorDao().insertItem(item)
        }
        Log.d(tag, "Done")
    }

    fun closeFeedbackManager() {
        db?.close()
        db = null
    }
}