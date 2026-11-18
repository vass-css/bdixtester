package com.jubayerhossain.bdixtester

import android.app.Application
import android.os.Looper
import com.jubayerhossain.bdixtester.core.di.AppContainer

class BdixtesterApplication : Application() {
  lateinit var appContainer: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    appContainer = AppContainer(this)
    Looper.getMainLooper().queue.addIdleHandler {
      appContainer.initializeOnStartup()
      false
    }
  }
}
