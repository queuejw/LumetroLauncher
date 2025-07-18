package ru.queuejw.lumetro.components.itemtouch


interface ItemTouchHelperViewHolder {
    /**
     * Called when the [ItemTouchCallback] first registers an item as being moved or swiped.
     * Implementations should update the item view to indicate it's active state.
     */
    fun onItemSelected()

    /**
     * Called when the [ItemTouchCallback] has completed the move or swipe, and the active item
     * state should be cleared.
     */
    fun onItemClear()
}