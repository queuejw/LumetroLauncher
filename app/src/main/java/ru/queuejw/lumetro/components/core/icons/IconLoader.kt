package ru.queuejw.lumetro.components.core.icons

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.collection.LruCache
import androidx.core.graphics.drawable.toBitmap
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.adapters.viewtypes.AppViewTypes
import ru.queuejw.lumetro.components.core.cache.CacheUtils
import ru.queuejw.lumetro.components.core.cache.DiskLruCache
import ru.queuejw.lumetro.components.prefs.Prefs
import ru.queuejw.lumetro.model.App

class IconLoader(
    private val loadFromIconPack: Boolean = false,
    private val iconPackPackage: String? = null
) : IconProvider {

    var iconPackManager: IconPackManager? = null
    private var diskCache: DiskLruCache? = null
    private var iconSize: Int? = null

    private val cacheSize by lazy {
        ((Runtime.getRuntime().maxMemory() / 1024) / 8).toInt()
    }
    private val memoryCache by lazy {
        object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }

    private fun getIconSize(context: Context): Int {
        return context.resources.getDimensionPixelSize(R.dimen.icon_size)
    }

    private fun getIconPackManager(context: Context): IconPackManager? {
        return IconPackManager(context)
    }

    private fun getDiskCache(context: Context): DiskLruCache? {
        return CacheUtils.initDiskCache(context)
    }

    private fun cacheBitmap(bitmap: Bitmap, key: String) {
        memoryCache.put(key, bitmap)
        CacheUtils.saveIconToDiskCache(diskCache, key, bitmap)
    }

    override fun getIconForPackage(context: Context, mPackage: String): Bitmap? {
        memoryCache[mPackage]?.let {
            return it
        }
        if (diskCache == null) {
            diskCache = getDiskCache(context)
        }
        CacheUtils.loadIconFromDiskCache(diskCache, mPackage)?.let {
            memoryCache.put(mPackage, it)
            return it
        }
        if (iconPackManager == null && loadFromIconPack) {
            iconPackManager = getIconPackManager(context)
        }
        return runCatching {
            iconSize ?: getIconSize(context).also { iconSize = it }
            if (!loadFromIconPack) {
                context.packageManager.getApplicationIcon(mPackage).toBitmap(iconSize!!, iconSize!!)
                    .also {
                        cacheBitmap(it, mPackage)
                    }
            } else {
                iconPackManager?.getIconPackWithName(iconPackPackage!!)?.getDrawableIconForPackage(
                    mPackage,
                    context.packageManager.getApplicationIcon(mPackage)
                )?.toBitmap(iconSize!!, iconSize!!)?.also {
                    cacheBitmap(it, mPackage)
                }
            }
        }.onFailure {
            Log.e("Icon loader", it.toString())
        }.getOrNull()
    }

    fun isIconPackAvailable(context: Context, mPackage: String?): Boolean {
        if (mPackage == null) return true
        try {
            context.packageManager.getPackageInfo(mPackage, 0)
            return true
        } catch (_: PackageManager.NameNotFoundException) {
            return false
        }
    }

    fun removeIconPack(prefs: Prefs) {
        prefs.apply {
            iconPackPackage = null
        }
    }

    fun cacheAllIcons(appList: List<App>, context: Context) {
        for (item in appList) {
            if (item.viewType == AppViewTypes.TYPE_HEADER.type) continue
            item.mPackage?.let {
                getIconForPackage(context, it)
            }
        }
    }

    fun resetIconLoader(closeCache: Boolean = false) {
        iconPackManager = null
        iconSize = null
        memoryCache.evictAll()
        if (closeCache) {
            CacheUtils.closeDiskCache(diskCache)
        }
        diskCache = null
    }

    fun clearCache(context: Context) {
        iconPackManager = null
        memoryCache.evictAll()
        if (diskCache == null) {
            diskCache = getDiskCache(context)
        }
        diskCache?.delete()
    }
}