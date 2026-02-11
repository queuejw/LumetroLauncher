package ru.queuejw.lumetro.components.ui.window

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import ru.queuejw.lumetro.components.core.Utils
import ru.queuejw.lumetro.databinding.TileWindowBinding
import ru.queuejw.lumetro.model.TileEntity

abstract class TileWindow(
    private val item: TileEntity,
    private val isMoreTilesEnabled: Boolean
) {

    abstract fun onTileWindowDismiss()

    abstract fun onTileResizeClick()

    abstract fun onTileUnpinClick()

    abstract fun onTileSettingsClick()

    abstract fun onTileWindowEnter()

    private var popupWindow: PopupWindow? = null

    private fun setPopupWindowLayout(binding: TileWindowBinding) {
        binding.apply {
            applyResizeIcon(tileResize, item.tileSize)
            tileResize.setOnClickListener {
                onTileResizeClick()
                popupWindow?.dismiss()
            }
            tileUnpin.setOnClickListener {
                onTileUnpinClick()
                popupWindow?.dismiss()
            }
            tileSettings.setOnClickListener {
                onTileSettingsClick()
                popupWindow?.dismiss()
            }
        }
    }

    private fun getWindowBinding(context: Context): TileWindowBinding =
        TileWindowBinding.inflate(LayoutInflater.from(context))

    private fun getPopupWindowWidth(width: Int, context: Context): Int {
        return when {
            item.tileSize == 2 -> width
            isMoreTilesEnabled && item.tileSize == 0 -> width + Utils.px2dp(
                context,
                48
            )

            else -> width + Utils.px2dp(context, 24)
        }
    }

    private fun getPopupWindowHeight(height: Int, context: Context): Int {
        return when {
            item.tileSize == 2 -> height
            isMoreTilesEnabled && item.tileSize == 0 -> height + Utils.px2dp(
                context,
                48
            )

            else -> height + Utils.px2dp(context, 24)
        }
    }

    private fun getPopupWindow(root: View, width: Int, height: Int): PopupWindow =
        PopupWindow(root, width, height, true)

    private fun createPopupWindow(width: Int, height: Int, context: Context): PopupWindow {
        var binding: TileWindowBinding? = getWindowBinding(context)
        binding!!
        setPopupWindowLayout(binding)
        val localPopupWindow =
            getPopupWindow(
                binding.root,
                getPopupWindowWidth(width, context),
                getPopupWindowHeight(height, context)
            )
        localPopupWindow.setOnDismissListener {
            onTileWindowDismiss()
            binding?.apply {
                tileResize.setOnClickListener(null)
                tileUnpin.setOnClickListener(null)
                tileSettings.setOnClickListener(null)
            }
            binding = null
            popupWindow = null
        }
        return localPopupWindow
    }

    private fun applyResizeIcon(view: View, tileSize: Int) {
        view.rotation = when (tileSize) {
            0 -> -135f
            1 -> -180f
            else -> 0f
        }
    }

    fun showTilePopupWindow(view: View) {
        if (popupWindow?.isShowing == true) return

        if (popupWindow == null) {
            popupWindow = createPopupWindow(view.measuredWidth, view.measuredHeight, view.context)
        }

        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val popupX = location[0] + (view.measuredWidth - popupWindow!!.width) / 2
        val popupY = location[1] + (view.measuredHeight - popupWindow!!.height) / 2

        popupWindow?.showAtLocation(view, Gravity.NO_GRAVITY, popupX, popupY)
        onTileWindowEnter()
    }
}