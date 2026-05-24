package com.afgalindob.assistantapp.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.ui.components.NoirBackground
import com.afgalindob.assistantapp.ui.theme.AccentPrimary
import com.afgalindob.assistantapp.ui.theme.OnSurfacePrimary
import com.afgalindob.assistantapp.ui.theme.OnSurfaceSecondary
import com.afgalindob.assistantapp.ui.theme.SurfaceContainer
import com.afgalindob.assistantapp.ui.theme.SurfaceContainerHigh
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afgalindob.assistantapp.data.domain.AudioDomain
import com.afgalindob.assistantapp.data.local.network.ServerStatus
import com.afgalindob.assistantapp.ui.components.EntitySnackbar
import com.afgalindob.assistantapp.ui.components.SectionHeader
import com.afgalindob.assistantapp.ui.components.cards.AudioCard
import com.afgalindob.assistantapp.ui.dialogs.alert.AdvertisementDialog
import com.afgalindob.assistantapp.viewmodel.MainViewModel
import com.afgalindob.assistantapp.viewmodel.room.AudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTaskScreen(
    viewModel: AudioViewModel,
    mainViewModel: MainViewModel
) {

    val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()
    val isTokenConfigured by viewModel.isTokenConfigured.collectAsStateWithLifecycle()
    val isUploadingAudios by viewModel.isUploadingAudiosState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        repeat(2) { withFrameNanos { } }

        mainViewModel.updateBars(
            topBar = {
                if (serverStatus == ServerStatus.ONLINE && isTokenConfigured){
                    IconButton(enabled = !isUploadingAudios, onClick = { viewModel.processPendingUploads() }) {
                        Icon(
                            painter = painterResource(R.drawable.upload),
                            contentDescription = "Upload",
                            tint = OnSurfacePrimary
                        )
                    }
                }else{
                    IconButton(onClick = { viewModel.checkCommunicationWithServer() }) {
                        Icon(
                            painter = painterResource(R.drawable.wifi_alert),
                            contentDescription = "Upload",
                            tint = OnSurfacePrimary
                        )
                    }
                }
            },
            bottomBar = {
                val isRecordingInternal by viewModel.isRecording.collectAsStateWithLifecycle()
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) { viewModel.toggleRecording() }
                }

                BottomAppBar(
                    containerColor = SurfaceContainerHigh,
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FloatingActionButton(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            containerColor = if (isRecordingInternal) AccentPrimary else SurfaceContainer
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isRecordingInternal) R.drawable.stop else R.drawable.circle_filled
                                ),
                                contentDescription = if (isRecordingInternal) "Detener" else "Grabar",
                                tint = if (isRecordingInternal) OnSurfacePrimary else AccentPrimary
                            )
                        }
                    }
                }
            }
        )

        // Una vez inyectadas las barras, abrimos la señal para desvanecer el CurtainOverlay
        mainViewModel.notifyScreenRendered()
    }

    val undoLabel = stringResource(R.string.undo)
    val messageLabel = stringResource(R.string.audio) + " " + stringResource(R.string.sent_to_trash)

    val focusManager = LocalFocusManager.current

    val audiosBySection by viewModel.audiosBySection.collectAsStateWithLifecycle()

    val currentlyPlayingName by viewModel.currentlyPlayingName.collectAsStateWithLifecycle()
    val playbackProgressMs by viewModel.playbackProgressMs.collectAsStateWithLifecycle()
    val playbackDuration by viewModel.playbackDuration.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val isRecording = viewModel.isRecording.collectAsStateWithLifecycle().value

    var showDeleteWarning by remember { mutableStateOf(false) }

    var deletingAudio by remember { mutableStateOf<AudioDomain?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) { viewModel.toggleRecording() }
    }

    NoirBackground(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        }
    ){
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                audiosBySection.forEach { (sectionResId, audioStates) ->
                    item(key = "header_$sectionResId") {
                        SectionHeader(sectionResId)
                    }

                    items(audioStates, key = { it.id }) { audio ->
                        val isThisPlaying = currentlyPlayingName == audio.fileName

                        val currentProgress =
                            remember(isThisPlaying, playbackProgressMs, playbackDuration) {
                                if (isThisPlaying && playbackDuration > 0) {
                                    playbackProgressMs.toFloat() / playbackDuration.toFloat()
                                } else 0f
                            }

                        AudioCard(
                            audio = audio,
                            isPlaying = isThisPlaying,
                            anyCardExpanded = currentlyPlayingName != null,
                            sliderProgress = currentProgress,
                            currentPosition = playbackProgressMs,
                            totalDuration = playbackDuration,
                            onTogglePlay = { viewModel.togglePlayStop(audio.fileName) },
                            onDelete = { onConfirmSwipe ->
                                deletingAudio = audio
                                onConfirmSwipe(true)
                            },
                            onSeek = { viewModel.onSliderSeek(it) },
                            onSeekRelative = { viewModel.seekRelative(it) }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRecording) "Escuchando..."
                    else "Presiona el micrófono para crear una tarea",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isRecording) OnSurfacePrimary else OnSurfaceSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        EntitySnackbar(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showDeleteWarning) {
            AdvertisementDialog(
                titleToShow = "Error la nota no se pudo eliminar",
                descriptionToShow = "La IA está procesando este audio. Espera a que termine para poder eliminarlo.",
                onConfirm = { showDeleteWarning = false }
            )
        }

        LaunchedEffect(deletingAudio) {
            val audioToHandle = deletingAudio
            if (audioToHandle != null) {
                // Ejecutamos la acción del ViewModel usando el ID numérico de dominio
                val success = viewModel.sendToTrash(audioToHandle)

                if (!success) {
                    showDeleteWarning = true
                    deletingAudio = null
                }

                val snackbarResult = snackbarHostState.showSnackbar(
                    message = messageLabel,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short
                )

                if (snackbarResult == SnackbarResult.ActionPerformed) {
                    viewModel.restoreAudio(audioToHandle.id)
                }

                deletingAudio = null
            }
        }
    }
}