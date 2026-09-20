package com.abrar.motolog.di

import com.abrar.motolog.domain.time.Clock
import com.abrar.motolog.domain.time.DefaultClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = DefaultClock
}
