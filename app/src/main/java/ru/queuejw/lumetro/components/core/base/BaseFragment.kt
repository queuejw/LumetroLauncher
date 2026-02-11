package ru.queuejw.lumetro.components.core.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import ru.queuejw.lumetro.components.prefs.Prefs

abstract class BaseFragment<viewBinding : ViewBinding?> : Fragment() {

    private var _prefs: Prefs? = null
    val prefs get() = _prefs!!

    // in future i want to add a font manager

    private var _binding: viewBinding? = null
    val binding get() = _binding!!

    protected abstract fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): viewBinding?

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _prefs = Prefs(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = getFragmentViewBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        _prefs = null
        super.onDetach()
    }
}