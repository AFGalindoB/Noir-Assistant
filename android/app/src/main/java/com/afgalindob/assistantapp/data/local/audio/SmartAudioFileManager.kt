package com.afgalindob.assistantapp.data.local.audio

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.FileObserver
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.afgalindob.assistantapp.utils.DateUtils

const val TAG = "SmartAudioFileManager"

class SmartAudioFileManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val audioDirectory: File by lazy {
        File(context.filesDir, "smart_audio").apply {
            if (!exists()) {
                mkdir()
            }
        }
    }

    // ─── FLUJOS DE ESTADO DE ARCHIVOS ───
    private val _availableAudioNames = MutableStateFlow<List<String>>(emptyList())
    val availableAudioNames: StateFlow<List<String>> = _availableAudioNames.asStateFlow()
    private var directoryObserver: FileObserver? = null

    // ─── CONTROLADOR NATIVO DE REPRODUCCIÓN (EXOPLAYER) ───
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private val _playbackProgressMs = MutableStateFlow(0L)
    val playbackProgressMs: StateFlow<Long> = _playbackProgressMs.asStateFlow()
    private val _playbackDuration = MutableStateFlow(0L)
    val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()
    private val _currentlyPlayingName = MutableStateFlow<String?>(null)
    val currentlyPlayingName: StateFlow<String?> = _currentlyPlayingName.asStateFlow()

    // ─── CONTROLADOR NATIVO DE GRABACIÓN (MEDIA RECORDER) ───
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingName: String? = null
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    init {
        updateAudioNamesList()
        startObservingDirectory()
        initializeExoPlayerListener()
    }


    // ─── VERIFICACIÓN PÚBLICA DE DISCO ───
    fun fileExists(fileName: String): Boolean {
        return resolveFileByName(fileName).exists()
    }

    // ─── LÓGICA DE MONITOREO DE DISCO ───
    private fun startObservingDirectory() {
        val mask = FileObserver.CREATE or FileObserver.DELETE or FileObserver.MOVED_TO or FileObserver.MOVED_FROM

        directoryObserver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(audioDirectory, mask) {
                override fun onEvent(event: Int, path: String?) {
                    handleDirectoryEvent(path)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            object : FileObserver(audioDirectory.absolutePath, mask) {
                override fun onEvent(event: Int, path: String?) {
                    handleDirectoryEvent(path)
                }
            }
        }
        directoryObserver?.startWatching()
    }

    private fun handleDirectoryEvent(path: String?) {
        if (path != null && path.endsWith(".m4a")) {
            updateAudioNamesList()
        }
    }

    private fun updateAudioNamesList() {
        val files = audioDirectory.listFiles { file ->
            file.isFile && file.extension == "m4a" && file.name.startsWith("audio_")
        } ?: emptyArray()

        val names = files.map { it.name }.sortedByDescending { it }
        _availableAudioNames.update { names }
    }

    fun resolveFileByName(fileName: String): File {
        return File(audioDirectory, fileName)
    }

    // ─── ENCAPSULAMIENTO DE REPRODUCCIÓN (EXOPLAYER) ───

    private fun initializeExoPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _playbackDuration.value = exoPlayer.duration.coerceAtLeast(0L)
                }
                if (state == Player.STATE_ENDED) {
                    resetPlaybackState()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val corruptFileName = _currentlyPlayingName.value
                Log.e(TAG, "Error de hardware en ExoPlayer. Código: ${error.errorCodeName} ($corruptFileName)", error)

                exoPlayer.stop()
                resetPlaybackState()

                if (corruptFileName != null) {
                    scope.launch(Dispatchers.IO) {
                        val corruptFile = resolveFileByName(corruptFileName)
                        if (corruptFile.exists()) {
                            val wasDeleted = corruptFile.delete()
                            Log.e(TAG, "El archivo corrupto se eliminó del disco de manera preventiva: $wasDeleted")
                        }
                    }
                }
            }
        })

        scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _playbackProgressMs.value = exoPlayer.currentPosition
                }
                delay(100)
            }
        }
    }

    fun togglePlayStop(fileName: String) {
        if (_currentlyPlayingName.value == fileName) {
            exoPlayer.stop()
            resetPlaybackState()
        } else {
            val file = resolveFileByName(fileName)

            if (!file.exists()) {
                Log.e(TAG, "Inconsistencia: Se intentó reproducir $fileName pero no existe en el storage.")
                updateAudioNamesList()
                return
            }

            _currentlyPlayingName.value = fileName

            exoPlayer.apply {
                stop()
                clearMediaItems()
                setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                prepare()
                play()
            }
        }
    }

    fun seekTo(position: Long) {
        val currentFile = _currentlyPlayingName.value
        if (currentFile == null || !fileExists(currentFile)) {
            resetPlaybackState()
            return
        }
        exoPlayer.seekTo(position)
        _playbackProgressMs.value = position
    }

    fun seekRelative(milliseconds: Long) {
        val currentFile = _currentlyPlayingName.value
        if (currentFile == null || !fileExists(currentFile)) {
            resetPlaybackState()
            return
        }
        val newPos = (exoPlayer.currentPosition + milliseconds).coerceIn(0, _playbackDuration.value)
        seekTo(newPos)
    }

    private fun resetPlaybackState() {
        _currentlyPlayingName.value = null
        _playbackProgressMs.value = 0L
    }

    // ─── ENCAPSULAMIENTO DE GRABACIÓN (MEDIA RECORDER) ───
    fun startRecording() {
        if (_isRecording.value) return

        // Si hay una pista reproduciéndose al presionar grabar, se detiene automáticamente para evitar vicios en el audio
        _currentlyPlayingName.value?.let { togglePlayStop(it) }

        val timestamp = DateUtils.now()
        val tempName = "tmp_$timestamp.m4a"
        val tempFile = resolveFileByName(tempName)

        currentRecordingName = tempName

        @Suppress("DEPRECATION")
        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            setAudioChannels(1)
            setAudioSamplingRate(48000)
            setAudioEncodingBitRate(48000)
            setOutputFile(tempFile.absolutePath)

            try {
                prepare()
                start()
                _isRecording.value = true
                Log.d(TAG, "Grabación iniciada exitosamente: $tempName")
            } catch (e: Exception) {
                Log.e(TAG, "Error crítico al inicializar el MediaRecorder", e)
                tempFile.delete()
                resetRecorderState()
            }
        }
    }

    suspend fun stopRecording(): String? = withContext(Dispatchers.IO) {
        val tempName = currentRecordingName ?: return@withContext null
        val tempFile = resolveFileByName(tempName)

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al detener el MediaRecorder. Limpiando residuos.", e)
            if (tempFile.exists()) tempFile.delete()
            resetRecorderState()
            return@withContext null
        }

        if (tempFile.exists()) {
            val finalName = tempName.replace("tmp_", "audio_")
            val finalDestination = resolveFileByName(finalName)

            if (tempFile.renameTo(finalDestination)) {
                Log.d(TAG, "Grabación consolidada en disco como: $finalName")
                resetRecorderState()
                return@withContext finalName
            }
        }

        resetRecorderState()
        return@withContext null
    }

    private fun resetRecorderState() {
        mediaRecorder = null
        currentRecordingName = null
        _isRecording.value = false
    }

    // ─── SISTEMA DE ARCHIVOS EXTERNO (A NIVEL DE STRING) ───

    suspend fun deleteAudioFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        // Si el archivo que se va a borrar está sonando, lo paramos antes de tumbar el binario físico
        if (_currentlyPlayingName.value == fileName) {
            withContext(Dispatchers.Main) {
                exoPlayer.stop()
                resetPlaybackState()
            }
        }

        val file = resolveFileByName(fileName)
        if (file.exists()) {
            val deleted = file.delete()
            Log.d(TAG, "Archivo físico purgado del almacenamiento: $fileName")
            deleted
        } else {
            false
        }
    }

    fun shutdownManager() {
        directoryObserver?.stopWatching()
        exoPlayer.release()
        resetRecorderState()
    }
}