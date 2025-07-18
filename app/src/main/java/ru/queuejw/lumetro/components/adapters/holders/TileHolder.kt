package ru.queuejw.lumetro.components.adapters.holders

import androidx.recyclerview.widget.RecyclerView
import ru.queuejw.lumetro.components.itemtouch.ItemTouchHelperViewHolder
import ru.queuejw.lumetro.databinding.TileBinding

class TileHolder(binding: TileBinding) : RecyclerView.ViewHolder(binding.root),
    ItemTouchHelperViewHolder {
    val card = binding.cardContainer
    val label = binding.tileLabel
    val icon = binding.tileIcon

    override fun onItemSelected() {
    }

    override fun onItemClear() {
    }
}