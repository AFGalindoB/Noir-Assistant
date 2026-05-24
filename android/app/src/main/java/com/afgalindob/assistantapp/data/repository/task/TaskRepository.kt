package com.afgalindob.assistantapp.data.repository.task

import com.afgalindob.assistantapp.data.domain.TaskDomain
import com.afgalindob.assistantapp.data.local.network.NetworkResult
import kotlinx.coroutines.flow.Flow

interface TaskRepository {

    fun getTasks(showCompleted: Boolean, today: Long): Flow<List<TaskDomain>>

    fun getDeletedTasks(): Flow<List<TaskDomain>>

    suspend fun getPendingTasksForToday(endOfDay: Long): List<TaskDomain>

    suspend fun insertTask(task: TaskDomain): Long

    suspend fun deleteTaskById(id: Long)

    suspend fun updateTask(task: TaskDomain)

    suspend fun setOnDeleteTask(id: Long, days: Long)

    suspend fun restoreSetOnDeleteTask(id: Long, days: Long)

    suspend fun restoreTask(id: Long)

    suspend fun deleteExpiredTasks()
    suspend fun getProcessedAudios(): Pair<NetworkResult, String>
}