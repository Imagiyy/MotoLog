package com.abrar.motolog.di

import com.abrar.motolog.data.repository.TrackingRepositoryImpl
import com.abrar.motolog.domain.repository.TrackingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTrackingRepository(
        impl: TrackingRepositoryImpl
    ): TrackingRepository

    @Binds
    @Singleton
    abstract fun bindRideRepository(
        impl: com.abrar.motolog.data.repository.RideRepositoryImpl
    ): com.abrar.motolog.domain.repository.RideRepository

    @Binds
    @Singleton
    abstract fun bindGarageRepository(
        impl: com.abrar.motolog.data.repository.GarageRepositoryImpl
    ): com.abrar.motolog.domain.repository.GarageRepository
}
