package ru.queuejw.lumetro.components.core.icons

import android.content.Context
import android.graphics.Bitmap

interface IconProvider {

    fun getIconForPackage(context: Context, mPackage: String): Bitmap?
}