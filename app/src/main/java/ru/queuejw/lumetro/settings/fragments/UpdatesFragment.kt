package ru.queuejw.lumetro.settings.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.databinding.SettingsUpdatesBinding
import ru.queuejw.lumetro.settings.SettingsActivity

class UpdatesFragment : BaseFragment<SettingsUpdatesBinding>() {

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): SettingsUpdatesBinding = SettingsUpdatesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as SettingsActivity?)?.setText(getString(R.string.launcher_update))
        binding.checkForUpdatesBtn.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://github.com/queuejw/LumetroLauncher/releases/latest".toUri()
                )
            )
        }
    }
}