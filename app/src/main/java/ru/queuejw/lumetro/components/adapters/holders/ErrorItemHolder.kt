package ru.queuejw.lumetro.components.adapters.holders

import androidx.recyclerview.widget.RecyclerView
import ru.queuejw.lumetro.databinding.ErrorHolderBinding

class ErrorItemHolder(binding: ErrorHolderBinding) : RecyclerView.ViewHolder(binding.root) {
    val textView = binding.errorDetails
    val shareBtn = binding.shareData

    val copyBtn = binding.copyData
    val deleteBtn = binding.deleteData
}