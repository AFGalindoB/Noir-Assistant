package com.afgalindob.assistantapp.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.afgalindob.assistantapp.data.local.room.entity.AudioRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioRequestDao {
    @Query("SELECT * FROM audio_requests WHERE onTrash = 0 ORDER BY createdAt DESC")
    fun getAllActiveRequestsStream(): Flow<List<AudioRequestEntity>>

    @Query("SELECT * FROM audio_requests WHERE onTrash = 1 ORDER BY createdAt DESC")
    fun getAllTrashedRequestsStream(): Flow<List<AudioRequestEntity>>

    @Query("""
        SELECT * FROM audio_requests 
        WHERE status = 'WAITING' OR status = 'FAILED' AND deleteAt = 0  AND onTrash = 0
        ORDER BY createdAt ASC 
        LIMIT 1
    """)
    suspend fun getNextPendingRequestInQueue(): AudioRequestEntity?

    @Query("SELECT fileName FROM audio_requests WHERE id = :id")
    suspend fun getFileNameById(id: Long): String?

    @Query("SELECT id FROM audio_requests WHERE fileName = :fileName")
    suspend fun getFileIdByName(fileName: String): Long?

    @Query("SELECT * FROM audio_requests WHERE deleteAt != 0 AND deleteAt < :now")
    suspend fun getExpiredRequests(now: Long): List<AudioRequestEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAudioRequest(audioRequest: AudioRequestEntity): Long

    @Query("UPDATE audio_requests SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateRequestStatus(id: Long, status: String, now: Long)

    @Query("UPDATE audio_requests SET deleteAt = :expirationTimestamp, updatedAt = :now WHERE id = :id")
    suspend fun setExpiration(id: Long, expirationTimestamp: Long, now: Long)

    @Query("UPDATE audio_requests SET onTrash = 1, deleteAt = :expirationTimestamp, updatedAt = :now WHERE id = :id")
    suspend fun sendToTrash(id: Long, now: Long, expirationTimestamp: Long)

    @Query("UPDATE audio_requests SET onTrash = 0, deleteAt = 0, updatedAt = :now WHERE id = :id")
    suspend fun restoreRequest(id: Long, now: Long)

    @Query("DELETE FROM audio_requests WHERE id = :id")
    suspend fun deleteRequest(id: Long)
}