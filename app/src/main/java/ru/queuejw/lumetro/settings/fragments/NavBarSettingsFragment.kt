package ru.queuejw.lumetro.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.databinding.SettingsNavbarBinding
import ru.queuejw.lumetro.settings.SettingsActivity

class NavBarSettingsFragment : BaseFragment<SettingsNavbarBinding>() {
    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): SettingsNavbarBinding {
        return SettingsNavbarBinding.inflate(inflater, container, false)
    }

    private fun setUi() {
        binding.navbarSwitch.apply {
            isChecked = prefs.navBarStyle == -1
            updateText()
            setOnCheckedChangeListener { _, isChecked ->
                prefs.navBarStyle = if (isChecked) -1 else 0
                updateText()
                prefs.isRestartRequired = true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as SettingsActivity?)?.setText(getString(R.string.navbar_label))
        setUi()
    }
}