package com.managesoundmode.app

import android.app.Application
import com.managesoundmode.app.di.AppContainer
import com.managesoundmode.app.di.DefaultAppContainer

class ManageSoundApplication : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = DefaultAppContainer(applicationContext)
  }
}
