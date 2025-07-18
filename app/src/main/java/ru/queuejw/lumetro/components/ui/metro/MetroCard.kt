package ru.queuejw.lumetro.components.ui.metro

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import ru.queuejw.lumetro.components.core.ColorManager

class MetroCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MaterialCardView(context, attrs) {

    private var colorManager = ColorManager()
    private var accentColor: Int? = getAccentColor()

    private fun getAccentColor(): Int {
        return colorManager.getAccentColor(context)
    }

    init {
        strokeColor = accentColor!!
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        accentColor = null
    }
}