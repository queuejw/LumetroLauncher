package ru.queuejw.lumetro.components.core

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.queuejw.lumetro.components.adapters.viewtypes.TileViewTypes
import ru.queuejw.lumetro.components.core.Lumetro.Companion.viewPagerUserInputEnabled
import ru.queuejw.lumetro.components.core.db.tile.TileDao
import ru.queuejw.lumetro.components.core.db.tile.TileDatabase
import ru.queuejw.lumetro.model.TileEntity
import kotlin.random.Random

class TileManager {

    fun getPlaceholderItem(list: List<TileEntity>, position: Int = list.size): TileEntity {
        return TileEntity(
            tilePosition = position,
            tileColor = null,
            tileType = TileViewTypes.TYPE_PLACEHOLDER.type,
            tileSize = 0,
            tileLabel = null,
            tilePackage = null
        )
    }

    suspend fun generatePlaceholders(size: Int, context: Context, force: Boolean): Boolean {
        val db: TileDatabase? = TileDatabase.getTileData(context)
        val dao: TileDao? = db!!.getTilesDao()
        val list = dao!!.getTilesData()
        if (list.isNotEmpty() && !force) return false
        for (i in 0..size) {
            withContext(Dispatchers.IO) {
                val item = getPlaceholderItem(list)
                list.add(item)
                dao.insertTile(item)
            }
        }
        return true
    }

    suspend fun checkAllTiles(context: Context): Boolean {
        val db: TileDatabase? = TileDatabase.getTileData(context)
        val dao: TileDao? = db!!.getTilesDao()
        val list = dao!!.getTilesData()
        val pm = context.packageManager
        if (dao.getUserTilesData().isEmpty()) return false
        var isDataOutdated = false
        list.forEachIndexed { pos, it ->
            withContext(Dispatchers.IO) {
                if (it.tileType != -1) {
                    try {
                        pm.getPackageInfo(it.tilePackage!!, 0)
                    } catch (_: PackageManager.NameNotFoundException) {
                        isDataOutdated = true
                        list[pos] = getPlaceholderItem(list)
                    }
                }
            }
        }
        if (isDataOutdated) {
            db.getTilesDao().updateAllTiles(list)
        }
        return true
    }

    suspend fun pinNewTile(
        context: Context,
        tileList: List<TileEntity>,
        mPackage: String,
        dao: TileDao
    ): MutableList<TileEntity>? {
        var isAppPinned = false
        var position: Int? = null
        for (i in 0..tileList.size) {
            val item = tileList[i]
            if (item.tilePackage == mPackage) {
                isAppPinned = true
                break
            }
            if (item.tileType == -1) {
                position = i
                break
            }
        }
        if (!isAppPinned) {
            if (position == null) position = Random.nextInt(0, tileList.size)
            val label = context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(
                    mPackage,
                    0
                )
            ).toString()
            val entity: TileEntity? = tileList[position].apply {
                tilePosition = position
                tileType = 0
                tileLabel = label
                tilePackage = mPackage
                tileSize = Random.nextInt(0, 3)
            }
            dao.updateTile(entity!!)
            val list = dao.getUserTilesData()
            withContext(Dispatchers.Main) {
                viewPagerUserInputEnabled = true
            }
            return list
        } else {
            withContext(Dispatchers.Main) {
                viewPagerUserInputEnabled = true
            }
            return null
        }
    }
}