package ru.queuejw.lumetro.components.itemtouch

import androidx.recyclerview.widget.RecyclerView

interface ItemTouchHelperAdapter {
    /**
     * Called when an item has been dragged far enough to trigger a move. This is called every time
     * an item is shifted, and not at the end of a "drop" event.
     *
     * @param fromHolder The start position of the moved item.
     * @param toHolder Then end position of the moved item.
     * @see RecyclerView.findViewHolderForAdapterPosition
     * @see RecyclerView.ViewHolder.getAdapterPosition
     */
    fun onItemMove(fromHolder: RecyclerView.ViewHolder, toHolder: RecyclerView.ViewHolder): Boolean

    /**
     * Called when an item has been dismissed by a swipe.
     *
     * @param position The position of the item dismissed.
     * @see RecyclerView.findViewHolderForAdapterPosition
     * @see RecyclerView.ViewHolder.getAdapterPosition
     */
    fun onItemDismiss(position: Int)

    /**
     * Called when Drag and Drop is completed.
     *
     * @see RecyclerView.findViewHolderForAdapterPosition
     * @see RecyclerView.ViewHolder.getAdapterPosition
     */
    fun onDragAndDropCompleted()
}