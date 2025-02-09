package ru.queuejw.mpl.content.settings.fragments

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customFont
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.data.bsod.BSOD
import ru.queuejw.mpl.content.data.bsod.BSODEntity
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.BsodItemBinding
import ru.queuejw.mpl.databinding.SettingsFeedbackListBinding
import ru.queuejw.mpl.helpers.utils.Utils

class FeedbackListFragment : Fragment() {

    private var _binding: SettingsFeedbackListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsFeedbackListBinding.inflate(inflater, container, false)
        (requireActivity() as SettingsActivity).setText(getString(R.string.saved_issues))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = BSOD.getData(requireContext())
            val mAdapter = BSODAdapter(db.getDao().getBsodList(), db, requireContext())
            withContext(Dispatchers.Main) {
                binding.bsodlistRecycler.apply {
                    layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.VERTICAL,
                        false
                    )
                    adapter = mAdapter
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class BSODAdapter(
        private var data: List<BSODEntity>,
        private val db: BSOD,
        private val context: Context
    ) :
        RecyclerView.Adapter<BSODAdapter.ViewHolder>() {

        inner class ViewHolder(val holderBinding: BsodItemBinding) :
            RecyclerView.ViewHolder(holderBinding.root) {
            init {
                if (PREFS.customFontInstalled) {
                    customFont?.let {
                        holderBinding.date.typeface = it
                        holderBinding.log.typeface = it
                    }
                }
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        private fun updateList() {
            lifecycleScope.launch(Dispatchers.IO) {
                val newList = db.getDao().getBsodList()
                withContext(Dispatchers.Main) {
                    data = newList
                    notifyDataSetChanged()
                }
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                BsodItemBinding.inflate(
                    LayoutInflater.from(viewGroup.context),
                    viewGroup,
                    false
                )
            )
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val item = data[position]
            viewHolder.holderBinding.date.text = item.date.toString()
            viewHolder.holderBinding.log.text = item.log
            viewHolder.holderBinding.delete.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    db.getDao().removeLog(item)
                    viewHolder.itemView.post {
                        updateList()
                    }
                }
            }
            viewHolder.itemView.setOnClickListener {
                showDialog(item.log)
            }
            viewHolder.holderBinding.share.setOnClickListener {
                Utils.sendCrash(item.log, requireActivity())
            }
        }

        private fun showDialog(text: String) {
            MaterialAlertDialogBuilder(context)
                .setMessage(text)
                .setPositiveButton(getString(R.string.copy)) { _: DialogInterface?, _: Int ->
                    copyError(text)
                }.setNegativeButton(getString(android.R.string.cancel), null).show()
        }

        private fun copyError(error: String) {
            val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("MPL_Error", error)
            clipboard.setPrimaryClip(clip)
        }

        override fun getItemCount() = data.size
    }
}