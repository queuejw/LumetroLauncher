package ru.queuejw.mpl.content.settings.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customFont
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.SettingsThemeBinding
import ru.queuejw.mpl.helpers.ui.WPDialog
import ru.queuejw.mpl.helpers.utils.Utils

class ThemeSettingsFragment : Fragment() {

    private var _binding: SettingsThemeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsThemeBinding.inflate(inflater, container, false)
        (requireActivity() as SettingsActivity).setText(getString(R.string.start_theme))
        setThemeText()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFont()
        configure()
        prepareTip()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupFont() {
        customFont?.let {
            binding.backgroundLabel.typeface = it
            binding.chooseTheme.typeface = it
            binding.chooseLight.typeface = it
            binding.chooseDark.typeface = it
            binding.chooseAuto.typeface = it
            binding.accentColorLabel.typeface = it
            binding.chosenAccentName.typeface = it
            binding.coloredStrokeLabel.typeface = it
            binding.coloredStrokeSwitch.typeface = it
            binding.dynamicColorLabel.typeface = it
            binding.dynamicColorSub.typeface = it
            binding.dynamicColorSwtich.typeface = it
            binding.autoPinLabel.typeface = it
            binding.autoPinSub.typeface = it
            binding.newAppsToStartSwitch.typeface = it
            binding.advancedOptions.typeface = it
            binding.blockStartLabel.typeface = it
            binding.blockStartSub.typeface = it
            binding.blockStartSwitch.typeface = it
            binding.screenOrientation.typeface = it
            binding.portraitOrientation.typeface = it
            binding.landscapeOrientation.typeface = it
            binding.defaultOrientation.typeface = it
            binding.accentTip.typeface = it
        }
    }

