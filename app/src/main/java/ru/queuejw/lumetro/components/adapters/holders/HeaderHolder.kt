package ru.queuejw.lumetro.components.adapters.holders

import androidx.recyclerview.widget.RecyclerView
import ru.queuejw.lumetro.databinding.HeaderHolderBinding

class HeaderHolder(binding: HeaderHolderBinding) : RecyclerView.ViewHolder(binding.root) {
    val card = binding.card
    val letter = binding.letterLabel
}