package com.afgalindob.assistantapp.data.container

import com.afgalindob.assistantapp.data.manager.GlobalStateManager
import com.afgalindob.assistantapp.data.repository.audio.AudioRepository
import com.afgalindob.assistantapp.data.repository.note.NoteRepository
import com.afgalindob.assistantapp.data.repository.task.TaskRepository
import com.afgalindob.assistantapp.data.repository.trash.TrashRepository
import com.afgalindob.assistantapp.data.repository.settings.SettingsRepository
import com.afgalindob.assistantapp.data.repository.network.NetworkRepository

interface AppContainer {
    val globalStateManager: GlobalStateManager
    val taskRepository: TaskRepository
    val noteRepository: NoteRepository
    val trashRepository: TrashRepository
    val settingsRepository: SettingsRepository
    val audioRepository: AudioRepository
    val networkRepository: NetworkRepository
}