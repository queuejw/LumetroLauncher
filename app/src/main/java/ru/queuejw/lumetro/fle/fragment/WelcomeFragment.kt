package ru.queuejw.lumetro.fle.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.AppManager
import ru.queuejw.lumetro.components.core.DefaultAccentColors
import ru.queuejw.lumetro.components.core.TileManager
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.components.ui.dialog.MetroDialog
import ru.queuejw.lumetro.databinding.FleWelcomeBinding
import ru.queuejw.lumetro.fle.FirstLaunchExperienceActivity
import ru.queuejw.lumetro.main.MainActivity

class WelcomeFragment : BaseFragment<FleWelcomeBinding>() {

    private val colors = DefaultAccentColors.entries

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FleWelcomeBinding? {
        return FleWelcomeBinding.inflate(inflater, container, false)
    }

    private var fragmentClosed = false
    private var prevColor = 0

    private fun prepareScreen() {
        (activity as FirstLaunchExperienceActivity?)?.apply {
            nextFragment = 1
            previousFragment = 0
            setAppBarText(null)
            updateNextButtonText(this.getString(R.string.next))
            updatePreviousButtonText(this.getString(R.string.back))
            enableAllButtons()
            setButtonState(0, true)
        }
        binding.imageView.setOnClickListener {
            changeIconColor()
        }
        binding.skipFleTextview.setOnClickListener {
            showSkipFleDialog(it.context)
        }
    }

    private fun changeIconColor() {
        context?.let {
            val randomColor = ContextCompat.getColor(it, colors.random().colorResId)
            if (prevColor == randomColor) {
                changeIconColor()
                return
            }
            binding.imageView.setColorFilter(randomColor)
            prevColor = randomColor
        }
    }

    private fun animateColorChange() {
        if (fragmentClosed) return
        Handler(Looper.getMainLooper()).postDelayed({
            changeIconColor()
            animateColorChange()
        }, 1500)
    }

    private fun animateScreen() {
        binding.let {
            it.linearLayout.animate()?.setDuration(700)?.alpha(1f)?.translationY(0f)?.scaleY(1f)
                ?.scaleX(1f)?.start()
            it.imageView.animate()?.setStartDelay(400)?.alpha(1f)?.setDuration(1000)
                ?.withEndAction {
                    (activity as FirstLaunchExperienceActivity?)?.animateBottomBar(true)
                    animateColorChange()
                    it.skipFleTextview.animate()?.alpha(0.5f)?.setDuration(300)?.start()
                }?.start()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareScreen()
        changeIconColor()
        animateScreen()
    }

    private fun skipFle() {
        (activity as FirstLaunchExperienceActivity?)?.animateBottomBar(false)
        binding.root.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            binding.root.visibility = View.GONE
        }?.start()

        lifecycleScope.launch(Dispatchers.IO) {
            prefs.isFirstLaunch = false
            val tileManager = TileManager()
            val appManager = AppManager()
            context?.let {
                val size = appManager.getInstalledApps(it, true).size * 2
                tileManager.generatePlaceholders(size, it, true)
            }
            withContext(Dispatchers.Main) {
                activity?.apply {
                    finishAndRemoveTask()
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
        }
    }

    private fun showSkipFleDialog(context: Context) {
        val dialog = MetroDialog.newInstance(Gravity.TOP).apply {
            setTitle(context.getString(R.string.fle_skip_setup))
            setMessage(context.getString(R.string.skip_fle_dialog_message))
            setPositiveDialogListener(context.getString(R.string.yes)) {
                skipFle()
                this.dismiss()
            }
            setNegativeDialogListener(context.getString(R.string.no)) {
                this.dismiss()
            }
        }
        dialog.show(childFragmentManager, "skip_fle")
    }

    override fun onDestroy() {
        fragmentClosed = true
        super.onDestroy()
    }
}