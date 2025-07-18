package ru.queuejw.lumetro.components.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import ru.queuejw.lumetro.components.adapters.diff.AppsDiffCallback
import ru.queuejw.lumetro.components.adapters.holders.AppHolder
import ru.queuejw.lumetro.components.adapters.holders.HeaderHolder
import ru.queuejw.lumetro.components.adapters.viewtypes.AppViewTypes
import ru.queuejw.lumetro.components.core.AnimationUtils
import ru.queuejw.lumetro.components.core.icons.IconProvider
import ru.queuejw.lumetro.databinding.AppHolderBinding
import ru.queuejw.lumetro.databinding.HeaderHolderBinding
import ru.queuejw.lumetro.model.App

class AppAdapter(
    var data: List<App>,
    private val iconProvider: IconProvider,
    private val accentColor: Int,
    private val onAppClick: (Int, App) -> Unit,
    private val onAppLongClick: (Int, App, View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun updateData(newData: List<App>) {
        val callback = AppsDiffCallback(data, newData)
        val result = DiffUtil.calculateDiff(callback, false)
        data = newData
        result.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position].viewType) {
            -1 -> AppViewTypes.TYPE_HEADER.type
            else -> AppViewTypes.TYPE_APP.type
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            AppViewTypes.TYPE_HEADER.type -> HeaderHolder(
                HeaderHolderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            else -> AppHolder(
                AppHolderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    private fun setAppHolderIcon(holder: AppHolder, mPackage: String) {
        holder.icon.apply {
            this.load(iconProvider.getIconForPackage(this.context, mPackage))
        }
    }

    private fun setAppHolderAnimation(itemView: View) {
        itemView.apply {
            alpha = 1f
        }
    }

    private fun setAppHolderText(holder: AppHolder, string: String) {
        holder.label.text = string
    }

    private fun setAppHolderColor(holder: AppHolder, color: Int) {
        holder.frame.setBackgroundColor(color)
    }

    private fun setAppHolderOnClick(holder: AppHolder, position: Int, item: App) {
        holder.itemView.setOnClickListener {
            onAppClick(position, item)
        }
    }

    private fun setAppHolderOnLongClick(holder: AppHolder, position: Int, item: App) {
        holder.itemView.setOnLongClickListener {
            onAppLongClick(position, item, holder.itemView)
            true
        }
    }

    private fun bindApp(holder: AppHolder, position: Int) {
        val item = data[position]
        holder.let { h ->
            setAppHolderIcon(h, item.mPackage!!)
            setAppHolderText(h, item.mName)
            setAppHolderColor(h, accentColor)
            setAppHolderOnClick(h, position, item)
            setAppHolderOnLongClick(h, position, item)
            h.itemView.apply {
                setAppHolderAnimation(this)
                AnimationUtils.Companion.setViewInteractAnimation(this, 5)
            }
        }
    }

    private fun bindLetter(holder: HeaderHolder, position: Int) {
        holder.apply {
            accentColor.apply {
                card.strokeColor = this
                letter.setTextColor(this)
            }
            letter.text = data[position].mName
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        if (holder is HeaderHolder) {
            bindLetter(holder, position)
        } else {
            bindApp(holder as AppHolder, position)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}