package com.inialpha.executiveai

import android.app.Application
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.notification.NotificationHelper

class ExecutiveAIApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannels(this)
    }
}
