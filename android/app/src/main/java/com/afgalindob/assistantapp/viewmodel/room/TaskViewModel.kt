package com.afgalindob.assistantapp.viewmodel.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afgalindob.assistantapp.data.repository.task.TaskRepository
import com.afgalindob.assistantapp.data.domain.TaskDomain
import com.afgalindob.assistantapp.data.domain.TaskFormState
import com.afgalindob.assistantapp.data.domain.validation.validateTaskForm
import com.afgalindob.assistantapp.data.local.network.NetworkResult
import com.afgalindob.assistantapp.data.local.network.ServerMessage
import com.afgalindob.assistantapp.data.local.network.ServerStatus
import com.afgalindob.assistantapp.data.manager.GlobalStateManager
import com.afgalindob.assistantapp.utils.DateUtils
import com.afgalindob.assistantapp.utils.sections.getTaskSection
import com.afgalindob.assistantapp.utils.sections.taskSectionOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository,
    private val globalStateManager: GlobalStateManager
) : ViewModel() {

    val serverStatus: StateFlow<ServerStatus> = globalStateManager.serverStatus
    val isTokenConfigured: StateFlow<Boolean> = globalStateManager.isTokenConfigured
    val isUrlConfigured: StateFlow<Boolean> = globalStateManager.isUrlConfigured

    private val showCompleted = MutableStateFlow(false)
    val showCompletedState: StateFlow<Boolean> = showCompleted

    // ─── FLUJO REACTIVO DIRECTO DE DOMINIO ───
    val tasksDomain: StateFlow<List<TaskDomain>> = showCompleted.flatMapLatest { showCompleted ->
        repository.getTasks(showCompleted, today = DateUtils.today())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tasksBySection: StateFlow<Map<Int, List<TaskDomain>>> =
        tasksDomain.map { list ->
            list.groupBy { it.getTaskSection() }
                .toSortedMap(compareBy { taskSectionOrder(it) })
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyMap()
        )

    fun toggleShowCompleted(showTaskCompleted: Boolean) {
        showCompleted.value = showTaskCompleted
    }

    fun validate(form: TaskFormState) = validateTaskForm(form)

    // ─── OPERACIONES DE ESCRITURA USANDO EL MODELO DE DOMINIO ───

    fun createTask(form: TaskFormState) {
        viewModelScope.launch {
            val task = TaskDomain(
                id = 0L,
                title = form.title,
                content = form.content,
                date = form.date,
                completed = form.completed,
                deleteAt = null
            )
            repository.insertTask(task)
        }
    }

    fun softDeleteTask(task: TaskDomain) {
        viewModelScope.launch {
            repository.setOnDeleteTask(id = task.id, days = 30)
        }
    }

    fun restoreTask(task: TaskDomain) {
        viewModelScope.launch {
            repository.restoreTask(task.id)
        }
    }

    fun toggleTaskCompleted(task: TaskDomain, completed: Boolean) {
        viewModelScope.launch {
            val updatedTask = TaskDomain(
                id = task.id,
                title = task.title,
                content = task.content,
                date = task.date,
                completed = completed,
                deleteAt = task.deleteAt
            )
            repository.updateTask(updatedTask)
        }
    }

    fun updateTask(task: TaskDomain, form: TaskFormState) {
        viewModelScope.launch {
            val updatedTask = TaskDomain(
                id = task.id,
                title = form.title,
                content = form.content,
                date = form.date,
                completed = form.completed,
                deleteAt = task.deleteAt
            )
            repository.updateTask(updatedTask)
        }
    }

    fun getProcessedAudios() {
        viewModelScope.launch {
            val (result, message) = repository.getProcessedAudios()
            when (result) {
                NetworkResult.SUCCESS -> {
                    globalStateManager.setServerMessage(ServerMessage("Audios procesados", message, false))
                }
                NetworkResult.LOGIC_ERROR -> {
                    globalStateManager.setServerMessage(ServerMessage("Error en la solicitud", message, true))
                }
                NetworkResult.CONNECTIVITY_ERROR -> {
                    globalStateManager.setServerMessage(ServerMessage("Fallo de Conexión", message, true))
                    globalStateManager.checkCommunicationWithServer()
                }
            }
        }
    }

    fun checkCommunicationWithServer() {
        globalStateManager.checkCommunicationWithServer(true)
    }
}