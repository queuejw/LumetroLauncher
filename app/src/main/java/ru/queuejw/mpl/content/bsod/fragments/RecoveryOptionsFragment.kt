package ru.queuejw.mpl.content.bsod.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.RecoveryOptionsFragmentBinding

class RecoveryOptionsFragment : Fragment() {

    private var _binding: RecoveryOptionsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = RecoveryOptionsFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUi()
    }

    private fun setUi() {
        binding.launcherSettingsCard.setOnClickListener {
            startActivity(Intent(requireActivity(), SettingsActivity::class.java))
        }
        binding.systemSettingsCard.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
        binding.openBrowserCard.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, "https://google.com".toUri()))
        }
        binding.openGithubCard.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://github.com/queuejw/MetroPhoneLauncher".toUri()
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}