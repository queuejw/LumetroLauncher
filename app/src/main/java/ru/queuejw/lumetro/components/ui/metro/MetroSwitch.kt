package ru.queuejw.lumetro.components.ui.metro

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.materialswitch.MaterialSwitch
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.ColorManager

class MetroSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MaterialSwitch(context, attrs) {

    private var colorManager: ColorManager? = null
    private var defaultTrack: Drawable? =
        ContextCompat.getDrawable(context, R.drawable.switch_track)
    private var coloredDrawable: Drawable? = null
    private var blackColor: Int? = ContextCompat.getColor(context, android.R.color.black)

    init {
        updateDrawable()
    }

    fun updateDrawable() {
        val isLight = ContextCompat.getColor(context, R.color.textColor) == blackColor
        colorManager = ColorManager()
        coloredDrawable = DrawableCompat.wrap(
            ContextCompat.getDrawable(context, R.drawable.switch_track)!!.mutate()
        ).also {
            DrawableCompat.setTintMode(
                it,
                if (isLight) PorterDuff.Mode.DARKEN else PorterDuff.Mode.LIGHTEN
            )
            DrawableCompat.setTint(it, colorManager!!.getAccentColor(context))
        }
        changeDrawable(isChecked)
    }

    override fun setChecked(checked: Boolean) {
        changeDrawable(checked)
        super.setChecked(checked)
    }

    private fun changeDrawable(isChecked: Boolean) {
        trackDrawable = if (isChecked) {
            coloredDrawable
        } else {
            defaultTrack
        }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        defaultTrack = null
        coloredDrawable = null
        blackColor = null
        colorManager = null
    }

    fun updateText() {
        text =
            if (isChecked) context.getString(R.string.on) else context.getString(R.string.off)
    }
}