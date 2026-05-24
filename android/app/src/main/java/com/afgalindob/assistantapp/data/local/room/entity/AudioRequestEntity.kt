package com.afgalindob.assistantapp.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_requests",
    indices = [
        Index("status"),
        Index("createdAt")
    ]
)
data class AudioRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deleteAt: Long = 0L,
    val onTrash: Boolean = false
)