package com.afgalindob.assistantapp.data.mapper

import com.afgalindob.assistantapp.data.local.room.entity.AudioRequestEntity
import com.afgalindob.assistantapp.data.domain.AudioDomain
import com.afgalindob.assistantapp.data.domain.AudioStatus

fun AudioRequestEntity.toAudioDomain(): AudioDomain {
    return AudioDomain(
        id = this.id,
        fileName = this.fileName,
        status = AudioStatus.fromString(this.status),
        deleteAt = this.deleteAt
    )
}

fun List<AudioRequestEntity>.toAudioDomainList(): List<AudioDomain> {
    return this.map { it.toAudioDomain() }
}