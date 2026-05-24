package com.afgalindob.assistantapp.navigation

import androidx.compose.material3.DrawerState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NavigationActions(
    private val scope: CoroutineScope,
    private val drawerState: DrawerState,
    private val onStartTransition: (Any) -> Unit
) {

    fun navigateToHome() {
        scope.launch {
            drawerState.close()
            onStartTransition(HomeGraph)
        }
    }

    fun navigateToTrash() {
        scope.launch {
            drawerState.close()
            onStartTransition(TrashGraph)
        }
    }

    fun navigateToVoiceTask() {
        scope.launch {
            drawerState.close()
            onStartTransition(VoiceTaskScreen)
        }
    }

    fun navigateToAccount() {
        scope.launch {
            drawerState.close()
            onStartTransition(AccountGraph)
        }
    }

    fun navigateInHome(destination: Any) {
        scope.launch {
            onStartTransition(destination)
        }
    }
}