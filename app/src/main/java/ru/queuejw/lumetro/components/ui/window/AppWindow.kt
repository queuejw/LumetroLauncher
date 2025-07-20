package ru.queuejw.lumetro.components.ui.window

import android.animation.ObjectAnimator
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.doOnEnd
import ru.queuejw.lumetro.databinding.AppWindowBinding
import ru.queuejw.lumetro.model.App
import ru.queuejw.lumetro.model.TileEntity

abstract class AppWindow(
    private val app: App,
    private val list: MutableList<TileEntity>?
) {

    abstract fun onAppWindowDismiss()
    abstract fun onAppPinClick(app: App, view: View)
    abstract fun onAppInfoClick(app: App)
    abstract fun onAppUninstallClick(app: App)

    private var popupWindow: PopupWindow? = null
    private var top = false
    private var popupHeight: Int? = null

    private fun getPopupWindow(root: View): PopupWindow = PopupWindow(
        root,
        LinearLayoutCompat.LayoutParams.MATCH_PARENT,
        LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
        true
    )

    private fun getPopupHeight(popupWindow: PopupWindow): Int {
        val contentView = popupWindow.contentView
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return contentView.measuredHeight
    }

    private fun isPopupInTop(anchorView: View, popupHeight: Int): Boolean {
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorY = location[1]
        val displayMetrics = anchorView.context.resources.displayMetrics
        return (anchorY - popupHeight) < displayMetrics.heightPixels / 2
    }

    private fun setupLayout(binding: AppWindowBinding) {
        binding.apply {
            pinApp.apply {
                if (list?.any {
                        it.tilePackage == app.mPackage
                    } == true) {
                    this.apply {
                        isEnabled = false
                        alpha = 0.5f
                    }
                } else {
                    setOnClickListener {
                        onAppPinClick(app, it)
                        popupWindow?.dismiss()
                    }
                }
            }
            appInfo.setOnClickListener {
                onAppInfoClick(app)
                popupWindow?.dismiss()
            }
            uninstallApp.setOnClickListener {
                onAppUninstallClick(app)
                popupWindow?.dismiss()
            }
        }
    }

    fun animateAppPopupWindowEnter(view: View) {
        val anim =
            ObjectAnimator.ofFloat(view, "scaleY", 0f, 0.01f).setDuration(1)
        val anim2 =
            ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f).setDuration(200)
        val anim3 =
            ObjectAnimator.ofFloat(view, "scaleY", 0.01f, 1f).setDuration(400)
        anim.doOnEnd {
            anim2.doOnEnd {
                anim3.start()
            }
            anim2.start()
        }
        anim.start()
    }

    private fun createWindow(itemView: View) {
        var windowBinding: AppWindowBinding? =
            AppWindowBinding.inflate(LayoutInflater.from(itemView.context))
        popupWindow = getPopupWindow(windowBinding!!.root).also {
            it.setOnDismissListener {
                windowBinding = null
                onAppWindowDismiss()
            }
        }
        popupWindow?.apply {
            popupHeight = getPopupHeight(this).also {
                top = isPopupInTop(itemView, it)
                windowBinding?.root?.pivotY = if (top) 0f else it.toFloat()
            }
        }
        windowBinding?.let {
            setupLayout(it)
        }
    }

    fun showAppWindow(view: View) {
        if (popupWindow?.isShowing == true) return

        if (popupWindow == null) {
            createWindow(view)
        }
        popupWindow?.apply {
            animateAppPopupWindowEnter(this.contentView)
            popupHeight?.let {
                showAsDropDown(
                    view,
                    0,
                    if (top) 0 else (-it + -view.measuredHeight),
                    Gravity.CENTER
                )
            }
        }
    }
}