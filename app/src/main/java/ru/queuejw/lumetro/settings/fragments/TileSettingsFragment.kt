package ru.queuejw.lumetro.settings.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.ColorManager
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.databinding.SettingsTilesBinding
import ru.queuejw.lumetro.settings.SettingsActivity

class TileSettingsFragment : BaseFragment<SettingsTilesBinding>() {

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): SettingsTilesBinding? = SettingsTilesBinding.inflate(inflater, container, false)

    private fun configureCornerSlider() {
        binding.cornerRadiusSlider.apply {
            val accentColor = ColorStateList.valueOf(ColorManager().getAccentColor(this.context))
            trackTintList = accentColor
            thumbTintList = accentColor
            value = prefs.tileCornerRadius.toFloat()
            addOnChangeListener { slider, value, bool ->
                prefs.isRestartRequired = true
                prefs.tileCornerRadius = value.toInt()
            }
        }
    }

    private fun setUi() {
        configureCornerSlider()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as SettingsActivity?)?.setText(getString(R.string.tiles))
        setUi()
    }
}