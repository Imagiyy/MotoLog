package com.abrar.motolog

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * MotoLog Application class.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation
 * and serve as the application-level dependency container.
 */
@HiltAndroidApp
class MotoLogApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.abrar.motolog.service.MaintenanceCheckWorker.schedule(this)
    }
}
