package com.afgalindob.assistantapp.viewmodel

import com.afgalindob.assistantapp.viewmodel.room.AudioViewModel
import com.afgalindob.assistantapp.viewmodel.room.NoteViewModel
import com.afgalindob.assistantapp.viewmodel.room.TaskViewModel
import com.afgalindob.assistantapp.viewmodel.room.TrashViewModel

data class AssistantViewModels(
    val task: TaskViewModel,
    val note: NoteViewModel,
    val trash: TrashViewModel,
    val settings: SettingsViewModel,
    val audio: AudioViewModel
)