package ru.queuejw.lumetro.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "errors")
data class ErrorEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var details: String
)