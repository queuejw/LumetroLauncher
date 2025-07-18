package ru.queuejw.lumetro.components.adapters.diff

import androidx.recyclerview.widget.DiffUtil
import ru.queuejw.lumetro.model.TileEntity

class TilesDIffUtilCallback(
    private val oldData: List<TileEntity>,
    private val newData: List<TileEntity>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldData.size

    override fun getNewListSize(): Int = newData.size

    override fun areItemsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean = oldData[oldItemPosition].id == newData[newItemPosition].id

    override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean =
        oldData[oldItemPosition].tilePackage == newData[newItemPosition].tilePackage && oldData[oldItemPosition].id == newData[newItemPosition].id
}