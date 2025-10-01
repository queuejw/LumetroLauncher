package ru.queuejw.lumetro.settings.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.lumetro.BuildConfig
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.ColorManager
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.components.core.db.error.ErrorDatabase
import ru.queuejw.lumetro.components.core.db.tile.TileDatabase
import ru.queuejw.lumetro.components.ui.dialog.MetroDialog
import ru.queuejw.lumetro.databinding.SettingsAboutBinding
import ru.queuejw.lumetro.settings.SettingsActivity
import kotlin.system.exitProcess

class AboutSettingsFragment : BaseFragment<SettingsAboutBinding>() {

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): SettingsAboutBinding? {
        return SettingsAboutBinding.inflate(inflater, container, false)
    }

    private fun showMoreInfo() {
        binding.apply {
            phoneinfoMore.text = getString(
                R.string.phone_more_info,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                Build.DEVICE,
                Build.BRAND,
                Build.MODEL,
                Build.PRODUCT,
                Build.HARDWARE,
                Build.DISPLAY,
                Build.TIME
            )
            moreInfobtn.visibility = View.GONE
            moreinfoLayout.visibility = View.VISIBLE
        }
    }

    private fun applyColors(context: Context) {
        val colorManager = ColorManager()
        colorManager.getAccentColor(context).let {
            binding.apply {
                getHelpText.setLinkTextColor(it)
                emailText.setLinkTextColor(it)
                githubText.setLinkTextColor(it)
                telegramText.setLinkTextColor(it)
                supportText.setLinkTextColor(it)
                lumetroAboutIcon.setColorFilter(it)
            }
        }
    }

    private fun enableExperiments(context: Context) {
        if (prefs.experimentsEnabled) return
        prefs.experimentsEnabled = true
        val d = MetroDialog.newInstance(Gravity.TOP).apply {
            setTitle(context.getString(android.R.string.unknownName))
            setMessage(context.getString(R.string.experiments_activated))
            setPositiveDialogListener(context.getString(android.R.string.ok)) {
                this.dismiss()
            }
        }
        d.show(childFragmentManager, "exp")
    }

    private suspend fun resetLauncher(context: Context): Boolean {
        prefs.reset()
        withContext(Dispatchers.IO) {
            val errDb = ErrorDatabase.getErrorData(context)
            val tileDb = TileDatabase.getTileData(context)
            errDb.getErrorDao().deleteAllErrorData()
            tileDb.getTilesDao().deleteAllTiles()
            errDb.close()
            tileDb.close()
        }
        return true
    }

    private fun setupLayout() {
        binding.apply {
            phoneinfo.text =
                getString(
                    R.string.phone_info,
                    "${Build.MANUFACTURER} ${Build.PRODUCT}",
                    Build.MODEL,
                    BuildConfig.VERSION_NAME
                )
            moreInfobtn.setOnClickListener {
                showMoreInfo()
            }
            resetLauncher.setOnClickListener { view ->
                MetroDialog.newInstance(Gravity.TOP).apply {
                    setTitle(view.context.getString(R.string.reset_title))
                    setMessage(view.context.getString(R.string.reset_msg))
                    setPositiveDialogListener(view.context.getString(R.string.yes)) {
                        lifecycleScope.launch {
                            if (resetLauncher(view.context)) {
                                activity?.finishAffinity()
                                exitProcess(0)
                            }
                        }
                    }
                    setNeutralDialogListener(view.context.getString(R.string.no)) {
                        dismiss()
                    }
                }.show(childFragmentManager, "reset_dialog")

            }
            restartLauncher.setOnClickListener {
                activity?.finish()
                exitProcess(0)
            }
            crashLauncher.setOnClickListener {
                val colorManager: ColorManager? = null
                colorManager!!.getAccentColor(it.context)
            }
            lumetroAboutIcon.setOnLongClickListener {
                enableExperiments(it.context)
                true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as SettingsActivity?)?.setText(getString(R.string.about))
        context?.let {
            applyColors(it)
        }
        setupLayout()
    }
}