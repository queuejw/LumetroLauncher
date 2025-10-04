package ru.queuejw.lumetro.components.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import ru.queuejw.lumetro.components.adapters.diff.TilesDIffUtilCallback
import ru.queuejw.lumetro.components.adapters.holders.TileHolder
import ru.queuejw.lumetro.components.adapters.holders.TilePlaceholder
import ru.queuejw.lumetro.components.adapters.viewtypes.TileViewTypes
import ru.queuejw.lumetro.components.core.TileManager
import ru.queuejw.lumetro.components.core.icons.IconProvider
import ru.queuejw.lumetro.components.itemtouch.ItemTouchHelperAdapter
import ru.queuejw.lumetro.components.ui.window.TileWindow
import ru.queuejw.lumetro.databinding.TileBinding
import ru.queuejw.lumetro.databinding.TilePlaceholderBinding
import ru.queuejw.lumetro.model.TileEntity
import java.util.Collections

interface TilesAdapterInterface {
    fun onListUpdate(newList: MutableList<TileEntity>)
    fun saveTilesFunction(list: MutableList<TileEntity>)
    fun editModeFunction(boolean: Boolean)
    fun tileWindowFunction(boolean: Boolean)
    fun onTileClick(entity: TileEntity)
    fun showTileSettingsScreen(entity: TileEntity)
}

