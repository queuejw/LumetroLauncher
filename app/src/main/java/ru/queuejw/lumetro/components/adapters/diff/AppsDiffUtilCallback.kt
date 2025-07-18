package ru.queuejw.lumetro.components.adapters.diff

import androidx.recyclerview.widget.DiffUtil
import ru.queuejw.lumetro.model.App

class AppsDiffCallback(val oldData: List<App>, val newData: List<App>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldData.size
    }

    override fun getNewListSize(): Int {
        return newData.size
    }

    override fun areItemsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean {
        return oldData[oldItemPosition].hashCode() == newData[newItemPosition].hashCode()
    }

    override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean {
        return oldData[oldItemPosition].mPackage == newData[newItemPosition].mPackage
    }

}