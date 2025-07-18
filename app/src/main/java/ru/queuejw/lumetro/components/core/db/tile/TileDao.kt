package ru.queuejw.lumetro.components.core.db.tile

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.queuejw.lumetro.model.TileEntity

@Dao
interface TileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTile(tile: TileEntity)

    @Delete
    suspend fun deleteTile(tile: TileEntity)

    @Update
    suspend fun updateTile(tile: TileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateAllTiles(tiles: List<TileEntity>)

    @Query("SELECT * FROM tiles ORDER BY tilePosition ASC")
    fun getTilesData(): MutableList<TileEntity>

    @Query("SELECT * FROM tiles WHERE tileType != 1 ORDER BY tilePosition ASC")
    fun getUserTilesData(): MutableList<TileEntity>

    @Query("DELETE FROM tiles")
    fun deleteAllTiles()
}