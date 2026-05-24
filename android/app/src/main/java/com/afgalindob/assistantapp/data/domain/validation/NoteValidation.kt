package com.afgalindob.assistantapp.data.domain.validation

import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.data.domain.NoteFormState

object NoteValidationSchema {

    val fields = listOf(
        FormField(
            key = "title",
            required = true,
            maxLengthChar = 70
        ),

        FormField(key = "content")
    )
}

fun validateNoteForm(form: NoteFormState): Map<String, ValidationError> {

    val errors = mutableMapOf<String, ValidationError>()

    NoteValidationSchema.fields.forEach { field ->
        val value = when (field.key) {
            "title" -> form.title
            "content" -> form.content
            else -> null
        }

        if (field.required && !Validators.required(value)) {
            errors[field.key] = ValidationError(R.string.field_required)
        }

        if (field.maxLengthChar != null && value is String &&
            !Validators.maxLength(value, field.maxLengthChar)) {
            errors[field.key] = ValidationError(
                resId = R.string.max_length,
                arg = field.maxLengthChar
            )
        }

    }

    return errors
}