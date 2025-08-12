package ru.queuejw.lumetro.settings.fragments

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.components.core.db.error.ErrorDatabase
import ru.queuejw.lumetro.components.ui.dialog.MetroDialog
import ru.queuejw.lumetro.databinding.SettingsFeedbackBinding
import ru.queuejw.lumetro.settings.SettingsActivity

class FeedbackSettingsFragment : BaseFragment<SettingsFeedbackBinding>() {


    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): SettingsFeedbackBinding? {
        return SettingsFeedbackBinding.inflate(inflater, container, false)
    }

    private fun eraseErrorData(context: Context) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = ErrorDatabase.getErrorData(context)
            db.getErrorDao().deleteAllErrorData()
        }
    }

    private fun showErrorDataEraseDialog(context: Context) {
        val dialog = MetroDialog.newInstance(Gravity.TOP).apply {
            setTitle(context.getString(R.string.warning_uc))
            setMessage(context.getString(R.string.erase_feedback_data))
            setPositiveDialogListener(context.getString(R.string.yes)) {
                eraseErrorData(context)
                this.dismiss()
            }
            setNegativeDialogListener(context.getString(R.string.no)) {
                this.dismiss()
            }
        }
        dialog.show(childFragmentManager, "erase_error_data_dialog")
    }

    private fun setShowDetailsSwitch() {
        binding.showErrorDetailsSwitch.apply {
            isChecked = prefs.showErrorDetailsWhenCrash
            updateText()
            setOnCheckedChangeListener { v, bool ->
                prefs.showErrorDetailsWhenCrash = bool
                updateText()
            }
        }
    }

    private fun setSaveErrorsSwitch() {
        binding.saveErrorSwitch.apply {
            isChecked = prefs.allowSaveErrorData
            updateText()
            setOnCheckedChangeListener { v, bool ->
                prefs.allowSaveErrorData = bool
                updateText()
            }
        }
    }

    private fun setAccentSwitch() {
        binding.useAccentErrorScreenSwitch.apply {
            isChecked = prefs.coloredErrorScreen
            updateText()
            setOnCheckedChangeListener { v, bool ->
                prefs.coloredErrorScreen = bool
                updateText()
            }
        }
    }

    private fun setUi() {
        setSaveErrorsSwitch()
        setShowDetailsSwitch()
        setAccentSwitch()
        binding.apply {
            showAllErrors.setOnClickListener {
                (activity as SettingsActivity?)?.changeFragment(
                    FeedbackErrorListFragment(),
                    "error_list_fragment"
                )
            }
            deleteAllErrors.setOnClickListener {
                showErrorDataEraseDialog(it.context)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as SettingsActivity?)?.setText(getString(R.string.feedback))
        setUi()
    }
}