package ru.queuejw.lumetro.components.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.queuejw.lumetro.components.adapters.holders.ErrorItemHolder
import ru.queuejw.lumetro.databinding.ErrorHolderBinding
import ru.queuejw.lumetro.model.ErrorEntity

class ErrorAdapter(
    private var data: MutableList<ErrorEntity>,
    private val onShareClick: (String) -> Unit,
    private val onCopyClick: (String) -> Unit,
    private val onDeleteClick: (ErrorEntity) -> Unit
) : RecyclerView.Adapter<ErrorItemHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ErrorItemHolder = ErrorItemHolder(
        ErrorHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(
        holder: ErrorItemHolder,
        position: Int
    ) {
        val item = data[position]
        holder.apply {
            textView.text = item.details
            shareBtn.setOnClickListener {
                onShareClick(item.details)
            }
            deleteBtn.setOnClickListener {
                onDeleteClick(item)
                data.remove(item)
                notifyDataSetChanged()
            }
            copyBtn.setOnClickListener {
                onCopyClick(item.details)
            }
        }
    }

    override fun getItemCount(): Int = data.size
}