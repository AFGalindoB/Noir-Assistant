package com.afgalindob.assistantapp.data.repository.task

import android.util.Log
import com.afgalindob.assistantapp.data.domain.AudioStatus
import com.afgalindob.assistantapp.data.local.room.dao.TaskDao
import com.afgalindob.assistantapp.data.local.room.entity.TaskEntity
import com.afgalindob.assistantapp.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.afgalindob.assistantapp.data.domain.TaskDomain
import com.afgalindob.assistantapp.data.local.network.NetworkResult
import com.afgalindob.assistantapp.data.mapper.toTaskDomainList
import com.afgalindob.assistantapp.data.repository.audio.AudioRepository
import com.afgalindob.assistantapp.data.repository.network.NetworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val tag = "OfflineTaskRepository"
class OfflineTaskRepository(
    private val taskDao: TaskDao,
    private val networkRepository: NetworkRepository,
    private val audioRepository: AudioRepository
) : TaskRepository {

    override fun getTasks(showCompleted: Boolean, today: Long): Flow<List<TaskDomain>> =
        taskDao.getTasks(showCompleted, today).map { it.toTaskDomainList() }

    override fun getDeletedTasks(): Flow<List<TaskDomain>> =
        taskDao.getDeletedTasks().map { it.toTaskDomainList() }

    override suspend fun getPendingTasksForToday(endOfDay: Long): List<TaskDomain> = withContext(Dispatchers.IO) {
        taskDao.getPendingTasksForToday(endOfDay).toTaskDomainList()
    }

    override suspend fun insertTask(task: TaskDomain): Long = withContext(Dispatchers.IO) {
        val now = DateUtils.now()
        val entity = TaskEntity(
            title = task.title,
            content = task.content,
            date = task.date ?: 0L,
            completed = task.completed,
            createdAt = now,
            updatedAt = now,
            deleteAt = task.deleteAt ?: 0L
        )
        taskDao.insertTask(entity)
    }

    override suspend fun deleteTaskById(id: Long) =
        taskDao.deleteTaskById(id)

    override suspend fun updateTask(task: TaskDomain) = withContext(Dispatchers.IO) {
        taskDao.updateTaskFields(
            id = task.id,
            title = task.title,
            content = task.content,
            date = task.date ?: 0L,
            completed = task.completed,
            now = DateUtils.now()
        )
    }

    override suspend fun setOnDeleteTask(id: Long, days: Long) {
        val expirationTimestamp = DateUtils.getExpirationTimestamp(days)
        taskDao.setOnDeleteTask(id, expirationTimestamp)
    }

    override suspend fun restoreSetOnDeleteTask(id: Long, days: Long) =
        taskDao.setOnDeleteTask(id, days)

    override suspend fun restoreTask(id: Long) =
        taskDao.restoreTask(id)

    override suspend fun deleteExpiredTasks() = withContext(Dispatchers.IO) {
        val now = DateUtils.now()
        taskDao.deleteExpiredTasks(now)
    }

    override suspend fun getProcessedAudios(): Pair<NetworkResult, String> {
        val (networkResult, serverMessage, bodyString) = networkRepository.getProcessedAudios()

        if (networkResult != NetworkResult.SUCCESS) {
            return networkResult to serverMessage
        }

        if (bodyString.isBlank()) {
            return NetworkResult.LOGIC_ERROR to "El servidor devolvió un cuerpo de respuesta vacío."
        }

        return try {
            val rootObject = JSONObject(bodyString)
            val audiosArray = rootObject.optJSONArray("audios")
                ?: return NetworkResult.SUCCESS to "No se encontraron audios disponibles en el servidor."

            var validTasksInserted = 0

            for (i in 0 until audiosArray.length()) {
                var fileName: String? = null

                try {
                    val audioJson = audiosArray.getJSONObject(i)

                    fileName = audioJson.optString("audio_filename").ifBlank { null }

                    val header = audioJson.getString("header")
                    if (header != "Noir Assistant - Transcripción de Audio") {
                        Log.w(tag, "JSON descartado en índice $i: Header inválido o desconocido.")
                        // Si el header está mal pero tenemos el nombre, es un fallo de estructura seguro
                        fileName?.let { audioRepository.updateRequestStatusByName(it, AudioStatus.FAILED) }
                        continue
                    }

                    val titulo = audioJson.getString("titulo")
                    val descripcion = audioJson.getString("descripcion")
                    val fecha = audioJson.getString("fecha").ifBlank { "0" }.toLong()

                    val now = DateUtils.now()
                    val entity = TaskEntity(
                        title = titulo,
                        content = descripcion,
                        date = fecha,
                        completed = false,
                        createdAt = now,
                        updatedAt = now,
                        deleteAt = 0L
                    )

                    taskDao.insertTask(entity)
                    validTasksInserted++

                    if (fileName != null) {
                        audioRepository.setRequestAsCompletedWithExpiration(fileName)
                    } else {
                        Log.w(tag, "Tarea insertada en índice $i, pero 'audio_filename' vino vacío en el JSON.")
                    }

                } catch (e: Exception) {
                    Log.e(tag, "Error de formato o conversión en el JSON del índice $i. Saltando...", e)

                    fileName?.let {
                        audioRepository.updateRequestStatusByName(it, AudioStatus.FAILED)
                    }
                    continue
                }
            }

            Log.i(tag, "Procesamiento completado. $validTasksInserted nuevas tareas guardadas localmente.")
            NetworkResult.SUCCESS to "Se sincronizaron e insertaron $validTasksInserted tareas de audio con éxito."

        } catch (e: Exception) {
            Log.e(tag, "Error crítico al procesar y almacenar la lista de audios estructurados", e)
            NetworkResult.LOGIC_ERROR to "Error al deserializar las transcripciones del servidor."
        }
    }
}