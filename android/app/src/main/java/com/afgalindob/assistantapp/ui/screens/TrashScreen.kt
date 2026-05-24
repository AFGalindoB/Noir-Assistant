package com.afgalindob.assistantapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.afgalindob.assistantapp.data.domain.AudioDomain
import com.afgalindob.assistantapp.data.domain.NoteDomain
import com.afgalindob.assistantapp.data.domain.TaskDomain
import com.afgalindob.assistantapp.utils.DialogType
import com.afgalindob.assistantapp.ui.components.EntitySnackbar
import com.afgalindob.assistantapp.ui.components.NoirBackground
import com.afgalindob.assistantapp.ui.components.SectionHeader
import com.afgalindob.assistantapp.ui.components.cards.NoteCard
import com.afgalindob.assistantapp.ui.components.cards.TaskCard
import com.afgalindob.assistantapp.ui.dialogs.alert.DeleteEntityDialog
import com.afgalindob.assistantapp.ui.theme.AccentSecondary
import com.afgalindob.assistantapp.ui.theme.OnAccentSecondary
import com.afgalindob.assistantapp.utils.DateUtils
import com.afgalindob.assistantapp.viewmodel.room.TrashViewModel

sealed class SelectedEntity {
    data class Task(val task: TaskDomain) : SelectedEntity()
    data class Note(val note: NoteDomain) : SelectedEntity()
    data class Audio(val audio: AudioDomain) : SelectedEntity()
}

