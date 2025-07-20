package ru.queuejw.lumetro.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.databinding.SettingsEditModeBinding
import ru.queuejw.lumetro.settings.SettingsActivity

class EditModeSettingsFragment : BaseFragment<SettingsEditModeBinding>() {

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): SettingsEditModeBinding? {
        return SettingsEditModeBinding.inflate(inflater, container, false)
    }

    private fun setAnimSwitch(binding: SettingsEditModeBinding) {
        binding.editmodeAnimSwitch.apply {
            isChecked = prefs.allowEditModeAnimation
            updateText()
            setOnCheckedChangeListener { _, isChecked ->
                prefs.apply {
                    isRestartRequired = true
                    allowEditModeAnimation = isChecked
                }
                updateText()
            }
        }
    }

    private fun setUi() {
        binding.apply {
            setAnimSwitch(this)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as SettingsActivity?)?.setText(getString(R.string.editmode))
        setUi()
    }
}