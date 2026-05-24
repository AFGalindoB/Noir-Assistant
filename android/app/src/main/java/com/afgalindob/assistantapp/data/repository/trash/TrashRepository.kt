package com.afgalindob.assistantapp.data.repository.trash

import com.afgalindob.assistantapp.data.domain.AudioDomain
import com.afgalindob.assistantapp.data.domain.NoteDomain
import com.afgalindob.assistantapp.data.domain.TaskDomain
import kotlinx.coroutines.flow.Flow

interface TrashRepository {
    fun getDeletedTasks(): Flow<List<TaskDomain>>
    fun getDeletedNotes(): Flow<List<NoteDomain>>
    fun getDeletedAudioFiles(): Flow<List<AudioDomain>>

    suspend fun restoreTask(id: Long)
    suspend fun restoreNote(id: Long)
    suspend fun restoreAudioFile(id: Long)

    suspend fun reDeleteTask(id: Long, days: Long)
    suspend fun reDeleteNote(id: Long, days: Long)
    suspend fun reDeleteAudio(id: Long, days: Long)

    suspend fun permanentlyDeleteTask(id: Long)
    suspend fun permanentlyDeleteNote(id: Long)
    suspend fun permanentlyDeleteAudio(id: Long): Boolean
}