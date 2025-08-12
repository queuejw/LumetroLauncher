package ru.queuejw.lumetro.components.core.error

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.queuejw.lumetro.BuildConfig
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.FeedbackManager
import ru.queuejw.lumetro.components.core.ColorManager
import ru.queuejw.lumetro.components.prefs.Prefs
import ru.queuejw.lumetro.main.MainActivity
import java.text.SimpleDateFormat
import java.util.GregorianCalendar

class CriticalErrorActivity : AppCompatActivity() {

    private val tag = "Lumetro"
    private var prefs: Prefs? = null
    private var handler: Handler? = null
    private var onBackPressedCallback: OnBackPressedCallback? = null

    private var textView: MaterialTextView? = null
    private var rootView: FrameLayout? = null

    private var errorSavingAllowed = false
    private var showDetailsOptionEnabled = false

    private var coloredScreen = false

    private fun printErrorData(result: String) {
        Log.e(tag, "===============")
        Log.e(tag, result)
        Log.e(tag, "===============")
    }

    private fun setOnBackPressedCallback() {
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        }
        onBackPressedCallback?.let {
            onBackPressedDispatcher.addCallback(
                this@CriticalErrorActivity,
                it
            )
        }
    }

    private fun initPrefs() {
        prefs = Prefs(this)
        prefs?.let {
            showDetailsOptionEnabled = it.showErrorDetailsWhenCrash
            errorSavingAllowed = it.allowSaveErrorData
            coloredScreen = it.coloredErrorScreen
        }
    }

    private fun returnResultText(): String {
        val stacktrace = intent.extras?.getString("stacktrace")

        val date =
            "\n${SimpleDateFormat.getInstance().format(GregorianCalendar.getInstance().time)}\n"
        val device = "\nDevice: ${Build.MODEL}\n"
        val version = "\n$tag Version: ${BuildConfig.VERSION_NAME}\n"
        val versionCode = "\n$tag Version code: ${BuildConfig.VERSION_CODE}\n"
        val android = "Android Version: ${Build.VERSION.SDK_INT}\n\n"

        return date + device + version + versionCode + android + stacktrace
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setOnBackPressedCallback()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handler = Handler(Looper.getMainLooper())
        setContentView(R.layout.error_screen)
        initPrefs()

        if(coloredScreen) {
            val colorManager = ColorManager()
            rootView = findViewById(R.id.root)
            rootView?.setBackgroundColor(colorManager.getAccentColor(this))
        }

        textView = findViewById(R.id.error_details)

        val result = returnResultText()

        printErrorData(result)
        if (showDetailsOptionEnabled) {
            textView?.text = result
        }
        if (errorSavingAllowed) {
            val feedbackManager = FeedbackManager()
            lifecycleScope.launch(Dispatchers.IO) {
                feedbackManager.apply {
                    saveErrorDetails(result, this@CriticalErrorActivity)
                    closeFeedbackManager()
                }
            }
        }
        handler?.postDelayed({
            startActivity(Intent(this@CriticalErrorActivity, MainActivity::class.java).also {
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        }, 3000)
    }

    override fun onDestroy() {
        handler = null
        onBackPressedCallback?.remove()
        onBackPressedCallback = null
        textView = null
        rootView = null
        prefs = null
        super.onDestroy()
    }
}