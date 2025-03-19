package ru.queuejw.mpl.content.settings.fragments

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customFont
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.settings.Reset
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.SettingsAboutBinding
import ru.queuejw.mpl.helpers.ui.WPDialog
import ru.queuejw.mpl.helpers.utils.Utils
import kotlin.system.exitProcess

class AboutSettingsFragment : Fragment() {

    private var _binding: SettingsAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsAboutBinding.inflate(inflater, container, false)
        (requireActivity() as SettingsActivity).setText(getString(R.string.about))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFont()
        setupLayout()
        context?.let { checkHome(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setFont() {
        if (PREFS.customFontInstalled) {
            customFont?.let {
                binding.phoneinfoLabel.typeface = it
                binding.queuejw.typeface = it
                binding.phoneinfo.typeface = it
                binding.phoneinfoMore.typeface = it
                binding.moreInfobtn.typeface = it
                binding.resetLauncher.typeface = it
                binding.restartLauncher.typeface = it
                binding.helpContactsLabel.typeface = it
                binding.getHelpText.typeface = it
                binding.emailText.typeface = it
                binding.onlineContactsText.typeface = it
                binding.getHelpText.typeface = it
                binding.githubText.typeface = it
                binding.telegramText.typeface = it
                binding.supportText.typeface = it
            }
        }
    }

    private fun setupLayout() {
        binding.phoneinfo.text =
            getString(
                R.string.phone_info,
                "${Build.MANUFACTURER} ${Build.PRODUCT}",
                Build.MODEL,
                Utils.VERSION_NAME
            )
        binding.moreInfobtn.setOnClickListener {
            showMoreInfo()
        }
        binding.resetLauncher.setOnClickListener {
            resetDialog()
        }
        binding.restartLauncher.setOnClickListener {
            exitProcess(0)
        }
        binding.crashBtn.setOnClickListener {
            requireActivity().findViewById<View>(R.id.test_font_text).background = null
        }
    }

    private fun checkHome(context: Context) {
        if (!isHomeApp(context)) {
            WPDialog(context).setTopDialog(false)
                .setTitle(getString(R.string.tip))
                .setMessage(getString(R.string.setAsDefaultLauncher))
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes)) {
                    startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                }.show()
        }
    }

    private fun isHomeApp(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val res = context.packageManager.resolveActivity(intent, 0)
        return res!!.activityInfo != null && (context.packageName
                == res.activityInfo.packageName)
    }

    private fun resetPart1() {
        val intent = (Intent(requireActivity(), Reset::class.java))
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        requireActivity().finishAffinity()
        startActivity(intent)
    }

    private fun resetDialog() {
        WPDialog(requireActivity()).setTopDialog(true)
            .setTitle(getString(R.string.reset_warning_title))
            .setMessage(getString(R.string.reset_warning))
            .setNegativeButton(getString(R.string.yes)) {
                resetPart1()
                WPDialog(requireActivity()).dismiss()
            }
            .setPositiveButton(getString(R.string.no), null).show()
    }

    private fun showMoreInfo() {
        binding.phoneinfoMore.text = getString(
            R.string.phone_moreinfo,
            Utils.VERSION_NAME,
            Utils.VERSION_CODE,
            Build.DEVICE,
            Build.BRAND,
            Build.MODEL,
            Build.PRODUCT,
            Build.HARDWARE,
            Build.DISPLAY,
            Build.TIME
        )
        binding.moreInfobtn.visibility = View.GONE
        binding.moreinfoLayout.visibility = View.VISIBLE
    }
}
