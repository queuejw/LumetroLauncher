package ru.queuejw.lumetro.settings

import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import ru.queuejw.lumetro.components.core.base.BaseActivity
import ru.queuejw.lumetro.databinding.ActivitySettingsBinding
import ru.queuejw.lumetro.settings.fragments.AboutSettingsFragment
import ru.queuejw.lumetro.settings.fragments.EditModeSettingsFragment
import ru.queuejw.lumetro.settings.fragments.ExperimentalSettingsFragment
import ru.queuejw.lumetro.settings.fragments.FeedbackErrorListFragment
import ru.queuejw.lumetro.settings.fragments.FeedbackSettingsFragment
import ru.queuejw.lumetro.settings.fragments.IconSettingsFragment
import ru.queuejw.lumetro.settings.fragments.MainSettingsFragment
import ru.queuejw.lumetro.settings.fragments.ThemeSettingsFragment
import ru.queuejw.lumetro.settings.fragments.UpdatesFragment

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    private var fragmentValue = 0

    private fun getFragment(value: Int): Fragment {
        return when (value) {
            1 -> AboutSettingsFragment()
            2 -> EditModeSettingsFragment()
            3 -> ExperimentalSettingsFragment()
            4 -> FeedbackSettingsFragment()
            5 -> FeedbackErrorListFragment()
            6 -> IconSettingsFragment()
            7 -> ThemeSettingsFragment()
            8 -> UpdatesFragment()
            else -> MainSettingsFragment()
        }
    }

    private fun getFragmentValue(fragment: Fragment): Int {
        return when (fragment) {
            is AboutSettingsFragment -> 1
            is EditModeSettingsFragment -> 2
            is ExperimentalSettingsFragment -> 3
            is FeedbackSettingsFragment -> 4
            is FeedbackErrorListFragment -> 5
            is IconSettingsFragment -> 6
            is ThemeSettingsFragment -> 7
            is UpdatesFragment -> 8
            else -> 0
        }
    }

    private fun animateFragmentExit() {
        binding.root.apply {
            animate().rotationY(90f).alpha(0.5f).translationX(-250f).setDuration(125)
                .setInterpolator(
                    AccelerateInterpolator()
                ).withEndAction {
                    supportFragmentManager.popBackStack()
                    rotationY = -70f
                    alpha = 0f
                    animate().setStartDelay(25).rotationY(0f).alpha(1f).translationX(0f)
                        .setDuration(125)
                        .setInterpolator(
                            AccelerateInterpolator()
                        ).start()
                }.start()
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

    override fun getActivityViewBinding(): ActivitySettingsBinding {
        return ActivitySettingsBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBackPressedDispatcher()
        val bool = savedInstanceState != null
        if (bool) {
            fragmentValue = savedInstanceState.getInt("fragment")
        }
        binding.apply {
            setWindowInsets(root, true)
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.commit {
                replace(
                    fragmentContainerView.id,
                    if (!bool) MainSettingsFragment() else getFragment(fragmentValue)
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.root.apply {
            pivotX = 0f
            pivotY = height / 4f
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("fragment", fragmentValue)
    }

    fun setText(newText: String) {
        binding.settingsLabel.text = newText
    }

    private fun changeFragmentFunction(fragment: Fragment, name: String) {
        binding.fragmentContainerView.let {
            fragmentValue = getFragmentValue(fragment)
            supportFragmentManager.commit {
                replace(it.id, fragment)
                addToBackStack(name)
            }
        }
    }

    private fun animateFragmentEnter(fragment: Fragment, name: String) {
        binding.root.apply {
            animate().rotationY(-30f).alpha(0.5f).translationX(-100f).setDuration(125)
                .setInterpolator(
                    AccelerateInterpolator()
                ).withEndAction {
                    alpha = 0f
                    rotationY = 90f
                    changeFragmentFunction(fragment, name)
                    alpha = 0.5f
                    animate().setStartDelay(25).rotationY(0f).alpha(1f).translationX(0f)
                        .setDuration(150)
                        .setInterpolator(
                            AccelerateInterpolator()
                        ).start()
                }.start()
        }
    }

    fun changeFragment(fragment: Fragment, name: String) {
        animateFragmentEnter(fragment, name)
    }
}