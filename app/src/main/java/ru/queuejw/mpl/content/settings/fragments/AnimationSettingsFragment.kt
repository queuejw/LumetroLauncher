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
import ru.queuejw.mpl.databinding.SettingsAnimationsBinding

class AnimationSettingsFragment : Fragment() {

    private var _binding: SettingsAnimationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsAnimationsBinding.inflate(inflater, container, false)
        (requireActivity() as SettingsActivity).setText(getString(R.string.animations))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFont()
        setupLayout()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupFont() {
        customFont?.let {
            binding.additionalOptions.typeface = it
            binding.animations.typeface = it
            binding.tilesAnimCheckbox.typeface = it
            binding.liveTilesAnimCheckbox.typeface = it
            binding.allAppsAnimCheckbox.typeface = it
            binding.transitionAnimCheckbox.typeface = it
            binding.autoShutdownAnimsCheckbox.typeface = it
        }
    }

    private fun setupLayout() {
        binding.tilesAnimCheckbox.apply {
            isChecked = PREFS.isTilesAnimEnabled
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.isTilesAnimEnabled = isChecked
            }
        }
        binding.liveTilesAnimCheckbox.apply {
            isChecked = PREFS.isLiveTilesAnimEnabled
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.isLiveTilesAnimEnabled = isChecked
            }
        }
        binding.allAppsAnimCheckbox.apply {
            isChecked = PREFS.isAAllAppsAnimEnabled
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.isAAllAppsAnimEnabled = isChecked
            }
        }
        binding.transitionAnimCheckbox.apply {
            isChecked = PREFS.isTransitionAnimEnabled
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.isTransitionAnimEnabled = isChecked
                PREFS.isPrefsChanged = true
            }
        }
        binding.autoShutdownAnimsCheckbox.apply {
            isChecked = PREFS.isAutoShutdownAnimEnabled
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.isAutoShutdownAnimEnabled = isChecked
            }
        }
    }
}