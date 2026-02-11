package ru.queuejw.lumetro.components.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.MutableScatterMap
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import ru.queuejw.lumetro.components.adapters.holders.AppFLEHolder
import ru.queuejw.lumetro.components.core.AnimationUtils
import ru.queuejw.lumetro.components.core.icons.IconProvider
import ru.queuejw.lumetro.databinding.AppFleHolderBinding
import ru.queuejw.lumetro.model.App

class AppFLEAdapter(
    val data: List<App>,
    private var iconProvider: IconProvider,
    private val accentColor: Int,
    private val onCheckboxChanged: (App, Boolean) -> Unit,

    ) : RecyclerView.Adapter<AppFLEHolder>() {

    private val selectedItems = MutableScatterMap<Int, App>()

    @SuppressLint("NotifyDataSetChanged")
    fun unpinAll() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun pinAll(): List<App> {
        data.forEachIndexed { pos, item ->
            selectedItems.put(pos, item)
        }
        notifyDataSetChanged()
        return data
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AppFLEHolder {
        return AppFLEHolder(
            AppFleHolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    private fun isAppSelected(position: Int, app: App): Boolean {
        return if (selectedItems.contains(position)) {
            selectedItems[position]?.mPackage == app.mPackage
        } else {
            false
        }
    }

    private fun setHolderLabel(holder: AppFLEHolder, string: String) {
        holder.label.text = string
    }

    private fun setHolderColor(holder: AppFLEHolder, color: Int) {
        holder.frame.setBackgroundColor(color)
    }

    private fun setHolderIcon(holder: AppFLEHolder, mPackage: String) {
        holder.icon.apply {
            this.load(iconProvider.getIconForPackage(this.context, mPackage))
        }
    }

    private fun setHolderCheckbox(holder: AppFLEHolder, position: Int, item: App) {
        holder.checkbox.apply {
            isChecked = isAppSelected(position, item)
            setOnCheckedChangeListener { v, isChecked ->
                if (!v.isShown) return@setOnCheckedChangeListener
                selectedItems.apply {
                    if (isChecked) {
                        put(position, item)
                    } else {
                        remove(position)
                    }
                }
                onCheckboxChanged(item, isChecked)
            }
        }
    }

    private fun setHolderOnClick(holder: AppFLEHolder) {
        holder.apply {
            itemView.setOnClickListener {
                this.checkbox.isChecked = !this.checkbox.isChecked
            }
        }
    }

    private fun setHolderAnim(itemView: View) {
        AnimationUtils.setViewInteractAnimation(itemView, 5)
    }

    override fun onBindViewHolder(
        holder: AppFLEHolder,
        position: Int
    ) {
        val item = data[position]
        holder.apply {
            setHolderLabel(this, item.mName)
            setHolderColor(this, accentColor)
            setHolderIcon(this, item.mPackage!!)
            setHolderCheckbox(holder, position, item)
            setHolderOnClick(this)
            setHolderAnim(this.itemView)
        }
    }

    override fun getItemCount(): Int = data.size

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        selectedItems.clear()
        super.onDetachedFromRecyclerView(recyclerView)
    }

}