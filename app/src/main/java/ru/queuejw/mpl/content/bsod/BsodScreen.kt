package ru.queuejw.mpl.content.bsod

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Main
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.bsod.recovery.Recovery
import ru.queuejw.mpl.content.data.bsod.BSOD
import ru.queuejw.mpl.databinding.BsodScreenBinding
import ru.queuejw.mpl.helpers.utils.Utils

class BsodScreen : AppCompatActivity() {

    private lateinit var db: BSOD
    private lateinit var binding: BsodScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = BsodScreenBinding.inflate(layoutInflater)
        var counter = PREFS.prefs.getInt("crashCounter", 0)
        counter += 1
        PREFS.prefs.edit {
            putBoolean("app_crashed", true)
            putInt("crashCounter", counter)
        }
        setTheme(R.style.bsod)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        saveError()
        if (counter >= 3) {
            openRecovery()
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        Utils.applyWindowInsets(binding.root)

    }

    private fun openRecovery() {
        val intent = Intent(this, Recovery::class.java)
        intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("stacktrace", intent.extras?.getString("stacktrace"))
        }
        finishAffinity()
        startActivity(intent)
    }

    private fun saveError() {
        CoroutineScope(Dispatchers.IO).launch {
            db = BSOD.getData(this@BsodScreen)
            val model = "\nModel: ${Build.MODEL}\n"
            val brand = "Brand: ${Build.BRAND}\n"
            val mplVerCode = "MPL Ver Code: ${Utils.VERSION_CODE}\n"
            val android = "Android Version: ${Build.VERSION.SDK_INT}\n\n"
            val code = intent.extras?.getString("errorCode")
            val errCode = "\nIf vou call a support person. aive them this info:\n" +
                    "Stop code: $code"
            val error =
                "Your launcher ran into a problem and needs to restart. We're just collecting some error info, and then we'll restart for you.\n $model$brand$android$mplVerCode" + intent.extras?.getString(
                    "stacktrace"
                ) + errCode
            Log.e("BSOD", error)
            Utils.saveError(error, db)
            if (PREFS.bsodOutputEnabled) {
                withContext(Dispatchers.Main) {
                    binding.bsodDetailsText.text = error
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Handler(Looper.getMainLooper()).postDelayed({ restartApplication() }, 3000)
    }

    private fun restartApplication() {
        val intent = Intent(this, Main::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}