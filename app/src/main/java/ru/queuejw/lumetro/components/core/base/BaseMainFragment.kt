package ru.queuejw.lumetro.components.core.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import ru.queuejw.lumetro.components.viewmodels.MainViewModel

abstract class BaseMainFragment<viewBinding : ViewBinding?> : BaseFragment<viewBinding>() {

    lateinit var viewModel: MainViewModel

    abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): viewBinding?

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): viewBinding? {
        return createBinding(inflater, container)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }

    fun applyInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}