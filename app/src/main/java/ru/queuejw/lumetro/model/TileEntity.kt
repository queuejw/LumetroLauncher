package ru.queuejw.lumetro.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tile object
 * @param tilePosition Tile position in the list
 * @param id Unique value for tile
 *
 * @param tileColor Tile color
 * @param tileType The type of tile, which will change its appearance
 * @param tileSize Tile size
 * @param tileLabel Tile name (most often the name of the application, but it can be changed)
 * @param tilePackage Application package
 */
@Entity(tableName = "tiles")
data class TileEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var tilePosition: Int,
    var tileColor: String?,
    var tileType: Int, // -1 - placeholder, 0 - default
    var tileSize: Int, //2 - big, 1 - medium, 0 - small
    var tileLabel: String?,
    var tilePackage: String?
)