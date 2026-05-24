package com.afgalindob.assistantapp.utils.sections

import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.data.domain.AudioDomain
import com.afgalindob.assistantapp.data.domain.AudioStatus

object AudioSections {
    val PROCESSING = R.string.ai_status_processing
    val WAITING = R.string.ai_status_waiting
    val SENT = R.string.ai_status_sent
    val COMPLETED = R.string.ai_status_completed
    val FAILED = R.string.ai_status_failed
}

fun AudioDomain.getAudioSection(): Int = when (this.status) {
    AudioStatus.PROCESSING -> AudioSections.PROCESSING
    AudioStatus.WAITING -> AudioSections.WAITING
    AudioStatus.SENT -> AudioSections.SENT
    AudioStatus.COMPLETED -> AudioSections.COMPLETED
    AudioStatus.FAILED -> AudioSections.FAILED
}

fun audioSectionOrder(section: Int): Int = when(section) {
    AudioSections.PROCESSING -> 0
    AudioSections.WAITING -> 1
    AudioSections.SENT -> 2
    AudioSections.FAILED -> 3
    AudioSections.COMPLETED -> 4
    else -> 5
}