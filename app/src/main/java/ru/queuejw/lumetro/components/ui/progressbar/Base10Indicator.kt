//https://github.com/shadowalker77/wp7progressbar

package ru.queuejw.lumetro.components.ui.progressbar

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import ru.queuejw.lumetro.components.core.Utils

class Base10Indicator(
    context: Context?,
    indicatorHeight: Int,
    private val color: Int,
    radius: Int
) : View(context) {
    init {
        initialize(indicatorHeight, radius)
    }

    private fun initialize(indicatorHeight: Int, radius: Int) {
        this.background = getCube(radius)
        val layoutParams = LinearLayout.LayoutParams(
            Utils.px2dp(context, indicatorHeight),
            Utils.px2dp(context, indicatorHeight)
        )
        //        layoutParams.rightMargin = Utils.px2dp(getContext(), (int) (1.7f * indicatorHeight));
        this.layoutParams = layoutParams
        //        startAnim(0, 0);
//        removeAnim();
    }

    private fun getCube(radius: Int): GradientDrawable {
        val drawable = GradientDrawable().also {
            it.shape = GradientDrawable.RECTANGLE
            it.setColor(color)
            it.cornerRadius = Utils.px2dp(context, radius).toFloat()
        }
        return drawable
    }
}
