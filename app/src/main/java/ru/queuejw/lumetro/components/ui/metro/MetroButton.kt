package ru.queuejw.lumetro.components.ui.metro

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import ru.queuejw.lumetro.components.core.AnimationUtils
import ru.queuejw.lumetro.components.core.ColorManager

class MetroButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MaterialButton(context, attrs) {

    private var transparentColor: Int? =
        ContextCompat.getColor(context, android.R.color.transparent)
    private val colorManager = ColorManager()
    private var accentColor: Int? = getAccentColor()

    private fun getAccentColor(): Int? {
        return colorManager.getAccentColor(context)
    }

    init {
        AnimationUtils.setViewInteractAnimation(this, 7)
    }

    fun updateColor(color: Int) {
        accentColor = color
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.apply {
            when (action) {
                MotionEvent.ACTION_DOWN -> accentColor?.let {
                    setBackgroundColor(it)
                    return true
                }

                MotionEvent.ACTION_UP -> transparentColor?.let {
                    setBackgroundColor(it)
                    if (isEnabled) performClick()
                }

                MotionEvent.ACTION_CANCEL -> transparentColor?.let {
                    setBackgroundColor(it)
                    return true
                }

                else -> return false
            }
        }
        return false
    }


    override fun onDetachedFromWindow() {
        setOnClickListener(null)
        transparentColor = null
        accentColor = null
        super.onDetachedFromWindow()
    }
}