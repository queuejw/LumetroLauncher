package ru.queuejw.lumetro.settings.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.ColorManager
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.components.ui.dialog.ColorDialog
import ru.queuejw.lumetro.databinding.SettingsThemeBinding
import ru.queuejw.lumetro.settings.SettingsActivity

class ThemeSettingsFragment : BaseFragment<SettingsThemeBinding>() {

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): SettingsThemeBinding? {
        return SettingsThemeBinding.inflate(inflater, container, false)
    }
    private var colorManager: ColorManager? = null
    private var menuVisible = false

    private fun setThemeText(context: Context) {
        if (colorManager == null) {
            colorManager = ColorManager()
        }
        val textFinal =
            getString(R.string.settings_theme_accent_title_part2) + getString(R.string.settings_theme_accent_title_part1) + getString(
                R.string.settings_theme_accent_title_part3
            )
        val spannable: Spannable = SpannableString(textFinal)
        spannable.setSpan(
            ForegroundColorSpan(colorManager!!.getAccentColor(context)),
            textFinal.indexOf(getString(R.string.settings_theme_accent_title_part1)),
            textFinal.indexOf(getString(R.string.settings_theme_accent_title_part1)) + getString(R.string.settings_theme_accent_title_part1).length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.apply {
            accentColorName.text =
                colorManager?.getAccentColorName(prefs.accentColorValue, context)
            colorView.setBackgroundColor(colorManager!!.getAccentColor(context))
            accentTip.setText(spannable, TextView.BufferType.SPANNABLE)
            phoneImg.imageTintList =
                ColorStateList.valueOf(colorManager!!.getAccentColor(context))
            autoPinAppsSwitch.updateDrawable()
            moreTilesSwitch.updateDrawable()
            dynamicColorSwtich.updateDrawable()
        }
    }

    private fun initComponents() {
        colorManager = ColorManager()
        context?.apply {
            setThemeText(this)
            setUi(this)
        }
    }

    private fun updateTheme() {
        when (prefs.appTheme) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setColorDialogFragmentResultListener() {
        childFragmentManager.setFragmentResultListener("color", viewLifecycleOwner) { key, bundle ->
            bundle.getString("color_value")?.let {
                prefs.accentColorValue = it
                context?.let { mContext ->
                    colorManager = null
                    setThemeText(mContext)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as SettingsActivity?)?.setText(getString(R.string.start_theme))
        initComponents()
        setColorDialogFragmentResultListener()
    }

    private fun updateThemeButtonText() {
        binding.backgroundButton.text = when (prefs.appTheme) {
            1 -> getString(R.string.dark)
            2 -> getString(R.string.light)
            else -> getString(R.string.auto)
        }
    }

    private fun setThemeDropdown(binding: SettingsThemeBinding) {
        binding.apply {
            updateThemeButtonText()
            chooseAuto.setOnClickListener {
                prefs.appTheme = 0
                updateThemeButtonText()
                dismissDropdown(backgroundMenu, backgroundButton)
                updateTheme()
            }
            chooseDark.setOnClickListener {
                prefs.appTheme = 1
                updateThemeButtonText()
                dismissDropdown(backgroundMenu, backgroundButton)
                updateTheme()
            }
            chooseLight.setOnClickListener {
                prefs.appTheme = 2
                updateThemeButtonText()
                dismissDropdown(backgroundMenu, backgroundButton)
                updateTheme()
            }
        }
    }

    private fun dismissDropdown(dropdown: View, button: View) {
        if (!menuVisible) return
        menuVisible = false
        button.visibility = View.VISIBLE
        dropdown.visibility = View.GONE
    }

    private fun updateDynamicColor(bool: Boolean) {
        if (DynamicColors.isDynamicColorAvailable()) {
            prefs.dynamicColorEnabled = bool
            activity?.let {
                DynamicColors.applyToActivityIfAvailable(it)
                it.recreate()
            }
        }
    }

    private fun setDynamicColorSwitch(binding: SettingsThemeBinding) {
        binding.dynamicColorSwtich.apply {
            if (!DynamicColors.isDynamicColorAvailable()) {
                isEnabled = false
                isChecked = false
                binding.dynamicColorSub.text = "${binding.dynamicColorSub.text}\n\n${context.getString(R.string.dynamicColor_error)}"
                binding.dynamicColorSub.alpha = 0.5f
            } else {
                isChecked = prefs.dynamicColorEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    updateText()
                    updateDynamicColor(isChecked)
                }
            }
            updateText()
        }
    }

    private fun setMoreTilesSwitch(binding: SettingsThemeBinding) {
        binding.moreTilesSwitch.apply {
            isChecked = prefs.showMoreTilesEnabled
            updateText()
            setOnCheckedChangeListener { _, isChecked ->
                updateText()
                prefs.showMoreTilesEnabled = isChecked
                binding.phoneImg.apply {
                    animate().alpha(0.5f).setDuration(50).withEndAction {
                        animate().alpha(1f).setDuration(50).start()
                        setImg(binding)
                    }.start()
                }
            }
        }
    }

    private fun setChooseAccentColorButton(context: Context, binding: SettingsThemeBinding) {
        binding.chooseAccent.apply {
            if (prefs.dynamicColorEnabled) {
                isEnabled = false
                alpha = 0.5f
            }
            setOnClickListener {
                val dialog = ColorDialog(context)
                dialog.show(childFragmentManager, "color")
            }
        }
    }

    private fun setImg(
        binding: SettingsThemeBinding
    ) {
        binding.phoneImg.setImageResource(if (!prefs.showMoreTilesEnabled) R.mipmap.tiles_default else R.mipmap.tiles_small)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setBackgroundButton(binding: SettingsThemeBinding) {
        binding.backgroundButton.setOnClickListener {
            if (menuVisible) return@setOnClickListener
            menuVisible = true
            it.visibility = View.GONE
            binding.backgroundMenu.visibility = View.VISIBLE
        }
        binding.root.setOnScrollChangeListener { view, x, y, oldX, oldY ->
            if (menuVisible) {
                menuVisible = false
                binding.backgroundButton.visibility = View.VISIBLE
                binding.backgroundMenu.visibility = View.GONE
            }
        }
    }
    private fun setAutoPinSwitch(binding: SettingsThemeBinding) {
        binding.autoPinAppsSwitch.apply {
            isChecked = prefs.autoPinEnabled
            updateText()
            setOnCheckedChangeListener { _, isChecked ->
                prefs.autoPinEnabled = isChecked
                updateText()
            }
        }
    }

    private fun setUi(context: Context) {
        binding.apply {
            setDynamicColorSwitch(this)
            setMoreTilesSwitch(this)
            setChooseAccentColorButton(context, this)
            setImg(this)
            setThemeDropdown(this)
            setBackgroundButton(this)
            setAutoPinSwitch(this)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        colorManager = null
    }
}
