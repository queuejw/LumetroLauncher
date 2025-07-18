package ru.queuejw.lumetro.components.core.db.tile

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.queuejw.lumetro.model.TileEntity

@Database(entities = [TileEntity::class], version = 1)
abstract class TileDatabase : RoomDatabase() {

    abstract fun getTilesDao(): TileDao

    companion object {
        private const val DB_NAME: String = "userTiles"

        fun getTileData(context: Context): TileDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TileDatabase::class.java,
                DB_NAME
            ).build()
        }
    }
}