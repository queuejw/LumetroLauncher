package ru.queuejw.mpl.helpers.disklru

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest

class CacheUtils {
    companion object {
        private val md by lazy { MessageDigest.getInstance("MD5") }

        private fun getDiskCacheDir(context: Context): File {
            val cachePath = context.cacheDir.absolutePath
            return File(cachePath + File.separator + "icons")
        }

        fun initDiskCache(context: Context): DiskLruCache? {
            try {
                val cacheDir = getDiskCacheDir(context)
                val cacheSize = 32 * 1024 * 1024 * 2
                return DiskLruCache.open(cacheDir, 1, 1, cacheSize.toLong())
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }

        fun saveIconToDiskCache(diskLruCache: DiskLruCache?, key: String, bitmap: Bitmap?) {
            if (diskLruCache != null && bitmap != null) {
                val editor = diskLruCache.edit(key.toMd5())
                if (editor != null) {
                    try {
                        val outputStream: OutputStream = editor.newOutputStream(0)
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        editor.commit()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        editor.abort()
                    }
                }
            } else {
                Log.d("saveIconToDiskCache", "diskLruCache or bitmap is null")
            }
        }

        private fun String.toMd5(): String {
            return md.digest(toByteArray())
                .joinToString("") { "%02x".format(it) }
        }

        fun loadIconFromDiskCache(diskLruCache: DiskLruCache, key: String): Bitmap? {
            val snapshot = diskLruCache.get(key.toMd5()) ?: return null
            return BitmapFactory.decodeStream(snapshot.getInputStream(0))
        }

        fun closeDiskCache(diskLruCache: DiskLruCache): Boolean {
            return try {
                diskLruCache.close()
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }
    }
}