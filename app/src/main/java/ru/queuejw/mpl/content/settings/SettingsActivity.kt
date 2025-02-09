package ru.queuejw.mpl.content.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.settings.fragments.MainSettingsFragment
import ru.queuejw.mpl.content.settings.fragments.ThemeSettingsFragment
import ru.queuejw.mpl.databinding.LauncherSettingsMainBinding
import ru.queuejw.mpl.helpers.ui.WPDialog
import ru.queuejw.mpl.helpers.utils.Utils
import kotlin.random.Random

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: LauncherSettingsMainBinding
    private var isTipActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        when (PREFS.appTheme) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        super.onCreate(savedInstanceState)
        binding = LauncherSettingsMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        Utils.applyWindowInsets(binding.root)
        setupBackPressedDispatcher()
        supportFragmentManager.commit {
            replace(binding.fragmentContainerView.id, MainSettingsFragment())
        }
        if (PREFS.prefs.getBoolean("themeChanged", false)) {
            PREFS.prefs.edit { putBoolean("themeChanged", false) }
            changeFragment(ThemeSettingsFragment(), "theme")
        }
    }

    override fun onStart() {
        super.onStart()
        prepareTip()
        checkHome()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun checkHome() {
        if (!isTipActive && !isHomeApp() && Random.nextFloat() < 0.25) {
            WPDialog(this).setTopDialog(false)
                .setTitle(getString(R.string.tip))
                .setMessage(getString(R.string.setAsDefaultLauncher))
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes)) {
                    startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                }.show()
        }
    }

    private fun prepareTip() {
        if (PREFS.prefs.getBoolean("tipSettingsEnabled", true)) {
            isTipActive = true
            WPDialog(this).setTopDialog(true)
                .setTitle(getString(R.string.tip))
                .setMessage(getString(R.string.tipSettings))
                .setPositiveButton(getString(android.R.string.ok), null)
                .setDismissListener {
                    isTipActive = false
                }
                .show()
            PREFS.prefs.edit { putBoolean("tipSettingsEnabled", false) }
        }
    }

    private fun isHomeApp(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val res = packageManager.resolveActivity(intent, 0)
        return res!!.activityInfo != null && (packageName
                == res.activityInfo.packageName)
    }

    private fun setupBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStackImmediate()
                } else {
                    finish()
                }
            }
        })
    }

    fun setText(newText: String) {
        binding.settingsLabel.text = newText
    }

    fun recreateFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().detach(fragment).commitNow()
        supportFragmentManager.beginTransaction().attach(fragment).commitNow()
    }

    fun changeFragment(fragment: Fragment, name: String) {
        supportFragmentManager.commit {
            replace(binding.fragmentContainerView.id, fragment)
            addToBackStack(name)
        }
    }
}