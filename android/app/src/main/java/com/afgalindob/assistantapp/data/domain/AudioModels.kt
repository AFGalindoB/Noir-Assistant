package com.afgalindob.assistantapp.data.domain

enum class AudioStatus(val value: String) {
    WAITING("WAITING"),
    PROCESSING("PROCESSING"),
    SENT("SENT"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    companion object {
        fun fromString(value: String): AudioStatus {
            return entries.find { it.value == value } ?: FAILED
        }
    }
}

data class AudioDomain(
    val id: Long,
    val fileName: String,
    val status: AudioStatus,
    val deleteAt: Long
)