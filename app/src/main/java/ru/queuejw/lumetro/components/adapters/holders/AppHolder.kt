package ru.queuejw.lumetro.components.adapters.holders

import androidx.recyclerview.widget.RecyclerView
import ru.queuejw.lumetro.databinding.AppHolderBinding

class AppHolder(binding: AppHolderBinding) : RecyclerView.ViewHolder(binding.root) {
    val frame = binding.frame
    val icon = binding.appIcon
    val label = binding.appLabel
}