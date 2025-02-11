package ru.queuejw.mpl.content.settings

import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.settings.fragments.MainSettingsFragment
import ru.queuejw.mpl.content.settings.fragments.ThemeSettingsFragment
import ru.queuejw.mpl.databinding.LauncherSettingsMainBinding
import ru.queuejw.mpl.helpers.ui.WPDialog
import ru.queuejw.mpl.helpers.utils.Utils

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
    }

    override fun onStart() {
        super.onStart()
        setupBackPressedDispatcher()
        supportFragmentManager.commit {
            replace(binding.fragmentContainerView.id, MainSettingsFragment())
        }
        if (PREFS.prefs.getBoolean("themeChanged", false)) {
            PREFS.prefs.edit { putBoolean("themeChanged", false) }
            changeFragment(ThemeSettingsFragment(), "theme")
        }
        prepareTip()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
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

    private fun setupBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    animateFragmentExit()
                } else {
                    finish()
                }
            }
        })
    }

    private fun animateFragmentExit() {
        if(!PREFS.isTransitionAnimEnabled) {
            supportFragmentManager.popBackStackImmediate()
        } else {
            binding.root.animate().rotationY(90f).alpha(0.75f).translationX(-100f).setDuration(150).setInterpolator(
                DecelerateInterpolator()
            ).withEndAction {
                supportFragmentManager.popBackStack()
                binding.root.apply {
                    rotationY = -45f
                    alpha = 0f
                }
                lifecycleScope.launch {
                    delay(25)
                    binding.root.animate().rotationY(0f).alpha(1f).translationX(0f).setDuration(150).setInterpolator(
                        DecelerateInterpolator()
                    ).start()
                }.start()
            }
        }
    }

    fun setText(newText: String) {
        binding.settingsLabel.text = newText
    }

    fun recreateFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().detach(fragment).commitNow()
        supportFragmentManager.beginTransaction().attach(fragment).commitNow()
    }

    fun changeFragment(fragment: Fragment, name: String) {
        if(PREFS.isTransitionAnimEnabled) {
            lifecycleScope.launch {
                animateFragmentEnter()
                delay(80)
                changeFragmentFunction(fragment, name)
            }
        } else {
            changeFragmentFunction(fragment, name)
        }
    }
    private fun changeFragmentFunction(fragment: Fragment, name: String) {
        supportFragmentManager.commit {
            replace(binding.fragmentContainerView.id, fragment)
            addToBackStack(name)
        }
    }
    private fun animateFragmentEnter() {
        binding.root.animate().rotationY(-45f).alpha(0.75f).translationX(-500f).setDuration(75)
            .setInterpolator(
                DecelerateInterpolator()
            ).withEndAction {
                binding.root.alpha = 0f
                binding.root.rotationY = 90f
                lifecycleScope.launch {
                    delay(50)
                    binding.root.alpha = 0.5f
                    binding.root.animate().rotationY(0f).alpha(1f).translationX(0f).setDuration(150)
                        .setInterpolator(
                            DecelerateInterpolator()
                        ).start()
                }.start()
            }
        }
    }