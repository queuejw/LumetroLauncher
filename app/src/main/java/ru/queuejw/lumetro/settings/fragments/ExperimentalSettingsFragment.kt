package ru.queuejw.lumetro.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.base.BaseMainFragment
import ru.queuejw.lumetro.databinding.SettingsExperimentalBinding
import ru.queuejw.lumetro.settings.SettingsActivity

class ExperimentalSettingsFragment : BaseMainFragment<SettingsExperimentalBinding>() {

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): SettingsExperimentalBinding {
        return SettingsExperimentalBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as SettingsActivity?)?.setText(getString(R.string.experiments))
    }
}