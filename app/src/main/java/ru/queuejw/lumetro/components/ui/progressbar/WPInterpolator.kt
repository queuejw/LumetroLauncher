//https://github.com/shadowalker77/wp7progressbar
package ru.queuejw.lumetro.components.ui.progressbar

import android.view.animation.Interpolator
import kotlin.math.pow

class WPInterpolator : Interpolator {
    override fun getInterpolation(v: Float): Float {
        if (v > 0.3f && v < 0.70f) return ((-(v - 0.5) / 6) + 0.5f).toFloat()
        return ((-4) * (v - 0.5).pow(3.0) + 0.5).toFloat()
    }
}
