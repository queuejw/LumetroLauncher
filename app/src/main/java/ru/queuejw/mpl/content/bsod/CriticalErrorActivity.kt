package ru.queuejw.mpl.content.bsod

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.bsod.fragments.BlueScreenFragment
import ru.queuejw.mpl.content.bsod.fragments.RecoveryFragment
import ru.queuejw.mpl.content.bsod.fragments.RecoveryOptionsFragment
import ru.queuejw.mpl.databinding.CriticalErrorActivityBinding

class CriticalErrorActivity : AppCompatActivity() {

    private lateinit var binding: CriticalErrorActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.bsod)
        super.onCreate(savedInstanceState)
        binding = CriticalErrorActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val counter = PREFS.prefs.getInt("crash_count", 0)
        if (counter >= 3) {
            startRecovery()
        } else {
            val stacktrace = intent.extras?.getString("stacktrace")!!
            startBlueScreen(stacktrace)
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