@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onRendered: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    val tasks by viewModel.deletedTasks.collectAsState()
    val notes by viewModel.deletedNotes.collectAsState()
    val audios by viewModel.deletedAudioFiles.collectAsState()

    var expandedEntity by remember { mutableStateOf<SelectedEntity?>(null) }
    val isAnyExpanded by remember { derivedStateOf { expandedEntity != null } }

    var lastRestoredEntity by remember { mutableStateOf<SelectedEntity?>(null) }
    var deletingEntity by remember { mutableStateOf<SelectedEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        repeat(2) { withFrameNanos { } }
        onRendered()
    }

    NoirBackground(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()){
            LazyColumn(modifier = Modifier.fillMaxSize()){

                if (tasks.isEmpty() && notes.isEmpty() && audios.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.trash_placeholder),
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // --- SECCIÓN DE TAREAS ---
                if (tasks.isNotEmpty()) {
                    item { SectionHeader(R.string.tasks) } // Header
                    items(tasks, key = { "task_${it.id}" }) { task ->
                        val isExpanded = expandedEntity is SelectedEntity.Task &&
                                (expandedEntity as SelectedEntity.Task).task.id == task.id

                        TaskCard(
                            task = task,
                            expanded = isExpanded,
                            date = task.deleteAt,
                            onTrash = true,
                            anyCardExpanded = isAnyExpanded,
                            enableSwipe = false,
                            onExpand = {
                                expandedEntity = if (isExpanded) null else SelectedEntity.Task(task)
                            },
                            onEvent = { },
                            actionArea = {
                                Column (
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ){
                                    IconButton(
                                        onClick = {
                                            viewModel.restoreTask(task)
                                            expandedEntity = null
                                            lastRestoredEntity = SelectedEntity.Task(task)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(AccentSecondary)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.restore),
                                            contentDescription = "Restore Task",
                                            tint = OnAccentSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(15.dp))
                                    IconButton(
                                        onClick = {
                                            deletingEntity = SelectedEntity.Task(task)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(AccentSecondary)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.trash),
                                            contentDescription = "Delete Task",
                                            tint = OnAccentSecondary
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // --- SECCIÓN DE NOTAS ---
                if (notes.isNotEmpty()) {
                    item { SectionHeader(R.string.notes) }
                    items(notes, key = { "note_${it.id}" }) { note ->
                        val isExpanded = expandedEntity is SelectedEntity.Note &&
                                (expandedEntity as SelectedEntity.Note).note.id == note.id

                        NoteCard(
                            note = note,
                            date = note.deleteAt,
                            onTrash = true,
                            expanded = isExpanded,
                            anyCardExpanded = isAnyExpanded,
                            enableSwipe = false,
                            onExpand = {
                                expandedEntity = if (isExpanded) null else SelectedEntity.Note(note)
                            },
                            onEvent = { },
                            actionArea = {
                                Column(
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    IconButton(
                                        onClick = {
                                            viewModel.restoreNote(note)
                                            expandedEntity = null
                                            lastRestoredEntity = SelectedEntity.Note(note)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(AccentSecondary)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.restore),
                                            contentDescription = "Restore Note",
                                            tint = OnAccentSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(15.dp))
                                    IconButton(
                                        onClick = {
                                            deletingEntity = SelectedEntity.Note(note)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(AccentSecondary)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.trash),
                                            contentDescription = "Delete Note",
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                if (audios.isNotEmpty()) {
                    item { SectionHeader(R.string.audios) }
                    items(audios, key = { "audio_${it.id}" }) { audio ->
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(audio.fileName)
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(DateUtils.formatReadable(DateUtils.fromTimestamp(audio.deleteAt)))
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Boton Restaurar
                            IconButton(
                                onClick = {
                                    lastRestoredEntity = SelectedEntity.Audio(audio)
                                    viewModel.restoreAudioFile(audio)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AccentSecondary)
                                ) {
                                Icon(
                                    painter = painterResource(R.drawable.restore),
                                    contentDescription = "Restore Audio",
                                    tint = OnAccentSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(15.dp))

                            // Boton Eliminar Permanentemente
                            IconButton(
                                onClick = {
                                    deletingEntity = SelectedEntity.Audio(audio)
                                    expandedEntity = null
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AccentSecondary)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.trash),
                                    contentDescription = "Delete Audio",
                                    tint = OnAccentSecondary
                                )
                            }
                        }
                    }
                }
            }
            EntitySnackbar(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    val undoLabel = stringResource(R.string.undo)
    val taskRestoredMessage = stringResource(R.string.task) + " " + stringResource(R.string.restored)
    val noteRestoredMessage = stringResource(R.string.note) + " " + stringResource(R.string.restored)
    val audioRestoredMessage = stringResource(R.string.audio) + " " + stringResource(R.string.restored)

    // Mostrar snackbar al restaurar
    LaunchedEffect(lastRestoredEntity) {
        lastRestoredEntity?.let { entityToProcess ->
            val message = when(entityToProcess) {
                is SelectedEntity.Task -> taskRestoredMessage
                is SelectedEntity.Note -> noteRestoredMessage
                is SelectedEntity.Audio -> audioRestoredMessage
            }

            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short
            )

            if (result == SnackbarResult.ActionPerformed) {
                when(entityToProcess) {
                    is SelectedEntity.Task -> viewModel.reDeleteTask(entityToProcess.task.id, entityToProcess.task.deleteAt ?: 0L)
                    is SelectedEntity.Note -> viewModel.reDeleteNote(entityToProcess.note.id, entityToProcess.note.deleteAt ?: 0L)
                    is SelectedEntity.Audio -> viewModel.reDeleteAudio(entityToProcess.audio.id, entityToProcess.audio.deleteAt)
                }
            }

            lastRestoredEntity = null
        }
    }

    // Mostrar dialogo de eliminacion
    deletingEntity?.let { entity ->
        DeleteEntityDialog(
            title = when(entity) {
                is SelectedEntity.Task -> entity.task.title
                is SelectedEntity.Note -> entity.note.title
                is SelectedEntity.Audio -> entity.audio.fileName
            },
            type = when(entity) { // Pasamos el valor directamente
                is SelectedEntity.Task -> DialogType.TASK
                is SelectedEntity.Note -> DialogType.NOTE
                is SelectedEntity.Audio -> DialogType.AUDIO
            },
            onConfirm = {
                when(entity) {
                    is SelectedEntity.Task -> viewModel.deletePermanentTask(entity.task)
                    is SelectedEntity.Note -> viewModel.deletePermanentNote(entity.note)
                    is SelectedEntity.Audio -> viewModel.deletePermanentAudio(entity.audio)
                }
                deletingEntity = null
                expandedEntity = null
            },
            onDismiss = { deletingEntity = null }
        )
    }
}