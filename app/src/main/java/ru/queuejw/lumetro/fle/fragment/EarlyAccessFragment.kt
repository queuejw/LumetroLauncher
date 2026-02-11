package ru.queuejw.lumetro.fle.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.databinding.FleEarlyAccessBinding
import ru.queuejw.lumetro.fle.FirstLaunchExperienceActivity

class EarlyAccessFragment : BaseFragment<FleEarlyAccessBinding>() {

    private var textVisible = false

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FleEarlyAccessBinding {
        return FleEarlyAccessBinding.inflate(inflater, container, false)
    }

    private fun setUi() {
        binding.eaCheckbox.setOnCheckedChangeListener { _, bool ->
            (activity as FirstLaunchExperienceActivity?)?.setButtonState(1, !bool)
        }
        binding.eaFeedbackCheckbox.apply {
            isChecked = prefs.allowSaveErrorData
            setOnCheckedChangeListener { _, bool ->
                prefs.allowSaveErrorData = bool
            }
        }
        binding.feedbackHintText.setOnClickListener {
            if (textVisible) return@setOnClickListener
            textVisible = true
            binding.feedbackInfo.apply {
                visibility = View.VISIBLE
                animate().setDuration(400).alpha(0.5f).start()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as FirstLaunchExperienceActivity?)?.apply {
            setAppBarText(getString(R.string.early_access))
            nextFragment = 2
            previousFragment = 0
            setButtonState(0, false)
            setButtonState(1, true)
            animateBottomBar(true)
        }
        setUi()
        if (savedInstanceState != null) {
            binding.eaCheckbox.isChecked = savedInstanceState.getBoolean("checkbox", false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("checkbox", binding.eaCheckbox.isChecked)
    }
}