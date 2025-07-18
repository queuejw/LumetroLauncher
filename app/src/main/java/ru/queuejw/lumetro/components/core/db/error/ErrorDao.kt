package ru.queuejw.lumetro.components.core.db.error

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.queuejw.lumetro.model.ErrorEntity

@Dao
interface ErrorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ErrorEntity)

    @Delete
    suspend fun deleteItem(item: ErrorEntity)

    @Query("SELECT * FROM errors")
    fun getErrorData(): MutableList<ErrorEntity>

    @Query("DELETE FROM errors")
    fun deleteAllErrorData()
}