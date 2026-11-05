package com.abrar.motolog

import android.app.Application
import android.util.Log
import com.abrar.motolog.data.io.BackupManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MotoLog Application class.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation
 * and serve as the application-level dependency container.
 */
@HiltAndroidApp
class MotoLogApp : Application() {
    @Inject lateinit var backupManager: BackupManager

    override fun onCreate() {
        super.onCreate()
        com.abrar.motolog.service.MaintenanceCheckWorker.schedule(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                backupManager.recoverPendingRestore()
            } catch (e: Exception) {
                Log.e("MotoLogApp", "Pending backup restore could not finish; it will be retried", e)
            }
        }
    }
}
