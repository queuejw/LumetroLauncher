package ru.queuejw.lumetro.components.adapters.holders

import androidx.recyclerview.widget.RecyclerView
import ru.queuejw.lumetro.databinding.AppFleHolderBinding

class AppFLEHolder(binding: AppFleHolderBinding) : RecyclerView.ViewHolder(binding.root) {
    val frame = binding.frame
    val icon = binding.appIcon
    val label = binding.appLabel

    val checkbox = binding.checkbox
}