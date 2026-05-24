package com.afgalindob.assistantapp.data.container

import android.content.Context
import com.afgalindob.assistantapp.data.local.room.db.AppDatabase
import com.afgalindob.assistantapp.data.local.audio.SmartAudioFileManager
import com.afgalindob.assistantapp.data.local.preferences.UserPreferencesManager
import com.afgalindob.assistantapp.data.repository.audio.AudioRepository
import com.afgalindob.assistantapp.data.repository.audio.OfflineAudioRepository
import com.afgalindob.assistantapp.data.repository.note.NoteRepository
import com.afgalindob.assistantapp.data.repository.note.OfflineNoteRepository
import com.afgalindob.assistantapp.data.repository.task.OfflineTaskRepository
import com.afgalindob.assistantapp.data.repository.task.TaskRepository
import com.afgalindob.assistantapp.data.repository.trash.OfflineTrashRepository
import com.afgalindob.assistantapp.data.repository.trash.TrashRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.afgalindob.assistantapp.data.manager.AndroidGlobalStateManager
import com.afgalindob.assistantapp.data.manager.GlobalStateManager
import com.afgalindob.assistantapp.data.repository.network.NetworkRepository
import com.afgalindob.assistantapp.data.repository.network.NetworkRepositoryImpl
import com.afgalindob.assistantapp.data.repository.settings.OfflineSettingsRepository
import com.afgalindob.assistantapp.data.repository.settings.SettingsRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppDataContainer(private val context: Context) : AppContainer {

    private val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val userPreferencesManager: UserPreferencesManager by lazy {
        UserPreferencesManager(context)
    }

    private val audioFileManager: SmartAudioFileManager by lazy {
        SmartAudioFileManager(context, applicationScope)
    }

    override val globalStateManager: GlobalStateManager by lazy {
        AndroidGlobalStateManager(
            context = context,
            settingsRepository = settingsRepository,
            externalScope = applicationScope
        )
    }

    override val taskRepository: TaskRepository by lazy {
        OfflineTaskRepository(
            taskDao = database.taskDao(),
            audioRepository = audioRepository,
            networkRepository = networkRepository
        )
    }

    override val noteRepository: NoteRepository by lazy {
        OfflineNoteRepository(database.noteDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        OfflineSettingsRepository(
            networkRepository = networkRepository,
            preferencesManager = userPreferencesManager
        )
    }

    override val audioRepository: AudioRepository by lazy {
        OfflineAudioRepository(
            fileManager = audioFileManager,
            audioRequestDao = database.audioRequestDao(),
            networkRepository = networkRepository,
            externalScope = applicationScope
        )
    }

    override val trashRepository: TrashRepository by lazy {
        OfflineTrashRepository(taskRepository, noteRepository, audioRepository)
    }

    override val networkRepository: NetworkRepository by lazy {
        NetworkRepositoryImpl(
            preferencesManager = userPreferencesManager,
            httpClient = httpClient
        )
    }

}