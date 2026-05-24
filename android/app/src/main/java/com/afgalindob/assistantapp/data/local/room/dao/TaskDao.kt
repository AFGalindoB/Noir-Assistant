package com.afgalindob.assistantapp.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.afgalindob.assistantapp.data.local.room.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("""
        SELECT * FROM tasks 
        WHERE 
            (date >= :today OR :showCompleted OR completed = 0)
            AND deleteAt = 0
        ORDER BY date ASC
    """)
    fun getTasks(showCompleted: Boolean, today: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE date <= :endOfDay 
        AND completed = 0 
        AND deleteAt = 0
    """)
    suspend fun getPendingTasksForToday(endOfDay: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE deleteAt > 0 ORDER BY deleteAt ASC")
    fun getDeletedTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTask(task: TaskEntity): Long

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET title = :title, content = :content, date = :date, completed = :completed, updatedAt = :now WHERE id = :id")
    suspend fun updateTaskFields(id: Long, title: String, content: String, date: Long, completed: Boolean, now: Long)

    @Query("UPDATE tasks SET deleteAt = :timestamp WHERE id = :id")
    suspend fun setOnDeleteTask(id: Long, timestamp: Long)

    @Query("UPDATE tasks SET deleteAt = 0 WHERE id = :id")
    suspend fun restoreTask(id: Long)

    @Query("DELETE FROM tasks WHERE deleteAt != 0 AND deleteAt < :now")
    suspend fun deleteExpiredTasks(now: Long)
}