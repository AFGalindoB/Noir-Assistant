package com.afgalindob.assistantapp.data.mapper

import com.afgalindob.assistantapp.data.local.room.entity.NoteEntity
import com.afgalindob.assistantapp.data.domain.NoteDomain

fun NoteEntity.toNoteDomain(): NoteDomain {
    return NoteDomain(
        id = this.id,
        title = this.title,
        content = this.content,
        deleteAt = if (this.deleteAt == 0L) null else this.deleteAt
    )
}

fun List<NoteEntity>.toNoteDomainList(): List<NoteDomain> {
    return this.map { it.toNoteDomain() }
}