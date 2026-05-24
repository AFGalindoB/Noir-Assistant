package com.afgalindob.assistantapp.data.repository.audio

import com.afgalindob.assistantapp.data.local.audio.SmartAudioFileManager
import com.afgalindob.assistantapp.data.local.room.dao.AudioRequestDao
import android.util.Log
import com.afgalindob.assistantapp.data.domain.AudioStatus
import com.afgalindob.assistantapp.data.local.room.entity.AudioRequestEntity
import com.afgalindob.assistantapp.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import com.afgalindob.assistantapp.data.domain.AudioDomain
import com.afgalindob.assistantapp.data.local.network.NetworkResult
import com.afgalindob.assistantapp.data.mapper.toAudioDomain
import com.afgalindob.assistantapp.data.mapper.toAudioDomainList
import com.afgalindob.assistantapp.data.repository.network.NetworkRepository
import kotlinx.coroutines.flow.map
import java.io.File

const val tag = "OfflineAudioRepository"
class OfflineAudioRepository(
    private val fileManager: SmartAudioFileManager,
    private val audioRequestDao: AudioRequestDao,
    private val networkRepository: NetworkRepository,
    private val externalScope: CoroutineScope
) : AudioRepository {

    override val playbackProgress = fileManager.playbackProgressMs
    override val playbackDuration = fileManager.playbackDuration
    override val currentlyPlayingName = fileManager.currentlyPlayingName

    override val isRecording = fileManager.isRecording

    override fun togglePlayStop(fileName: String) = fileManager.togglePlayStop(fileName)
    override fun seekTo(position: Long) = fileManager.seekTo(position)
    override fun seekRelative(milliseconds: Long) = fileManager.seekRelative(milliseconds)
    override fun release() = fileManager.shutdownManager()

    init {
        fileManager.availableAudioNames
            .onEach { physicalFiles ->
                reconcileDatabaseWithDisk(physicalFiles)
            }
            .launchIn(externalScope)
    }

    private suspend fun reconcileDatabaseWithDisk(physicalFiles: List<String>) = withContext(Dispatchers.IO) {
        val now = DateUtils.now()

        val activeInDb = audioRequestDao.getAllActiveRequestsStream().firstOrNull() ?: emptyList()
        val trashedInDb = audioRequestDao.getAllTrashedRequestsStream().firstOrNull() ?: emptyList()
        val allDbRecords = activeInDb + trashedInDb
        val dbFileNames = allDbRecords.map { it.fileName }.toSet()
        val physicalFilesSet = physicalFiles.toSet()

        physicalFiles.forEach { fileName ->
            if (!dbFileNames.contains(fileName)) {
                val newEntity = AudioRequestEntity(
                    fileName = fileName,
                    status = AudioStatus.WAITING.value,
                    createdAt = now,
                    updatedAt = now
                )
                audioRequestDao.insertAudioRequest(newEntity)
                Log.d(tag, "Conciliación: Registro insertado automáticamente: $fileName")
            }
        }

        allDbRecords.forEach { entity ->
            if (!physicalFilesSet.contains(entity.fileName)) {
                audioRequestDao.deleteRequest(entity.id)
                Log.d(tag, "Conciliación: Registro fantasma eliminado de Room (ID: ${entity.id})")
            }
        }
    }

    override fun getAllActiveRequestsStream(): Flow<List<AudioDomain>> =
        audioRequestDao.getAllActiveRequestsStream().map { it.toAudioDomainList() }
    override fun getAllTrashedRequestsStream(): Flow<List<AudioDomain>> =
        audioRequestDao.getAllTrashedRequestsStream().map { it.toAudioDomainList() }
    override suspend fun getNextPendingRequestInQueue(): AudioDomain? = withContext(Dispatchers.IO) {
        audioRequestDao.getNextPendingRequestInQueue()?.toAudioDomain()
    }

    override fun startRecording() {
        fileManager.startRecording()
    }

    override suspend fun stopRecording(): String? {
        return fileManager.stopRecording()
    }

    // ─── CORRECCIÓN DE RETORNOS IMPLÍCITOS (Forzando Unit al final del bloque) ───

    override suspend fun updateRequestStatusByName(fileName: String, status: AudioStatus): Boolean = withContext(Dispatchers.IO) {
        val audioId = audioRequestDao.getFileIdByName(fileName) ?: return@withContext false

        audioRequestDao.updateRequestStatus(audioId, status.value, DateUtils.now())
        Log.d(tag, "Audio ID $audioId actualizado a ${status.value} en Room.")
        true
    }

    override suspend fun setRequestAsCompletedWithExpiration(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val audioId = audioRequestDao.getFileIdByName(fileName) ?: return@withContext false
        val now = DateUtils.now()
        val days = 2L
        val expirationTimestamp = DateUtils.getExpirationTimestamp(days = days)
        audioRequestDao.updateRequestStatus(audioId, AudioStatus.COMPLETED.value, now)
        audioRequestDao.setExpiration(audioId, expirationTimestamp, now)
        Log.d(tag, "Audio $fileName completado. Expira en $days días.")
        true
    }

    override suspend fun sendToTrash(id: Long, days: Long) = withContext(Dispatchers.IO) {
        val now = DateUtils.now()
        val expirationTimestamp = DateUtils.getExpirationTimestamp(days)
        audioRequestDao.sendToTrash(id, now, expirationTimestamp)
        Log.d(tag, "Audio ID $id enviado a la papelera en Room.")
        Unit
    }

    override suspend fun restoreSetOnDeleteAudio(id: Long, days: Long) = withContext(Dispatchers.IO) {
        val now = DateUtils.now()
        audioRequestDao.sendToTrash(id, now, days)
        Log.d(tag, "Audio ID $id ah sido colocado en la papelera de nuevo.")
        Unit
    }

    override suspend fun restoreRequest(id: Long) = withContext(Dispatchers.IO) {
        audioRequestDao.restoreRequest(id, DateUtils.now())
        Log.d(tag, "Audio ID $id restaurado de la papelera en Room.")
        Unit
    }

    override suspend fun deletePermanently(id: Long): Boolean = withContext(Dispatchers.IO) {
        var isPhysicalDeleted = false
        audioRequestDao.getFileNameById(id)?.let { fileName ->
            isPhysicalDeleted = fileManager.deleteAudioFile(fileName)
            audioRequestDao.deleteRequest(id)
            Log.d(tag, "Audio ID $id eliminado permanentemente del disco y Room.")
        }
        isPhysicalDeleted
    }

    override suspend fun purgeExpiredRequests() = withContext(Dispatchers.IO) {
        val now = DateUtils.now()
        val expiredRequests = audioRequestDao.getExpiredRequests(now)

        expiredRequests.forEach { entity ->
            fileManager.deleteAudioFile(entity.fileName)
            Log.d(tag, "Purga automática: Archivo expirado eliminado del disco: ${entity.fileName}")
        }
    }

    override suspend fun processPendingUploadsQueue(): Pair<NetworkResult, String> = withContext(Dispatchers.IO) {
        Log.d(tag, "Iniciando procesamiento manual de la cola de audios.")

        var resultPair: Pair<NetworkResult, String>

        if (audioRequestDao.getNextPendingRequestInQueue() == null) {
            return@withContext NetworkResult.SUCCESS to "No hay audios pendientes para enviar."
        }

        while (true) {
            val pendingRequest = audioRequestDao.getNextPendingRequestInQueue()

            if (pendingRequest == null) {
                Log.d(tag, "Todos los audios pendientes han sido procesados con éxito.")
                resultPair = NetworkResult.SUCCESS to "Todos los audios se enviaron correctamente."
                break
            }

            val audioId = pendingRequest.id
            val fileName = pendingRequest.fileName

            try {
                Log.d(tag, "Cambiando estado de audio $audioId a ${AudioStatus.PROCESSING.value}")
                audioRequestDao.updateRequestStatus(audioId, AudioStatus.PROCESSING.value, DateUtils.now())

                val audioFile: File = fileManager.resolveFileByName(fileName)

                if (!audioFile.exists()) {
                    Log.e(tag, "El archivo físico no existe en el almacenamiento: $fileName. Eliminando registro de Room.")
                    audioRequestDao.deleteRequest(audioId)
                    continue
                }

                Log.d(tag, "Transmitiendo archivo a la API: ${audioFile.name}")
                val (networkResult, serverMessage) = networkRepository.uploadVoiceAudio(audioFile)

                if (networkResult == NetworkResult.SUCCESS) {
                    Log.i(tag, "Audio $audioId enviado exitosamente: $serverMessage")
                    audioRequestDao.updateRequestStatus(audioId, AudioStatus.SENT.value, DateUtils.now())
                } else {
                    Log.e(tag, "Error en el servidor para el audio $audioId: $serverMessage")
                    audioRequestDao.updateRequestStatus(audioId, AudioStatus.FAILED.value, DateUtils.now())
                    resultPair = networkResult to serverMessage
                    break
                }

            } catch (e: Exception) {
                Log.e(tag, "Fallo crítico en el pipeline del audio $audioId", e)
                audioRequestDao.updateRequestStatus(audioId, AudioStatus.FAILED.value, DateUtils.now())
                resultPair = NetworkResult.LOGIC_ERROR to "Error inesperado en el repositorio: ${e.message}"
                break
            }
        }

        resultPair
    }
}