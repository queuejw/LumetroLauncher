package ru.queuejw.lumetro.components.core

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.prefs.Prefs

class ColorManager {

    private var accentColor: Int? = null
    private var onSurfaceColor: Int? = null

    fun clearColors() {
        accentColor = null
        onSurfaceColor = null
    }

    private fun getThemeColor(theme: Resources.Theme, colorAttr: Int): Int {
        TypedValue().apply {
            theme.resolveAttribute(
                colorAttr,
                this,
                true
            )
        }.also {
            return it.data
        }
    }

    fun getAccentColor(context: Context): Int =
        accentColor ?: run {
            val prefs = Prefs(context)
            val color = if (!prefs.dynamicColorEnabled) {
                prefs.accentColorValue.toColorInt()
            } else {
                getThemeColor(context.theme, androidx.appcompat.R.attr.colorPrimary)
            }
            accentColor = color
            color
        }

    fun getOnSurfaceColor(context: Context): Int =
        onSurfaceColor ?: run {
            val color =
                getThemeColor(context.theme, com.google.android.material.R.attr.colorOnSurface)
            onSurfaceColor = color
            color
        }

    private fun getAccentString(context: Context, id: Int): String {
        return ContextCompat.getString(context, id)
    }

    fun getAccentColorName(value: String, context: Context): String {
        val prefs = Prefs(context)
        if (prefs.dynamicColorEnabled) {
            return getAccentString(context, R.string.color_dynamic)
        }
        DefaultAccentColors.entries.forEach {
            if (ContextCompat.getColor(context, it.colorResId) == value.toColorInt()) {
                return getAccentString(context, it.colorNameResId)
            }
        }
        return ContextCompat.getString(context, R.string.custom_color)
    }
}

enum class DefaultAccentColors(val colorResId: Int, val colorNameResId: Int) {
    LIME(R.color.tile_lime, R.string.color_lime),
    GREEN(R.color.tile_green, R.string.color_green),
    EMERALD(R.color.tile_emerald, R.string.color_emerald),
    CYAN(R.color.tile_cyan, R.string.color_cyan),
    TEAL(R.color.tile_teal, R.string.color_teal),
    COBALT(R.color.tile_cobalt, R.string.color_cobalt),
    INDIGO(R.color.tile_indigo, R.string.color_indigo),
    VIOLET(R.color.tile_violet, R.string.color_violet),
    PINK(R.color.tile_pink, R.string.color_pink),
    MAGENTA(R.color.tile_magenta, R.string.color_magenta),
    CRIMSON(R.color.tile_crimson, R.string.color_crimson),
    RED(R.color.tile_red, R.string.color_red),
    ORANGE(R.color.tile_orange, R.string.color_orange),
    AMBER(R.color.tile_amber, R.string.color_amber),
    YELLOW(R.color.tile_yellow, R.string.color_yellow),
    BROWN(R.color.tile_brown, R.string.color_brown),
    OLIVE(R.color.tile_olive, R.string.color_olive),
    STEEL(R.color.tile_steel, R.string.color_steel),
    MAUVE(R.color.tile_mauve, R.string.color_mauve),
    TAUPE(R.color.tile_taupe, R.string.color_taupe)
}