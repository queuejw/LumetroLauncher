package ru.queuejw.mpl.content.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import leakcanary.LeakCanary
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.SettingsListBinding
import ru.queuejw.mpl.helpers.utils.Utils

class MainSettingsFragment : Fragment() {

    private var _binding: SettingsListBinding? = null
    private val binding get() = _binding!!

    private var fragmentActive = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsListBinding.inflate(inflater, container, false)
        (requireActivity() as SettingsActivity).setText(getString(R.string.launcher))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUi()
        setOnClickers()
    }

    private fun setUi() {
        binding.themeSub.text = Utils.accentName(requireContext())
        binding.navbarSub.text = when (PREFS.navBarColor) {
            0 -> getString(R.string.always_dark)
            1 -> getString(R.string.always_light)
            2 -> getString(R.string.matches_accent_color)
            3 -> getString(R.string.hide_navbar)
            4 -> getString(R.string.auto)
            else -> getString(R.string.navigation_bar_2)
        }
        binding.fontsSub.text = runCatching {
            if (!PREFS.customFontInstalled) getString(R.string.fonts_tip) else PREFS.customFontName
        }.getOrElse {
            getString(R.string.fonts_tip)
        }
        binding.iconsSub.text = runCatching {
            if (PREFS.iconPackPackage == "null") getString(R.string.iconPackNotSelectedSub)
            else requireActivity().packageManager.getApplicationLabel(
                requireActivity().packageManager.getApplicationInfo(
                    PREFS.iconPackPackage!!,
                    0
                )
            )
        }.getOrElse { getString(R.string.iconPackNotSelectedSub) }
    }

    private fun setOnClickers() {
        setClickListener(binding.themeSetting, ThemeSettingsFragment(), "theme")
        setClickListener(binding.allAppsSetting, AllAppsSettingsFragment(), "allapps")
        setClickListener(binding.tilesSetting, TileSettingsFragment(), "tiles")
        setClickListener(binding.aboutSetting, AboutSettingsFragment(), "about")
        setClickListener(binding.animSetting, AnimationSettingsFragment(), "animations")
        setClickListener(binding.updatesSetting, UpdateSettingsFragment(), "updates")
        setClickListener(binding.feedbackSetting, FeedbackSettingsFragment(), "feedback")
        setClickListener(binding.navbarSetting, NavBarSettingsFragment(), "navbar")
        setClickListener(binding.iconsSetting, IconSettingsFragment(), "icons")
        setClickListener(binding.fontSetting, FontSettingsFragment(), "fonts")
        binding.leaks.setOnClickListener { startActivity(LeakCanary.newLeakDisplayActivityIntent()) } //LeakCanary
    }

    private fun setClickListener(view: View, fragment: Fragment, name: String) {
        view.setOnClickListener {
            if(PREFS.isTransitionAnimEnabled) {
                lifecycleScope.launch {
                    animateSettings(false)
                    delay(100)
                    fragmentActive = false
                    (requireActivity() as SettingsActivity).changeFragment(fragment, name)
                }
            } else {
                (requireActivity() as SettingsActivity).changeFragment(fragment, name)
            }
        }
    }

    override fun onResume() {
        if(fragmentActive) {
            animateSettings(true)
        }
        fragmentActive = true
        super.onResume()

    }

    private fun animateSettings(resumeAnimation: Boolean) {
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}