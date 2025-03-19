package ru.queuejw.mpl.content.bsod.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.queuejw.mpl.Main
import ru.queuejw.mpl.content.bsod.CriticalErrorActivity
import ru.queuejw.mpl.databinding.RecoveryFragmentBinding

class RecoveryFragment : Fragment() {

    private var _binding: RecoveryFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = RecoveryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUi()
    }

    private fun setUi() {
        binding.restartButton.setOnClickListener {
            val intent = Intent(requireActivity(), Main::class.java)
            intent.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
        binding.advancedOptionsButton.setOnClickListener {
            (requireActivity() as CriticalErrorActivity?)?.startRecoveryOptions()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}