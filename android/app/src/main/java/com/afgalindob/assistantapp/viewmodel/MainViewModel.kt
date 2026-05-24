package com.afgalindob.assistantapp.viewmodel

import android.util.Log
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afgalindob.assistantapp.data.container.AppContainer
import com.afgalindob.assistantapp.navigation.HomeGraph
import com.afgalindob.assistantapp.utils.DateUtils
import com.afgalindob.assistantapp.viewmodel.room.AudioViewModel
import com.afgalindob.assistantapp.viewmodel.room.NoteViewModel
import com.afgalindob.assistantapp.viewmodel.room.TaskViewModel
import com.afgalindob.assistantapp.viewmodel.room.TrashViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

private const val tag = "MainViewModel_Startup"
class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val _isAppReady = MutableStateFlow(false)
    val isAppReady = _isAppReady.asStateFlow()

    val subViewModels = AssistantViewModels(
        task = TaskViewModel(container.taskRepository, container.globalStateManager),
        note = NoteViewModel(container.noteRepository),
        trash = TrashViewModel(container.trashRepository),
        settings = SettingsViewModel(container.settingsRepository, container.globalStateManager),
        audio = AudioViewModel(container.audioRepository, container.globalStateManager)
    )

    // ─── VARIABLES PLANAS PARA LA GESTIÓN DE PANTALLAS UI ───
    private val _currentScreen = MutableStateFlow<Any>(HomeGraph)
    val currentScreen: StateFlow<Any> = _currentScreen.asStateFlow()

    private val _topBarActions = MutableStateFlow<@Composable RowScope.() -> Unit>({})
    val topBarActions: StateFlow<@Composable RowScope.() -> Unit> = _topBarActions.asStateFlow()
    private val _bottomBarContent = MutableStateFlow<(@Composable () -> Unit)?>(null)
    val bottomBarContent: StateFlow<(@Composable () -> Unit)?> = _bottomBarContent.asStateFlow()

    val isUrlConfigured: StateFlow<Boolean> = container.globalStateManager.isUrlConfigured

    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()

    private var pendingDestination: Any? = null

    init {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            Log.d(tag, "Iniciando arranque en frío y limpieza de mantenimiento...")
            try {
                val taskCleanup = async(Dispatchers.IO) { container.taskRepository.deleteExpiredTasks() }
                val noteCleanup = async(Dispatchers.IO) { container.noteRepository.deleteExpiredNotes() }
                val audioCleanup = async(Dispatchers.IO) { container.audioRepository.purgeExpiredRequests() }

                taskCleanup.await()
                noteCleanup.await()
                audioCleanup.await()

                Log.i(tag, "Mantenimiento completado con éxito.")

                container.taskRepository.getTasks(showCompleted = true, today = DateUtils.today()).firstOrNull()
                Log.d(tag, "Base de datos caliente y lista para peticiones.")

                container.globalStateManager.checkCommunicationWithServer()

            } catch (e: Exception) {
                Log.e(tag, "Error crítico durante el arranque en frío: ${e.message}", e)
            } finally {
                val endTime = System.currentTimeMillis()
                Log.i(tag, "Arranque finalizado en ${endTime - startTime}ms. Liberando Splash Screen.")

                yield()
                delay(200)
                _isAppReady.value = true
            }
        }
    }

    // ─── FLUJO DE NAVEGACIÓN Y LOGICA DE CONTROL DE TRANSICIÓN ───

    fun navigateTo(destination: Any) {
        if (_isTransitioning.value || pendingDestination != null) return

        pendingDestination = destination
        _isTransitioning.value = true
    }

    fun notifyCurtainFullyVisible() {
        pendingDestination?.let { target ->
            _topBarActions.value = {}
            _bottomBarContent.value = null
            _currentScreen.value = target
            pendingDestination = null
        }
    }

    fun notifyScreenRendered() {
        _isTransitioning.value = false
    }

    fun updateBars(
        topBar: @Composable RowScope.() -> Unit = {},
        bottomBar: (@Composable () -> Unit)? = null
    ) {
        _topBarActions.value = topBar
        _bottomBarContent.value = bottomBar
    }
}