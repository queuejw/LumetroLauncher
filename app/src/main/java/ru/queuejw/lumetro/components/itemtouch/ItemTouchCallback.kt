package ru.queuejw.lumetro.components.itemtouch

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ru.queuejw.lumetro.components.adapters.TilesAdapter
import ru.queuejw.lumetro.components.adapters.viewtypes.TileViewTypes

class ItemTouchCallback(private val mAdapter: TilesAdapter) :
    ItemTouchHelper.Callback() {
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        if (viewHolder.itemViewType == TileViewTypes.TYPE_PLACEHOLDER.type) {
            return 0
        }
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        if (!mAdapter.isEditMode || !mAdapter.editModeEnabled) return false
        return if (viewHolder.itemViewType != -TileViewTypes.TYPE_PLACEHOLDER.type) {
            mAdapter.onItemMove(viewHolder, target)
        } else {
            false
        }
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun isLongPressDragEnabled(): Boolean {
        return mAdapter.isEditMode && mAdapter.editModeEnabled
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {

    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        mAdapter.onDragAndDropCompleted()
    }
}