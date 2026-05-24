package com.afgalindob.assistantapp.data.domain

data class NoteFormState(
    val title: String = "",
    val content: String = ""
)

data class NoteDomain(
    val id: Long,
    val title: String,
    val content: String,
    val deleteAt: Long?
)