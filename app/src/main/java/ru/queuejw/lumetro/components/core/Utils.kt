package ru.queuejw.lumetro.components.core

import android.content.Context

object Utils {
    @JvmStatic
    fun px2dp(context: Context, px: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (px * scale + 0.5f).toInt()
    }
}