package com.afgalindob.assistantapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.afgalindob.assistantapp.data.manager.GlobalStateManager
import com.afgalindob.assistantapp.ui.components.overlays.CurtainOverlay
import kotlinx.coroutines.delay
import com.afgalindob.assistantapp.viewmodel.MainViewModel
import com.afgalindob.assistantapp.ui.components.overlays.LoadingOverlay
import com.afgalindob.assistantapp.ui.dialogs.alert.AdvertisementDialog
import com.afgalindob.assistantapp.ui.theme.AssistantTheme
import com.afgalindob.assistantapp.utils.LanguageUtils
import com.afgalindob.assistantapp.utils.notifications.AlarmScheduler
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val container = (applicationContext as AssistantApplication).container
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(container) as T
            }
        }
    }

    private lateinit var globalStateManager: GlobalStateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        splashScreen.setKeepOnScreenCondition { !viewModel.isAppReady.value }

        val container = (applicationContext as AssistantApplication).container
        globalStateManager = container.globalStateManager

        setContent {
            val isAppReady by viewModel.isAppReady.collectAsState()
            val langByRepo by container.settingsRepository.languageData.collectAsState(initial = null)
            val preferences by container.settingsRepository.userData.collectAsState(initial = null)
            var showOverlay by remember { mutableStateOf(false) }
            val hasNotificationPermission by globalStateManager.hasNotificationPermission.collectAsState()
            val serverMessage by globalStateManager.serverMessage.collectAsState()

            val isTransitioning by viewModel.isTransitioning.collectAsState()

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ ->
                globalStateManager.refreshSystemStates()
            }

            LaunchedEffect(isAppReady) {
                if (isAppReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            LaunchedEffect(langByRepo, isAppReady) {
                if (isAppReady && langByRepo != null) {
                    val currentAppLang = AppCompatDelegate.getApplicationLocales().get(0)?.language
                    if (langByRepo != currentAppLang) {
                        showOverlay = true
                        delay(500)
                        LanguageUtils.applyAppLanguage(langByRepo!!)
                    }
                }
            }

            LaunchedEffect(preferences?.reminderTime) {
                preferences?.reminderTime?.let { time ->
                    Log.d("Main_Alarm", "Sincronizando alarma para las: $time")
                    AlarmScheduler.schedule(applicationContext, time)
                }
            }

            AssistantTheme {
                if (isAppReady) {
                    AssistantApp(viewModel)
                } else {
                    Box(Modifier.fillMaxSize())
                }
                CurtainOverlay(
                    isVisible = isTransitioning,
                    onFullyVisible = { viewModel.notifyCurtainFullyVisible() }
                )
                LoadingOverlay(isVisible = showOverlay)
                serverMessage?.let { message ->
                    AdvertisementDialog(
                        titleToShow = message.title,
                        descriptionToShow = message.description,
                        alert = message.isError,
                        onConfirm = { globalStateManager.clearMessage() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::globalStateManager.isInitialized) {
            globalStateManager.refreshSystemStates()
        }
    }
}
