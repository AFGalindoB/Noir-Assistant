package com.afgalindob.assistantapp

import android.app.Application
import com.afgalindob.assistantapp.data.container.AppContainer
import com.afgalindob.assistantapp.data.container.AppDataContainer

class AssistantApplication : Application() {
    val container: AppContainer by lazy {
        AppDataContainer(this)
    }
    override fun onCreate() {
        super.onCreate()
    }
}