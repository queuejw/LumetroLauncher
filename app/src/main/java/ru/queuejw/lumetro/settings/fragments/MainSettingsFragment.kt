package ru.queuejw.lumetro.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import leakcanary.LeakCanary
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.databinding.SettingsMainBinding
import ru.queuejw.lumetro.settings.SettingsActivity

class MainSettingsFragment : BaseFragment<SettingsMainBinding>() {

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): SettingsMainBinding? {
        return SettingsMainBinding.inflate(inflater, container, false)
    }

    private fun setClickListener(view: View, fragment: Fragment, name: String) {
        view.setOnClickListener {
            (activity as SettingsActivity?)?.changeFragment(fragment, name)
        }
    }

    private fun setOnClickers() {
        binding.settingsList.apply {
            setClickListener(themeSetting, ThemeSettingsFragment(), "theme")
            setClickListener(iconsSetting, IconSettingsFragment(), "icons")
            setClickListener(editModeSetting, EditModeSettingsFragment(), "edit_mode")
            setClickListener(feedbackSetting, FeedbackSettingsFragment(), "feedback")
            setClickListener(updatesSetting, UpdatesFragment(), "updates")
            setClickListener(aboutSetting, AboutSettingsFragment(), "about")
            setClickListener(tileSetting, TileSettingsFragment(), "tiles")
            if (prefs.experimentsEnabled) {
                experimentSetting.visibility = View.VISIBLE
                setClickListener(experimentSetting, ExperimentalSettingsFragment(), "experiments")
            }
            leaks.setOnClickListener { startActivity(LeakCanary.newLeakDisplayActivityIntent()) } //LeakCanary
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as SettingsActivity?)?.setText(getString(R.string.launcher))
        setOnClickers()
    }
}