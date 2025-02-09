package ru.queuejw.mpl.content.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customFont
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.SettingsAllappsBinding

class AllAppsSettingsFragment : Fragment() {

    private var _binding: SettingsAllappsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsAllappsBinding.inflate(inflater, container, false)
        (requireActivity() as SettingsActivity).setText(getString(R.string.all_apps_list))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFont()
        setUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUI() {
        binding.settingsBtnSwitch.apply {
            isChecked = PREFS.isSettingsBtnEnabled
            text =
                if (PREFS.isSettingsBtnEnabled) getString(R.string.on) else getString(R.string.off)
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.isSettingsBtnEnabled = isChecked
                PREFS.isPrefsChanged = true
                text =
                    if (PREFS.isSettingsBtnEnabled) getString(R.string.on) else getString(R.string.off)
            }
        }
        binding.disableAllAppsSwitch.apply {
            isChecked = PREFS.isAllAppsEnabled
            text = if (PREFS.isAllAppsEnabled) getString(R.string.on) else getString(R.string.off)
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.isAllAppsEnabled = isChecked
                PREFS.isPrefsChanged = true
                text =
                    if (PREFS.isAllAppsEnabled) getString(R.string.on) else getString(R.string.off)
            }
        }
        binding.keyboardWhenSearchingSwitch.apply {
            isChecked = PREFS.showKeyboardWhenSearching
            text =
                if (PREFS.showKeyboardWhenSearching) getString(R.string.on) else getString(R.string.off)
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.showKeyboardWhenSearching = isChecked
                text =
                    if (PREFS.showKeyboardWhenSearching) getString(R.string.on) else getString(R.string.off)
            }
        }
        binding.keyboardWhenAllAppsOpened.apply {
            isChecked = PREFS.showKeyboardWhenOpeningAllApps
            text =
                if (PREFS.showKeyboardWhenOpeningAllApps) getString(R.string.on) else getString(R.string.off)
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.showKeyboardWhenOpeningAllApps = isChecked
                text =
                    if (PREFS.showKeyboardWhenOpeningAllApps) getString(R.string.on) else getString(
                        R.string.off
                    )
            }
        }
        binding.allAppsKeyboardActionSwitch.apply {
            isChecked = PREFS.allAppsKeyboardActionEnabled
            text =
                if (PREFS.allAppsKeyboardActionEnabled) getString(R.string.on) else getString(R.string.off)
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.allAppsKeyboardActionEnabled = isChecked
                text =
                    if (PREFS.allAppsKeyboardActionEnabled) getString(R.string.on) else getString(R.string.off)
            }
        }
    }

    private fun setupFont() {
        customFont?.let {
            binding.settingsBtnSwitch.typeface = it
            binding.disableAllAppsSwitch.typeface = it
            binding.keyboardWhenSearchingSwitch.typeface = it
            binding.keyboardWhenAllAppsOpened.typeface = it
            binding.settingsButtonLabel.typeface = it
            binding.settingsButtonLabelSwitch.typeface = it
            binding.alphabetSettingKeyboardLabel.typeface = it
            binding.additionalOptions.typeface = it
            binding.showScreenAllAppsLabel.typeface = it
            binding.autoSearchLabel.typeface = it
            binding.allAppsKeyboardActionLabel.typeface = it
            binding.allAppsKeyboardActionSwitch.typeface = it
        }
    }
}