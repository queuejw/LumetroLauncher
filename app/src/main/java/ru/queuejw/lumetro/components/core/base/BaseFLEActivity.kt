package ru.queuejw.lumetro.components.core.base

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.ui.dialog.MetroDialog
import ru.queuejw.lumetro.databinding.FleActivityBinding
import ru.queuejw.lumetro.fle.fragment.EarlyAccessFragment
import ru.queuejw.lumetro.fle.fragment.FleAppsFragment
import ru.queuejw.lumetro.fle.fragment.FleCompleteSetupFragment
import ru.queuejw.lumetro.fle.fragment.FleThemeFragment
import ru.queuejw.lumetro.fle.fragment.ToUFragment
import ru.queuejw.lumetro.fle.fragment.ToURejectedFragment
import ru.queuejw.lumetro.fle.fragment.WelcomeFragment
import kotlin.system.exitProcess

open class BaseFLEActivity : BaseActivity<FleActivityBinding>() {

    private var bottomBarTranslation: Float? = null
    private val backCallback by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showCloseAppDialog(this@BaseFLEActivity)
            }
        }
    }
    private val animationDuration = 300L
    private var handler: Handler? = null
    var nextFragment = 1
    var currentFragment = 0
    var previousFragment = 0

    override fun getActivityViewBinding(): FleActivityBinding? {
        return FleActivityBinding.inflate(layoutInflater)
    }

    private fun showCloseAppDialog(context: Context) {
        val dialog = MetroDialog.newInstance(Gravity.TOP).apply {
            setTitle(context.getString(R.string.warning_uc))
            setMessage(context.getString(R.string.fle_on_back_pressed_message))
            setPositiveDialogListener(context.getString(R.string.yes)) {
                this.dismiss()
                exitProcess(0)
            }
            setNegativeDialogListener(context.getString(R.string.no)) {
                this.dismiss()
            }
        }
        dialog.show(supportFragmentManager, "exit")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bottomBarTranslation =
            resources.getDimensionPixelSize(R.dimen.fle_bottom_bar_translation_y).toFloat()
        handler = Handler(Looper.getMainLooper())
        onBackPressedDispatcher.addCallback(this, backCallback)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current_fle_fragment", currentFragment)
    }

    fun updateColor(color: Int) {
        binding.apply {
            next.updateColor(color)
            previous.updateColor(color)
        }
    }

    fun setAppBarText(newText: String?) {
        binding.appbarTextView.text = newText
    }

    fun animateBottomBar(showBottomBar: Boolean) {
        binding.bottomBar.animate()
            ?.translationY(if (!showBottomBar) bottomBarTranslation!! else 0f)
            ?.alpha(if (!showBottomBar) 0f else 1f)?.setDuration(125)?.start()
    }

    fun updateNextButtonText(text: String) {
        binding.next.text = text
    }

    fun updatePreviousButtonText(text: String) {
        binding.previous.text = text
    }

    fun enableAllButtons() {
        binding.next.apply {
            visibility = View.VISIBLE
            isEnabled = true
        }
        binding.previous.apply {
            visibility = View.VISIBLE
            isEnabled = true
        }
    }

    // 0 - back, 1 - next
    fun setButtonState(buttonCode: Int, makeInvisible: Boolean = false) {
        if (buttonCode == 1) binding.next.apply {
            visibility = if (makeInvisible) View.INVISIBLE else View.VISIBLE
            isEnabled = !makeInvisible
        } else {
            binding.previous.apply {
                visibility = if (makeInvisible) View.INVISIBLE else View.VISIBLE
                isEnabled = !makeInvisible
            }
        }
    }

    private fun getFragment(value: Int): Fragment {
        return when (value) {
            -1 -> ToURejectedFragment()
            1 -> EarlyAccessFragment()
            2 -> ToUFragment()
            4 -> FleThemeFragment()
            5 -> FleAppsFragment()
            6 -> FleCompleteSetupFragment()
            else -> WelcomeFragment()
        }
    }

    fun setFragment(value: Int, animEnabled: Boolean = true) {
        currentFragment = value
        if (!animEnabled) {
            binding.apply {
                lifecycleScope.launch {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        supportFragmentManager.commit {
                            replace(
                                fragmentContainerView.id,
                                getFragment(value),
                                "fle_$currentFragment"
                            )
                        }
                    }
                }
            }
        } else {
            val returnAnim = value == previousFragment
            binding.apply {
                fragmentContainerView.animate().translationX(if (returnAnim) 1000f else -1000f)
                    .alpha(0f).setDuration(animationDuration)
                    .start()
                oobeView.animate().translationX(if (returnAnim) 500f else -500f).alpha(0f)
                    .setDuration(animationDuration + 50)
                    .start()
            }
            handler?.postDelayed({
                lifecycleScope.launch {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        binding.apply {
                            supportFragmentManager.commit {
                                replace(
                                    fragmentContainerView.id,
                                    getFragment(value),
                                    "fle_$currentFragment"
                                )
                            }
                            fragmentContainerView.translationX = if (returnAnim) -500f else 500f
                            oobeView.translationX = if (returnAnim) -500f else 500f
                            fragmentContainerView.animate().translationX(0f).alpha(1f)
                                .setDuration(animationDuration)
                                .start()
                            oobeView.animate().translationX(0f).alpha(1f)
                                .setDuration(animationDuration + 50)
                                .start()
                        }
                    }
                }

            }, 300)
        }
    }

    override fun onDestroy() {
        handler = null
        backCallback.remove()
        super.onDestroy()
    }
}