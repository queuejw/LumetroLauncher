package ru.queuejw.lumetro.fle.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.databinding.FleCompleteSetupBinding
import ru.queuejw.lumetro.fle.FirstLaunchExperienceActivity
import ru.queuejw.lumetro.main.MainActivity

class FleCompleteSetupFragment : BaseFragment<FleCompleteSetupBinding>() {

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FleCompleteSetupBinding {
        return FleCompleteSetupBinding.inflate(inflater, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        prefs.fleProgress = 6
    }

    private fun destroyActivity() {
        activity?.apply {
            startActivity(Intent(this, MainActivity::class.java))
            finishAndRemoveTask()
        }
    }

    private fun closeFle() {
        prefs.isFirstLaunch = false
        prefs.fleProgress = 0
        (activity as FirstLaunchExperienceActivity?)?.setAppBarText(null)
        binding.apply {
            buttonLayout.animate().alpha(0f).setDuration(300).start()
            textLayout.animate().alpha(0f).setDuration(300).withEndAction {
                destroyActivity()
            }.start()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as FirstLaunchExperienceActivity?)?.setAppBarText(getString(R.string.fle_almost_done))
        binding.finishFleButton.setOnClickListener {
            closeFle()
        }
    }

    override fun onDestroyView() {
        binding.finishFleButton.setOnClickListener(null)
        super.onDestroyView()
    }
}