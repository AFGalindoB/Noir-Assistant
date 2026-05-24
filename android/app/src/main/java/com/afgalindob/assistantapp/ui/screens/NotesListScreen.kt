package com.afgalindob.assistantapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.data.domain.NoteDomain
import com.afgalindob.assistantapp.data.domain.validation.ValidationError
import com.afgalindob.assistantapp.utils.FormMode
import com.afgalindob.assistantapp.ui.components.EntitySnackbar
import com.afgalindob.assistantapp.ui.components.NoirBackground
import com.afgalindob.assistantapp.ui.components.cards.NoteCard
import com.afgalindob.assistantapp.ui.components.cards.NoteEvent
import com.afgalindob.assistantapp.ui.dialogs.alert.NoteUpserDialog
import com.afgalindob.assistantapp.ui.theme.AccentPrimary
import com.afgalindob.assistantapp.ui.theme.AccentSecondary
import com.afgalindob.assistantapp.ui.theme.OnAccentPrimary
import com.afgalindob.assistantapp.ui.theme.OnAccentSecondary
import com.afgalindob.assistantapp.viewmodel.room.NoteViewModel

@Composable
fun NotesListScreen(
    viewModel: NoteViewModel,
    onRendered: () -> Unit
){
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        repeat(2) { withFrameNanos { } }
        onRendered()
    }
    NoirBackground(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        }
    ) {
        val notes by viewModel.notesDomain.collectAsState()

        var expandedNoteId by remember { mutableStateOf<Long?>(null) }

        val snackbarHostState = remember { SnackbarHostState() }
        var deletingNote by remember { mutableStateOf<NoteDomain?>(null) }

        var dialogMode by remember { mutableStateOf<String?>(null) }
        var editingNote by remember { mutableStateOf<NoteDomain?>(null) }
        val noteErrors = remember { mutableStateMapOf<String, ValidationError>() }

        val onExpandAction = remember {
            { id: Long -> expandedNoteId = if (expandedNoteId == id) null else id }
        }

        val isAnyExpanded by remember { derivedStateOf { expandedNoteId != null } }

        val onEditAction = remember {
            { note: NoteDomain ->
                editingNote = note
                dialogMode = FormMode.Edit.name
                noteErrors.clear()
            }
        }

        Box(modifier = Modifier.fillMaxSize()){
            LazyColumn {
                if (notes.isEmpty()) {
                    item {
                        Text(
                            stringResource(
                                R.string.list_placeholder,
                                stringResource(R.string.notes).lowercase()
                            ),
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                items(
                    items = notes,
                    key = { it.id }
                ) { note ->
                    val isExpanded = expandedNoteId == note.id
                    NoteCard(
                        note = note,
                        expanded = isExpanded,
                        anyCardExpanded = isAnyExpanded,
                        onExpand = { onExpandAction(note.id) },
                        onEvent = { event ->
                            if (event is NoteEvent.Edit) {
                                onEditAction(note)
                            }
                            if (event is NoteEvent.Delete) {
                                deletingNote = note
                            }
                        },
                        actionArea = {
                            IconButton(
                                onClick = { onEditAction(note) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AccentSecondary)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.edit),
                                    contentDescription = "Edit Note",
                                    tint = OnAccentSecondary
                                )
                            }
                        }
                    )
                }
                if (!notes.isEmpty()) {
                    item { Spacer(Modifier.height(50.dp)) }
                    item {
                        Text(
                            stringResource(R.string.end_of_list) + " " + stringResource(R.string.notes),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            EntitySnackbar(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            FloatingActionButton(
                onClick = {
                    editingNote = null
                    dialogMode = FormMode.New.name
                    noteErrors.clear()
                },
                containerColor = AccentPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = "Add Note",
                    tint = OnAccentPrimary
                )
            }
        }
        dialogMode?.let{
            NoteUpserDialog(
                note = if (dialogMode == FormMode.Edit.name) editingNote else null,
                errors = noteErrors,
                onConfirm = {
                    val validationErrors = viewModel.validate(it)
                    noteErrors.clear()
                    noteErrors.putAll(validationErrors)

                    if (validationErrors.isEmpty()) {
                        if (dialogMode == FormMode.New.name) {
                            viewModel.createNote(it)
                        } else if (dialogMode == FormMode.Edit.name && editingNote != null) {
                            viewModel.updateNote(editingNote!!, it)
                        }
                        dialogMode = null
                        editingNote = null
                    }
                },
                onDismiss = {
                    dialogMode = null
                    editingNote = null
                }
            )
        }
        val undoLabel = stringResource(R.string.undo)
        val messageLabel = stringResource(R.string.note) + " " + stringResource(R.string.sent_to_trash).lowercase()
        LaunchedEffect(deletingNote) {
            val noteToHandle = deletingNote // Copia local para evitar problemas de referencia
            if (noteToHandle != null) {
                viewModel.softDeleteNote(noteToHandle)

                val result = snackbarHostState.showSnackbar(
                    message = messageLabel,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short
                )

                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restoreNote(noteToHandle)
                }

                deletingNote = null
            }
        }
    }
}