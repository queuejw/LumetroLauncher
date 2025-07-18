package ru.queuejw.lumetro.components.core.db.error

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.queuejw.lumetro.model.ErrorEntity

@Database(entities = [ErrorEntity::class], version = 1)
abstract class ErrorDatabase : RoomDatabase() {

    abstract fun getErrorDao(): ErrorDao

    companion object {
        private const val DB_NAME: String = "error_database"

        fun getErrorData(context: Context): ErrorDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ErrorDatabase::class.java,
                DB_NAME
            ).build()
        }
    }
}