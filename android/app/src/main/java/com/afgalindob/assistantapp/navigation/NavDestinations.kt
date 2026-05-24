package com.afgalindob.assistantapp.navigation

import kotlinx.serialization.Serializable

@Serializable sealed interface RootGraph
@Serializable data object HomeGraph : RootGraph
@Serializable data object TrashGraph : RootGraph

@Serializable data object AccountGraph : RootGraph
@Serializable data object VoiceTaskGraph : RootGraph

// Definimos las pantallas individuales
@Serializable data object TaskList
@Serializable data object NoteList
@Serializable data object Account
@Serializable data object TrashScreen
@Serializable data object VoiceTaskScreen
@Serializable data object TransitionScreen