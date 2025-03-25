package ru.queuejw.mpl.content.bsod

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.bsod.fragments.BlueScreenFragment
import ru.queuejw.mpl.content.bsod.fragments.RecoveryFragment
import ru.queuejw.mpl.content.bsod.fragments.RecoveryOptionsFragment
import ru.queuejw.mpl.content.data.bsod.BSOD
import ru.queuejw.mpl.databinding.CriticalErrorActivityBinding
import ru.queuejw.mpl.helpers.utils.Utils

class CriticalErrorActivity : AppCompatActivity() {

    private lateinit var binding: CriticalErrorActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.bsod)
        super.onCreate(savedInstanceState)
        binding = CriticalErrorActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val stacktrace = intent.extras?.getString("stacktrace")!!
        saveError(stacktrace, this)
        val counter = PREFS.prefs.getInt("crash_count", 0)
        if (counter >= 3) {
            startRecovery()
        } else {
            startBlueScreen(stacktrace)
        }
    }

    private fun saveError(stacktrace: String, context: Context) {
        val model = "\nModel: ${Build.MODEL}\n"
        val brand = "Brand: ${Build.BRAND}\n"
        val mplVerCode = "MPL Ver Code: ${Utils.VERSION_CODE}\n"
        val android = "Android Version: ${Build.VERSION.SDK_INT}\n\n"
        val errCode = "\nIf vou call a support person. aive them this info:\n" +
                "Stop code: $stacktrace"
        val error =
            "Your launcher ran into a problem and needs to restart. We're just collecting some error info, and then we'll restart for you.\n$model$brand$android$mplVerCode\n$stacktrace$errCode"

        lifecycleScope.launch(Dispatchers.IO) {
            val db = BSOD.getData(context)
            Utils.saveError(error, db)
            db.close()
        }
    }

    private fun startRecovery() {
        supportFragmentManager.commit {
            replace(binding.fragmentContainerView.id, RecoveryFragment(), "bsod")
        }
    }

    fun startRecoveryOptions() {
        supportFragmentManager.commit {
            replace(binding.fragmentContainerView.id, RecoveryOptionsFragment(), "bsod")
        }
    }

    private fun startBlueScreen(stacktrace: String) {
        val fragment = BlueScreenFragment()
        val bundle = Bundle()
        bundle.putString("stacktrace", stacktrace)
        fragment.arguments = bundle
        supportFragmentManager.commit {
            replace(binding.fragmentContainerView.id, fragment, "bsod")
        }
    }
}