class TilesAdapter(
    private var adapterInterface: TilesAdapterInterface,
    private var data: MutableList<TileEntity>,
    private val iconSizes: Triple<Int, Int, Int>,
    private val defaultTileCornerRadius: Float,
    private val isMoreTilesEnabled: Boolean,
    private val accentColor: Int,
    private val iconProvider: IconProvider,
    val editModeEnabled: Boolean,
    private val editModeAnimation: Boolean
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemTouchHelperAdapter {
    var isEditMode = false

    private var userTiles: List<TileEntity> = createUserTiles()
    private var lastUserTilePosition: Int = 0
    private val tileManager = TileManager()

    fun getTilesList(): MutableList<TileEntity> = data

    init {
        setHasStableIds(true)
    }

    private fun createUserTiles(tiles: List<TileEntity> = data): List<TileEntity> {
        val list = tiles.filter { it.tileType != TileViewTypes.TYPE_PLACEHOLDER.type }
        if (list.isNotEmpty()) {
            lastUserTilePosition = list.last().tilePosition
        }

        return list
    }

    fun getItemPositionById(item: TileEntity): Int {
        val currentIndex = data.indexOfFirst { it.id == item.id }
        return if (currentIndex == -1) 0
        else currentIndex
    }

    private fun getTileWindow(item: TileEntity): TileWindow {
        return object : TileWindow(item, isMoreTilesEnabled) {

            override fun onTileWindowDismiss() {
                adapterInterface.tileWindowFunction(true)
            }

            override fun onTileResizeClick() {
                val position = getItemPositionById(item)
                data[position].tileSize = when (data[position].tileSize) {
                    0 -> 1
                    1 -> 2
                    2 -> 0
                    else -> 0
                }
                adapterInterface.saveTilesFunction(data)
                notifyItemChanged(position)
            }

            override fun onTileUnpinClick() {
                val position = getItemPositionById(item)
                val placeholder = tileManager.getPlaceholderItem(data, position)
                val currentId = item.id
                placeholder.id = currentId
                data[position] = placeholder
                notifyItemChanged(position)
                adapterInterface.saveTilesFunction(data)
            }

            override fun onTileSettingsClick() {
                adapterInterface.showTileSettingsScreen(item)
            }

            override fun onTileWindowEnter() {
                adapterInterface.tileWindowFunction(false)
            }
        }
    }

    private fun refreshTilesByViewType(viewType: Int) {
        for (i in 0..<itemCount) {
            if (getItemViewType(i) == viewType) notifyItemChanged(i)
        }
    }

    fun updateData(newData: MutableList<TileEntity>) {
        if (newData.isEmpty()) return
        DiffUtil.calculateDiff(TilesDIffUtilCallback(data, newData), false).apply {
            data = newData
            userTiles = createUserTiles(newData)
            dispatchUpdatesTo(this@TilesAdapter)
        }
        adapterInterface.onListUpdate(newData)
    }

    fun setAdapterEditMode(boolean: Boolean) {
        isEditMode = boolean
        adapterInterface.editModeFunction(isEditMode)
        refreshTilesByViewType(TileViewTypes.TYPE_PLACEHOLDER.type)
        if (boolean) {
            notifyItemRangeInserted(lastUserTilePosition + 3, data.size - userTiles.size)
        } else {
            notifyItemRangeRemoved(lastUserTilePosition + 3, data.size - userTiles.size)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position].tileType) {
            -1 -> TileViewTypes.TYPE_PLACEHOLDER.type
            else -> TileViewTypes.TYPE_DEFAULT.type
        }

    }

    override fun getItemId(position: Int): Long {
        return data[position].id
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TileViewTypes.TYPE_PLACEHOLDER.type -> TilePlaceholder(
                TilePlaceholderBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            else -> TileHolder(TileBinding.inflate(inflater, parent, false))
        }
    }

    private fun setTileIconSize(imageView: View, tileSize: Int) {
        imageView.layoutParams.apply {
            when (tileSize) {
                0 -> {
                    width = if (!isMoreTilesEnabled) iconSizes.second else iconSizes.first
                    height = if (!isMoreTilesEnabled) iconSizes.second else iconSizes.first
                }

                else -> {
                    width = iconSizes.third
                    height = iconSizes.third
                }
            }
        }
    }

    private fun setTileLabelVisibility(holder: TileHolder, tileSize: Int): Boolean {
        return when (tileSize) {
            0 -> {
                holder.label.visibility = View.GONE
                true
            }

            1 -> {
                if (isMoreTilesEnabled) {
                    holder.label.visibility = View.GONE
                    return false
                }
                holder.label.visibility = View.VISIBLE
                true
            }

            else -> {
                holder.label.visibility = View.VISIBLE
                true
            }
        }
    }

    private fun setTileCardColor(holder: TileHolder, customColor: String?) {
        holder.card.setCardBackgroundColor(customColor?.toColorInt() ?: accentColor)
    }

    private fun setTileCardCornerRadius(holder: TileHolder, item: TileEntity) {
        holder.card.apply {
            shapeAppearanceModel =
                ShapeAppearanceModel.builder().setAllCorners(
                    CornerFamily.ROUNDED,
                    if (item.tileCornerRadius != -1) item.tileCornerRadius.toFloat() else defaultTileCornerRadius
                )
                    .build()
        }
    }

    private fun loadTileIcon(holder: TileHolder, mPackage: String?) {
        if (mPackage != null) {
            holder.icon.apply {
                this.load(iconProvider.getIconForPackage(this.context, mPackage))
            }
        }
    }

    private fun setTileLabel(holder: TileHolder, str: String?) {
        holder.label.text = str
    }


    private fun setTileOnClick(holder: TileHolder, item: TileEntity) {
        holder.itemView.setOnClickListener {
            if (isEditMode) {
                showTilePopup(item, holder)
            } else {
                adapterInterface.onTileClick(item)
            }
        }
    }

    private fun bindDefaultTile(
        position: Int,
        holder: TileHolder
    ) {
        val item = data[position]
        holder.let {
            setTileCardCornerRadius(it, item)
            setTileCardColor(it, item.tileColor)
            setTileIconSize(it.icon, item.tileSize)
            loadTileIcon(it, item.tilePackage)
            if (setTileLabelVisibility(holder, item.tileSize)) {
                setTileLabel(holder, item.tileLabel)
            }
        }
        holder.itemView.apply {
            setTileOnClick(holder, item)
            setOnLongClickListener {
                if (tileOnLongClick()) {
                    callOnClick()
                }
                true
            }
        }
    }

    private fun showTilePopup(item: TileEntity, holder: TileHolder) {
        val tileWindow = getTileWindow(item)
        tileWindow.showTilePopupWindow(holder.itemView)
    }

    private fun tileOnLongClick(): Boolean {
        if (!isEditMode && editModeEnabled) {
            setAdapterEditMode(true)
            return true
        }
        return false
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        holder.itemView.animate()?.cancel()
        holder.itemView.clearAnimation()
        super.onViewRecycled(holder)
    }

    private fun bindPlaceholderTile(
        holder: TilePlaceholder
    ) {
        holder.card.animate().alpha(if (isEditMode) 1f else 0f).setDuration(125).start()
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (data[position].tileType) {
            TileViewTypes.TYPE_PLACEHOLDER.type -> bindPlaceholderTile(holder as TilePlaceholder)
            else -> bindDefaultTile(position, holder as TileHolder)
        }
    }

    override fun getItemCount(): Int {
        return if (!isEditMode) lastUserTilePosition + 3 else data.size
    }


    override fun onItemMove(
        fromHolder: RecyclerView.ViewHolder,
        toHolder: RecyclerView.ViewHolder
    ): Boolean {
        if (!isEditMode || !editModeEnabled) return false
        if (fromHolder == toHolder) return false
        if (toHolder.itemViewType == TileViewTypes.TYPE_DEFAULT.type) return false
        val from = fromHolder.bindingAdapterPosition
        val to = toHolder.bindingAdapterPosition
        Collections.swap(data, fromHolder.bindingAdapterPosition, toHolder.bindingAdapterPosition)
        notifyItemMoved(from, to)
        return true
    }


    override fun onItemDismiss(position: Int) {
    }

    override fun onDragAndDropCompleted() {
        adapterInterface.saveTilesFunction(data)
        updateData(data.toList().toMutableList())
    }
}