package com.afgalindob.assistantapp.data.domain.validation

data class FormField(
    val key: String,
    val required: Boolean = false,
    val maxLengthChar: Int? = null
)

data class ValidationError(
    val resId: Int,
    val arg: Any? = null
)