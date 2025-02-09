package ru.queuejw.mpl.content.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.slider.Slider
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customFont
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.SettingsTilesBinding

class TileSettingsFragment : Fragment() {

    private var _binding: SettingsTilesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsTilesBinding.inflate(inflater, container, false)
        (requireActivity() as SettingsActivity).setText(getString(R.string.tiles))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFont()
        initView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupFont() {
        customFont?.let {
            binding.tileTransparencyText.typeface = it
        }
    }

    private fun initView() {
        binding.alphaSlider.apply {
            value = PREFS.tilesTransparency
            addOnChangeListener(Slider.OnChangeListener { _: Slider?, value: Float, _: Boolean ->
                PREFS.tilesTransparency = value
                PREFS.isPrefsChanged = true
            })
        }
    }
}