    private fun configure() {
        binding.chosenAccentName.text = Utils.accentName(requireActivity())
        binding.chooseTheme.apply {
            text = when (PREFS.appTheme) {
                1 -> getString(R.string.dark)
                2 -> getString(R.string.light)
                else -> getString(R.string.auto)
            }
            setOnClickListener {
                visibility = View.GONE
                binding.chooseThemeMenu.visibility = View.VISIBLE
            }
        }
        binding.chooseAuto.setOnClickListener {
            applyThemeClickListener(0)
        }
        binding.chooseLight.setOnClickListener {
            applyThemeClickListener(2)
        }
        binding.chooseDark.setOnClickListener {
            applyThemeClickListener(1)
        }
        binding.chooseAccent.setOnClickListener {
            AccentDialog.display(
                childFragmentManager
            )
        }
        binding.newAppsToStartSwitch.apply {
            isChecked = PREFS.pinNewApps
            text = if (PREFS.pinNewApps) getString(R.string.on) else getString(R.string.off)
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.pinNewApps = isChecked
                text = if (isChecked) getString(R.string.on) else getString(R.string.off)
            }
        }
        binding.dynamicColorSwtich.apply {
            if (!DynamicColors.isDynamicColorAvailable()) {
                isEnabled = false
            }
            isChecked = PREFS.accentColor == 20
            text = if (isChecked) getString(R.string.on) else getString(R.string.off)
            setOnCheckedChangeListener { _, isChecked ->
                if (DynamicColors.isDynamicColorAvailable()) {
                    PREFS.accentColor =
                        if (isChecked) 20 else PREFS.prefs.getInt("previous_accent_color", 5)
                    (requireActivity() as SettingsActivity).recreateFragment(this@ThemeSettingsFragment)
                } else {
                    Snackbar.make(
                        this,
                        getString(R.string.dynamicColor_error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
        binding.blockStartSwitch.apply {
            isChecked = PREFS.isStartBlocked
            text = if (isChecked) getString(R.string.on) else getString(R.string.off)
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.isStartBlocked = isChecked
                text = if (isChecked) getString(R.string.on) else getString(R.string.off)
            }
        }
        binding.coloredStrokeSwitch.apply {
            isChecked = PREFS.coloredStroke
            text = if (isChecked) getString(R.string.on) else getString(R.string.off)
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.coloredStroke = isChecked
                text = if (isChecked) getString(R.string.on) else getString(R.string.off)
                PREFS.isPrefsChanged = true
            }
        }
        setOrientationButtons()
    }

    private fun applyThemeClickListener(value: Int) {
        PREFS.apply {
            PREFS.appTheme = value
        }
        applyTheme()
    }

    private fun setThemeText() {
        val textFinal =
            getString(R.string.settings_theme_accent_title_part2) + " " + getString(R.string.settings_theme_accent_title_part1) + " " + getString(
                R.string.settings_theme_accent_title_part3
            )
        val spannable: Spannable = SpannableString(textFinal)
        val color = Utils.launcherAccentColor(requireActivity().theme)
        spannable.setSpan(
            ForegroundColorSpan(color),
            textFinal.indexOf(getString(R.string.settings_theme_accent_title_part1)),
            textFinal.indexOf(getString(R.string.settings_theme_accent_title_part1)) + getString(R.string.settings_theme_accent_title_part1).length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.colorView.setBackgroundColor(color)
        binding.accentTip.setText(spannable, TextView.BufferType.SPANNABLE)
    }

    private fun prepareTip() {
        if (PREFS.prefs.getBoolean("tipSettingsThemeEnabled", true)) {
            WPDialog(requireActivity()).setTopDialog(true)
                .setTitle(getString(R.string.tip))
                .setMessage(getString(R.string.tipSettingsTheme))
                .setPositiveButton(getString(android.R.string.ok), null)
                .show()
            PREFS.prefs.edit { putBoolean("tipSettingsThemeEnabled", false) }
        }
    }

    private fun applyTheme() {
        PREFS.prefs.edit { putBoolean("themeChanged", true) }
        requireActivity().recreate()
    }

    private fun setOrientationButtons() {
        val orientations = mapOf(
            "p" to Triple(true, false, false),
            "l" to Triple(false, true, false)
        )
        val (portrait, landscape, default) = orientations[PREFS.orientation] ?: Triple(
            false,
            false,
            true
        )
        binding.portraitOrientation.isChecked = portrait
        binding.landscapeOrientation.isChecked = landscape
        binding.defaultOrientation.isChecked = default
        binding.orientationRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.portraitOrientation.id -> {
                    PREFS.orientation = "p"
                }

                binding.landscapeOrientation.id -> {
                    PREFS.orientation = "l"
                }

                binding.defaultOrientation.id -> {
                    PREFS.orientation = "default"
                }
            }
            PREFS.isPrefsChanged = true
        }
    }

    class AccentDialog : DialogFragment() {

        private val viewIds = arrayOf(
            R.id.choose_color_lime, R.id.choose_color_green, R.id.choose_color_emerald,
            R.id.choose_color_cyan, R.id.choose_color_teal, R.id.choose_color_cobalt,
            R.id.choose_color_indigo, R.id.choose_color_violet, R.id.choose_color_pink,
            R.id.choose_color_magenta, R.id.choose_color_crimson, R.id.choose_color_red,
            R.id.choose_color_orange, R.id.choose_color_amber, R.id.choose_color_yellow,
            R.id.choose_color_brown, R.id.choose_color_olive, R.id.choose_color_steel,
            R.id.choose_color_mauve, R.id.choose_color_taupe
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        }

        override fun onDismiss(dialog: DialogInterface) {
            super.onDismiss(dialog)
            if (PREFS.prefs.getBoolean("themeChanged", false)) {
                activity?.recreate()
            }
        }

        override fun onStart() {
            super.onStart()
            dialog?.apply {
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setTitle("ACCENT")
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.accent_dialog, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val back = view.findViewById<FrameLayout>(R.id.back_accent_menu)
            back.setOnClickListener { dismiss() }
            for (i in 0..<viewIds.size) {
                setOnClick(view.findViewById<ImageView>(viewIds[i]), i)
            }
            val customColor = view.findViewById<ImageView>(R.id.choose_color_custom)
            customColor.setOnClickListener {
            }
        }

        private fun setOnClick(colorView: View, value: Int) {
            colorView.setOnClickListener {
                PREFS.apply {
                    accentColor = value
                    isPrefsChanged = true
                }
                PREFS.prefs.edit { putBoolean("themeChanged", true) }
                dismiss()
            }
        }

        companion object {
            private const val TAG = "accentDialog"
            fun display(fragmentManager: FragmentManager?): AccentDialog {
                val accentDialog = AccentDialog()
                fragmentManager?.let { accentDialog.show(it, TAG) }
                return accentDialog
            }
        }
    }
}