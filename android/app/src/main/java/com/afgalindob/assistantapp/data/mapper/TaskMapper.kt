package com.afgalindob.assistantapp.data.mapper

import com.afgalindob.assistantapp.data.local.room.entity.TaskEntity
import com.afgalindob.assistantapp.data.domain.TaskDomain

fun TaskEntity.toTaskDomain(): TaskDomain {
    return TaskDomain(
        id = this.id,
        title = this.title,
        content = this.content,
        date = if (this.date == 0L) null else this.date,
        completed = this.completed,
        deleteAt = if (this.deleteAt == 0L) null else this.deleteAt
    )
}

fun List<TaskEntity>.toTaskDomainList(): List<TaskDomain> {
    return this.map { it.toTaskDomain() }
}