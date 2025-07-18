package ru.queuejw.lumetro.fle.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.queuejw.lumetro.databinding.FleTouRejectedBinding
import ru.queuejw.lumetro.fle.FirstLaunchExperienceActivity
import kotlin.system.exitProcess

class ToURejectedFragment : Fragment() {

    private var binding: FleTouRejectedBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FleTouRejectedBinding.inflate(inflater, container, false)
        (activity as FirstLaunchExperienceActivity?)?.apply {
            setAppBarText(null)
        }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.closeApp?.setOnClickListener {
            exitProcess(0)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}