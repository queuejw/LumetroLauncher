package ru.queuejw.lumetro.fle.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.toColorInt
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.base.BaseFLEActivity
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.components.ui.dialog.ColorDialog
import ru.queuejw.lumetro.databinding.FleThemeBinding
import ru.queuejw.lumetro.fle.FirstLaunchExperienceActivity


class FleThemeFragment : BaseFragment<FleThemeBinding>() {

    private var menuVisible = false

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FleThemeBinding? {
        return FleThemeBinding.inflate(inflater, container, false)
    }

    private fun prepareScreen() {
        (activity as FirstLaunchExperienceActivity?)?.apply {
            setAppBarText(getString(R.string.personalization))
            nextFragment = 5
            previousFragment = 2
            enableAllButtons()
            updateNextButtonText(this.getString(R.string.next))
            updatePreviousButtonText(this.getString(R.string.back))
            setDefaultButtonOnClickListeners()
            animateBottomBar(true)
        }
    }

    private fun updateThemeButtonText() {
        binding.apply {
            when (prefs.appTheme) {
                0 -> fleBackgroundButton.text = getString(R.string.auto)
                1 -> fleBackgroundButton.text = getString(R.string.dark)
                2 -> fleBackgroundButton.text = getString(R.string.light)
            }
        }
    }

    private fun updateTheme() {
        when (prefs.appTheme) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setThemeDropdown() {
        binding.apply {
            updateThemeButtonText()
            chooseAuto.setOnClickListener {
                prefs.appTheme = 0
                updateThemeButtonText()
                dismissDropdown(fleBackgroundMenu, fleBackgroundButton)
                updateTheme()
            }
            chooseDark.setOnClickListener {
                prefs.appTheme = 1
                updateThemeButtonText()
                dismissDropdown(fleBackgroundMenu, fleBackgroundButton)
                updateTheme()
            }
            chooseLight.setOnClickListener {
                prefs.appTheme = 2
                updateThemeButtonText()
                dismissDropdown(fleBackgroundMenu, fleBackgroundButton)
                updateTheme()
            }
        }
    }

    private fun dismissDropdown(dropdown: View, button: View) {
        if (!menuVisible) return
        menuVisible = false
        button.visibility = View.VISIBLE
        dropdown.visibility = View.GONE
    }

    private fun setColorDialogFragmentResultListener() {
        childFragmentManager.setFragmentResultListener("color", viewLifecycleOwner) { key, bundle ->
            bundle.getString("color_value")?.let {
                prefs.accentColorValue = it
                (activity as BaseFLEActivity?)?.updateColor(it.toColorInt())
            }
            (activity as BaseFLEActivity?)?.setFragment(4, false)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        prefs.fleProgress = 4
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareScreen()
        setThemeDropdown()
        setColorDialogFragmentResultListener()
        binding.fleBackgroundButton.setOnClickListener {
            if (menuVisible) return@setOnClickListener
            menuVisible = true
            it.visibility = View.GONE
            binding.fleBackgroundMenu.visibility = View.VISIBLE
        }
        binding.fleColorButton.setOnClickListener {
            val dialog = ColorDialog(it.context)
            dialog.show(childFragmentManager, "color")
        }
    }
}