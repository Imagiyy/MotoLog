package com.abrar.motolog.di

import com.abrar.motolog.data.map.OpenFreeMapStyleProvider
import com.abrar.motolog.data.map.MapLibreMapProvider
import com.abrar.motolog.data.sensor.AndroidBarometerSource
import com.abrar.motolog.data.sensor.BarometerSource
import com.abrar.motolog.domain.map.MapStyleProvider
import com.abrar.motolog.domain.map.MapProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing map and sensor bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MapModule {

    @Binds
    @Singleton
    abstract fun bindMapProvider(impl: MapLibreMapProvider): MapProvider

    @Binds
    @Singleton
    abstract fun bindMapStyleProvider(impl: OpenFreeMapStyleProvider): MapStyleProvider

    @Binds
    @Singleton
    abstract fun bindBarometerSource(impl: AndroidBarometerSource): BarometerSource
}
