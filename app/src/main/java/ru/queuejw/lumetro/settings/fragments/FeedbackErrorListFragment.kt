package ru.queuejw.lumetro.settings.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.FeedbackManager
import ru.queuejw.lumetro.components.adapters.ErrorAdapter
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.components.core.db.error.ErrorDatabase
import ru.queuejw.lumetro.components.ui.dialog.MetroDialog
import ru.queuejw.lumetro.databinding.SettingsFeedbackErrorListBinding
import ru.queuejw.lumetro.model.ErrorEntity

class FeedbackErrorListFragment : BaseFragment<SettingsFeedbackErrorListBinding>() {

    private var errorAdapter: ErrorAdapter? = null
    private val feedbackManager = FeedbackManager()
    private var db: ErrorDatabase? = null

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): SettingsFeedbackErrorListBinding? {
        return SettingsFeedbackErrorListBinding.inflate(inflater, container, false)
    }

    private fun copyErrorText(error: String, context: Context) {
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        val clip = ClipData.newPlainText("Lumetro Error", error)
        clipboard?.setPrimaryClip(clip)
        successCopyDialog(context)
    }

    private fun successCopyDialog(context: Context) {
        val d = MetroDialog.newInstance(Gravity.TOP).apply {
            setMessage(context.getString(R.string.dialog_copy_success))
            setPositiveDialogListener(context.getString(android.R.string.ok)) {
                this.dismiss()
            }
        }
        d.show(childFragmentManager, "copy")
    }

    private fun deleteLog(item: ErrorEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            db?.getErrorDao()?.deleteItem(item)

            if (db?.getErrorDao()?.getErrorData()?.isEmpty() == true) {
                withContext(Dispatchers.Main) {
                    binding.emptyListTextview.visibility = View.VISIBLE
                    binding.errorList.visibility = View.GONE
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {
            db = ErrorDatabase.getErrorData(view.context)
            val list = db!!.getErrorDao().getErrorData()
            if (list.isEmpty()) {
                withContext(Dispatchers.Main) {
                    binding.emptyListTextview.visibility = View.VISIBLE
                    binding.errorList.visibility = View.GONE
                }
            } else {
                errorAdapter = ErrorAdapter(
                    data = list,
                    onShareClick = { text ->
                        context?.let {
                            feedbackManager.sendEmail(text, it)
                        }
                    },
                    onCopyClick = { text ->
                        context?.let {
                            copyErrorText(text, it)
                        }
                    },
                    onDeleteClick = {
                        deleteLog(it)
                    }
                )
                val lm = LinearLayoutManager(view.context)
                withContext(Dispatchers.Main) {
                    binding.errorList.apply {
                        layoutManager = lm
                        adapter = errorAdapter
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.errorList.apply {
            adapter = null
            layoutManager = null
        }
        errorAdapter = null
        db = null
        super.onDestroyView()
    }
}