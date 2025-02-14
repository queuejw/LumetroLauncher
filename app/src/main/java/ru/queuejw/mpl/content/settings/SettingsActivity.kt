package ru.queuejw.mpl.content.settings

import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customBoldFont
import ru.queuejw.mpl.Application.Companion.customFont
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
        binding.root.apply {
            pivotX = 24f
            pivotY = height.toFloat()
        }
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        Utils.applyWindowInsets(binding.root)
        setupBackPressedDispatcher()
        setupFont()
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        if (PREFS.prefs.getBoolean("themeChanged", false)) {
            changeFragmentFunction(ThemeSettingsFragment(), "theme")
            PREFS.prefs.edit { putBoolean("themeChanged", false) }
        } else {
            supportFragmentManager.commit {
                replace(binding.fragmentContainerView.id, MainSettingsFragment())
            }
        }
    }

    private fun setupFont() {
        customFont?.let {
            binding.settings.typeface = it
        }
        customBoldFont?.let {
            binding.settings.typeface = it
        }
    }

    override fun onStart() {
        super.onStart()
        prepareTip()
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
            binding.root.animate().rotationY(90f).alpha(0.75f).translationX(-500f).setDuration(125).setInterpolator(
                DecelerateInterpolator()
            ).withEndAction {
                supportFragmentManager.popBackStack()
                binding.root.apply {
                    rotationY = -90f
                    alpha = 0f
                }
                lifecycleScope.launch {
                    delay(25)
                    binding.root.animate().rotationY(0f).alpha(1f).translationX(0f).setDuration(125).setInterpolator(
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
            animateFragmentEnter(fragment, name)
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
    private fun animateFragmentEnter(fragment: Fragment, name: String) {
        binding.root.animate().rotationY(-90f).alpha(0.75f).translationX(-500f).setDuration(125)
            .setInterpolator(
                DecelerateInterpolator()
            ).withEndAction {
                binding.root.apply {
                    alpha = 0f
                    rotationY = 90f
                }
                changeFragmentFunction(fragment, name)
                lifecycleScope.launch {
                    delay(25)
                    binding.root.alpha = 0.5f
                    binding.root.animate().rotationY(0f).alpha(1f).translationX(0f).setDuration(125)
                        .setInterpolator(
                            DecelerateInterpolator()
                        ).start()
                }.start()
            }
        }
    }