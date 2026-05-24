package com.afgalindob.assistantapp.data.repository.trash

import com.afgalindob.assistantapp.data.repository.note.NoteRepository
import com.afgalindob.assistantapp.data.repository.task.TaskRepository
import kotlinx.coroutines.flow.Flow
import com.afgalindob.assistantapp.data.domain.AudioDomain
import com.afgalindob.assistantapp.data.domain.NoteDomain
import com.afgalindob.assistantapp.data.domain.TaskDomain
import com.afgalindob.assistantapp.data.repository.audio.AudioRepository

class OfflineTrashRepository(
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val audioRepository: AudioRepository
) : TrashRepository {

    override fun getDeletedTasks(): Flow<List<TaskDomain>> =
        taskRepository.getDeletedTasks()

    override fun getDeletedNotes(): Flow<List<NoteDomain>> =
        noteRepository.getDeletedNotes()

    override fun getDeletedAudioFiles(): Flow<List<AudioDomain>> =
        audioRepository.getAllTrashedRequestsStream()

    override suspend fun restoreTask(id: Long) =
        taskRepository.restoreTask(id)

    override suspend fun restoreNote(id: Long) =
        noteRepository.restoreNote(id)

    override suspend fun restoreAudioFile(id: Long) =
        audioRepository.restoreRequest(id)

    override suspend fun reDeleteTask(id: Long, days: Long) =
        taskRepository.restoreSetOnDeleteTask(id, days)

    override suspend fun reDeleteNote(id: Long, days: Long) =
        noteRepository.restoreSetOnDeleteNote(id, days)

    override suspend fun reDeleteAudio(id: Long, days: Long) =
        audioRepository.restoreSetOnDeleteAudio(id, days)

    override suspend fun permanentlyDeleteTask(id: Long) =
        taskRepository.deleteTaskById(id)

    override suspend fun permanentlyDeleteNote(id: Long) =
        noteRepository.deleteNoteById(id)

    override suspend fun permanentlyDeleteAudio(id: Long): Boolean =
        audioRepository.deletePermanently(id)
}