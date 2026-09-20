package com.abrar.motolog.di

import android.content.Context
import androidx.room.Room
import com.abrar.motolog.data.local.MotoLogDatabase
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.FuelLogDao
import com.abrar.motolog.data.local.dao.MaintenanceDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the Room database and DAO instances.
 * Scoped to SingletonComponent for app-wide lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MotoLogDatabase {
        return Room.databaseBuilder(
            context,
            MotoLogDatabase::class.java,
            "motolog.db"
        )
            .addMigrations(MotoLogDatabase.MIGRATION_1_2, MotoLogDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideBikeDao(database: MotoLogDatabase): BikeDao = database.bikeDao()

    @Provides
    fun provideRideDao(database: MotoLogDatabase): RideDao = database.rideDao()

    @Provides
    fun provideRidePointDao(database: MotoLogDatabase): RidePointDao = database.ridePointDao()

    @Provides
    fun provideMaintenanceDao(database: MotoLogDatabase): MaintenanceDao = database.maintenanceDao()

    @Provides
    fun provideFuelLogDao(database: MotoLogDatabase): FuelLogDao = database.fuelLogDao()
}
