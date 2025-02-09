package ru.queuejw.mpl.content.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customFont
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.data.bsod.BSOD
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.SettingsFeedbackBinding

class FeedbackSettingsFragment : Fragment() {

    private var _binding: SettingsFeedbackBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsFeedbackBinding.inflate(inflater, container, false)
        (requireActivity() as SettingsActivity).setText(getString(R.string.feedback))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFont()
        initViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupFont() {
        customFont?.let {
            binding.save1bsod.typeface = it
            binding.save5bsod.typeface = it
            binding.save10bsod.typeface = it
            binding.saveallbsods.typeface = it
            binding.showBsodInfo.typeface = it
            binding.deleteBsodInfo.typeface = it
            binding.sendFeedbackSwitch.typeface = it
            binding.showErrorDetailsOnBsodSwitch.typeface = it
            binding.feedbackLabel.typeface = it
            binding.sendFeedbackLabel.typeface = it
            binding.bsodDetailsLabel.typeface = it
            binding.text.typeface = it
            binding.numOfIssuesLabel.typeface = it
            binding.setCrashLogLimitBtn.typeface = it

        }
    }

    private fun initViews() {
        updateMaxLogsSize(binding.save1bsod, 0)
        updateMaxLogsSize(binding.save5bsod, 1)
        updateMaxLogsSize(binding.save10bsod, 2)
        updateMaxLogsSize(binding.saveallbsods, 3)

        binding.showBsodInfo.setOnClickListener {
            (requireActivity() as SettingsActivity).changeFragment(
                FeedbackListFragment(),
                "feedback_list"
            )
        }
        binding.deleteBsodInfo.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                BSOD.getData(requireActivity()).clearAllTables()
            }.start()
        }
        binding.sendFeedbackSwitch.apply {
            isChecked = PREFS.isFeedbackEnabled
            text = if (PREFS.isFeedbackEnabled) getString(R.string.on) else getString(R.string.off)
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.isFeedbackEnabled = isChecked
                text = if (isChecked) getString(R.string.on) else getString(R.string.off)
            }
        }
        binding.setCrashLogLimitBtn.apply {
            setButtonText(this)
            setOnClickListener {
                binding.chooseBsodInfoLimit.visibility = View.VISIBLE
                visibility = View.GONE
            }
        }
        binding.showErrorDetailsOnBsodSwitch.apply {
            isChecked = PREFS.bsodOutputEnabled
            text = if (PREFS.bsodOutputEnabled) getString(R.string.on) else getString(R.string.off)
            setOnCheckedChangeListener { _, isChecked ->
                PREFS.bsodOutputEnabled = isChecked
                text = if (isChecked) getString(R.string.on) else getString(R.string.off)
            }
        }
    }

    private fun updateMaxLogsSize(view: View, value: Int) {
        view.setOnClickListener {
            PREFS.maxCrashLogs = value
            binding.chooseBsodInfoLimit.visibility = View.GONE
            binding.setCrashLogLimitBtn.visibility = View.VISIBLE
            setButtonText(binding.setCrashLogLimitBtn)
        }
    }

    private fun setButtonText(button: MaterialButton) {
        button.text = when (PREFS.maxCrashLogs) {
            1 -> getString(R.string.feedback_limit_1)
            2 -> getString(R.string.feedback_limit_2)
            3 -> getString(R.string.feedback_limit_3)
            else -> getString(R.string.feedback_limit_0)
        }
    }
}