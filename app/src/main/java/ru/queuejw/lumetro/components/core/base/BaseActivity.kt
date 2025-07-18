package ru.queuejw.lumetro.components.core.base

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.prefs.Prefs

abstract class BaseActivity<viewBinding : ViewBinding?>() : AppCompatActivity() {

    private var _prefs: Prefs? = null
    val prefs get() = _prefs!!

    // in future i want to add a font manager

    private var _binding: viewBinding? = null
    val binding get() = _binding!!

    protected abstract fun getActivityViewBinding(): viewBinding?

    fun setWindowInsets(view: View, includeTopInsets: Boolean) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                if (!includeTopInsets) 0 else systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }

    private fun setAppTheme() {
        if (prefs.dynamicColorEnabled) {
            setTheme(R.style.MPL_Dynamic)
        }
    }

    private fun setDarkMode() {
        when (prefs.appTheme) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun initComponents() {
        _prefs = Prefs(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initComponents()
        setAppTheme()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setDarkMode()
        _binding = getActivityViewBinding()
        setContentView(binding.root)
    }

    private fun destroyComponents() {
        _prefs = null
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyComponents()
    }
}