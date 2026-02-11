package ru.queuejw.lumetro.fle.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.databinding.FleTouRejectedBinding
import ru.queuejw.lumetro.fle.FirstLaunchExperienceActivity
import kotlin.system.exitProcess

class ToURejectedFragment : BaseFragment<FleTouRejectedBinding>() {

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FleTouRejectedBinding = FleTouRejectedBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as FirstLaunchExperienceActivity?)?.apply {
            setAppBarText(null)
        }
        binding.closeApp.setOnClickListener {
            exitProcess(0)
        }
        prefs.fleProgress = 0
    }
}