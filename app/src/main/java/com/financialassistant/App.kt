package com.financialassistant

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Timber logging
        Timber.plant(Timber.DebugTree())
    }
}
