package com.afgalindob.assistantapp.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index("date"),
        Index("completed")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long  = 0,
    val title: String,
    val content: String,
    val date: Long,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deleteAt: Long
)