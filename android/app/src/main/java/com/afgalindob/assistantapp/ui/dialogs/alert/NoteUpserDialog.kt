package com.afgalindob.assistantapp.ui.dialogs.alert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.data.domain.NoteDomain
import com.afgalindob.assistantapp.data.domain.NoteFormState
import com.afgalindob.assistantapp.data.domain.validation.ValidationError
import com.afgalindob.assistantapp.ui.animations.AnimatedPlaceholder
import com.afgalindob.assistantapp.ui.theme.AccentSecondary
import com.afgalindob.assistantapp.ui.theme.ErrorColor
import com.afgalindob.assistantapp.ui.theme.OnAccentSecondary
import com.afgalindob.assistantapp.ui.theme.OnSurfacePrimary
import com.afgalindob.assistantapp.ui.theme.SurfaceContainer
import com.afgalindob.assistantapp.ui.theme.SurfaceVariant

@Composable
fun NoteUpserDialog(
    note: NoteDomain? = null,
    errors: Map<String, ValidationError> = emptyMap(),
    onConfirm: (NoteFormState) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }

    var isTitleFocused by remember { mutableStateOf(false) }

    val transparentFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    AlertDialog(
        onDismissRequest = onDismiss,

        containerColor = SurfaceContainer,

        title = {
            Text(
                text =
                    stringResource(
                        if (note != null)
                            R.string.edit
                        else
                            R.string.label_new
                    )+ " " + stringResource(R.string.note),
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
            )
        },

        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // --- SECCIÓN TÍTULO ---
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {

                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = transparentFieldColors,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                isTitleFocused = it.isFocused
                            }
                    )

                    // Animaciones extraídas
                    AnimatedPlaceholder(
                        text = stringResource(R.string.title),
                        isVisible = title.isEmpty() && !isTitleFocused,
                        isLabel = false
                    )

                    Box(Modifier.align(Alignment.TopStart)) {
                        AnimatedPlaceholder(
                            text = stringResource(R.string.title),
                            isVisible = isTitleFocused || title.isNotEmpty(),
                            isLabel = true,
                            isFocused = isTitleFocused
                        )
                    }
                }

                // Error título
                if (errors.containsKey("title")) {
                    val error = errors["title"]
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            error == null -> ""
                            error.arg != null -> stringResource(error.resId, error.arg)
                            else -> stringResource(error.resId)
                        },
                        color = ErrorColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- SECCIÓN CONTENIDO ---
                HorizontalDivider(
                    color = SurfaceVariant,
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = content,
                    onValueChange = { content = it },
                    colors = transparentFieldColors,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                )

                HorizontalDivider(
                    color = SurfaceVariant,
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        },

        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Botón cancelar
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceVariant.copy(alpha = 0.3f),
                        contentColor = OnSurfacePrimary
                    ),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                // Boton Confirm
                Button(
                    onClick = {
                        onConfirm(
                            NoteFormState(
                                title = title,
                                content = content
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentSecondary,
                        contentColor = OnAccentSecondary
                    ),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (note != null)
                                R.string.edit
                            else
                                R.string.add
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },

        dismissButton = {}
    )
}