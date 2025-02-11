package ru.queuejw.mpl.content.settings.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customBoldFont
import ru.queuejw.mpl.Application.Companion.customFont
import ru.queuejw.mpl.Application.Companion.customLightFont
import ru.queuejw.mpl.Application.Companion.setupFonts
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.data.bsod.BSOD
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.SettingsFontsBinding
import ru.queuejw.mpl.helpers.ui.WPDialog
import ru.queuejw.mpl.helpers.utils.Utils
import java.io.File

class FontSettingsFragment : Fragment() {

    private var _binding: SettingsFontsBinding? = null
    private val binding get() = _binding!!

    private lateinit var regularFontPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var lightFontPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var boldFontPickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsFontsBinding.inflate(inflater, container, false)
        (requireActivity() as SettingsActivity).setText(getString(R.string.fonts))
        prepareResultLaunchers()
        return binding.root
    }

    private fun prepareResultLaunchers() {
        regularFontPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    PREFS.customFontInstalled = true
                    result.data?.data?.let { uri ->
                        applyFontFromUri(uri, "regular", requireActivity())
                    }
                }
            }
        lightFontPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        applyFontFromUri(uri, "light", requireActivity())
                    }
                }
            }
        boldFontPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        applyFontFromUri(uri, "bold", requireActivity())
                    }
                }
            }
    }

    private fun applyFontFromUri(uri: Uri, fontType: String, context: Context) {
        runCatching {
            val file = File(uri.path!!)
            val fileName: String
            when (fontType) {
                "light" -> {
                    PREFS.customLightFontName = file.name
                    fileName = "custom_light." + file.extension
                }

                "bold" -> {
                    PREFS.customBoldFontName = file.name
                    fileName = "custom_bold." + file.extension
                }

                else -> {
                    PREFS.customFontName = file.name
                    fileName = "custom_regular." + file.extension
                }
            }
            val fontFile = File(ContextCompat.getDataDir(context), fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                fontFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
            }
            saveFontPath(fontFile.absolutePath, fontType)
            activateNewFont()
        }.getOrElse {
            Utils.saveError(it.toString(), BSOD.getData(context))
            WPDialog(context).apply {
                setTopDialog(true)
                setTitle(getString(R.string.error))
                setMessage(
                    getString(
                        R.string.apply_font_error
                    )
                )
                setPositiveButton(getString(android.R.string.ok)) {
                    dismiss()
                }
                show()
            }
            it.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFont()
        setOnClickers()
        setUi()
    }

    private fun setOnClickers() {
        binding.chooseFont.setOnClickListener {
            if (!PREFS.customFontInstalled) selectCustomFont("regular") else removeCustomFont("regular")
            PREFS.isPrefsChanged = true
        }
        binding.chooseLightFont.setOnClickListener {
            if (PREFS.customLightFontPath == null) selectCustomFont("light") else removeCustomFont("light")
            PREFS.isPrefsChanged = true
        }
        binding.chooseBoldFont.setOnClickListener {
            if (PREFS.customBoldFontPath == null) selectCustomFont("bold") else removeCustomFont("bold")
            PREFS.isPrefsChanged = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun activateNewFont() {
        setupFonts()
        setUi()
    }

    private fun saveFontPath(path: String, fontType: String) {
        when (fontType) {
            "light" -> PREFS.customLightFontPath = path
            "bold" -> PREFS.customBoldFontPath = path
            else -> PREFS.customFontPath = path
        }
    }

    private fun setUi() {
        binding.chooseFont.text =
            getString(if (!PREFS.customFontInstalled) R.string.choose_font else R.string.remove_font)
        binding.chooseLightFont.text =
            getString(if (PREFS.customLightFontPath == null) R.string.choose_light_font else R.string.remove_light_font)
        binding.chooseBoldFont.text =
            getString(if (PREFS.customBoldFontPath == null) R.string.choose_bold_font else R.string.remove_bold_font)
        binding.currentFont.visibility =
            if (!PREFS.customFontInstalled) View.GONE else View.VISIBLE
        binding.currentLightFont.visibility =
            if (PREFS.customLightFontPath == null) View.GONE else View.VISIBLE
        binding.currentBoldFont.visibility =
            if (PREFS.customBoldFontPath == null) View.GONE else View.VISIBLE
        if (PREFS.customFontInstalled) binding.currentFont.text =
            getString(R.string.current_font, PREFS.customFontName)
        if (PREFS.customLightFontPath != null) binding.currentLightFont.text =
            getString(R.string.current_light_font, PREFS.customLightFontName)
        if (PREFS.customBoldFontPath != null) binding.currentBoldFont.text =
            getString(R.string.current_bold_font, PREFS.customBoldFontName)
        setFont()
    }

    private fun setFont() {
        customFont?.let {
            binding.currentFont.typeface = it
            binding.chooseFont.typeface = it
            binding.testFontText.typeface = it
            binding.fontsTip.typeface = it
            binding.additionalOptions.typeface = it
            binding.additionalFontsTip.typeface = it
        }
        customLightFont?.let {
            binding.currentLightFont.typeface = it
            binding.chooseLightFont.typeface = it
            binding.testLightFontText.typeface = it
        }
        customBoldFont?.let {
            binding.chooseBoldFont.typeface = it
            binding.currentBoldFont.typeface = it
            binding.testBoldFontText.typeface = it
        }
    }

    private fun selectCustomFont(fontType: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "font/*"
        }
        when (fontType) {
            "light" -> lightFontPickerLauncher.launch(intent)
            "bold" -> boldFontPickerLauncher.launch(intent)
            else -> regularFontPickerLauncher.launch(intent)
        }
    }

    private fun removeCustomFont(fontType: String) {
        PREFS.apply {
            when (fontType) {
                "regular" -> {
                    customFontPath = null
                    customFontName = null
                    customFontInstalled = false
                    customLightFontPath = null
                    customLightFontName = null
                    customBoldFontPath = null
                    customBoldFontName = null
                }

                "light" -> {
                    customLightFontPath = null
                    customLightFontName = null
                }

                "bold" -> {
                    customBoldFontPath = null
                    customBoldFontName = null
                }
            }
        }
        activateNewFont()
        (requireActivity() as SettingsActivity).recreateFragment(this)
    }
}