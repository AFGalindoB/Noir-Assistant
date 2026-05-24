package com.afgalindob.assistantapp.data.domain.validation

object Validators {
    fun required(value: Any?): Boolean {
        return when (value) {
            is String -> value.isNotBlank()
            else -> value != null
        }
    }

    fun maxLength(value: String?, max: Int): Boolean {
        return (value?.length ?: 0) <= max
    }
}