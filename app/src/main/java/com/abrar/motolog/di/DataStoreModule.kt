package com.abrar.motolog.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Extension property for accessing the settings DataStore
 * from any Context. Defined at the top level as required by
 * the DataStore library.
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "motolog_settings"
)

/**
 * Hilt module providing the DataStore<Preferences> instance.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.settingsDataStore
}
