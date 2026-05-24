package com.afgalindob.assistantapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.afgalindob.assistantapp.AssistantApplication
import com.afgalindob.assistantapp.utils.notifications.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            val container = (context.applicationContext as AssistantApplication).container
            val userRepository = container.settingsRepository

            CoroutineScope(Dispatchers.IO).launch {
                val prefs = userRepository.userData.first()
                prefs?.reminderTime?.let { time ->
                    AlarmScheduler.schedule(context, time)
                }
            }
        }
    }
}