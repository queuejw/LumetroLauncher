package ru.queuejw.mpl.content.data.tile

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tile object
 * @param tilePosition Tile position in the list
 * @param id Unique value for tile
 * @param tileColor Tile color
 * @param tileType The type of tile, which will change its appearance
 * @param isSelected is unused and will be deleted
 * @param tileSize Tile size
 * @param tileLabel Tile name (most often the name of the application, but it can be changed)
 * @param tilePackage Application package
 */
@Entity(tableName = "tiles")
data class Tile(
    @PrimaryKey
    var tilePosition: Int = 0,
    var id: Long? = null,
    @ColumnInfo
    var tileColor: Int = -1, //-1 - theme color, ... see Prefs.kt
    var tileType: Int = -1, // -1 - placeholder, 0 - default, 1 - weather ...
    var isSelected: Boolean = false,
    var tileSize: String = "", //big, medium, small
    var tileLabel: String = "",
    var tilePackage: String = ""
)