package com.abrar.motolog.di

import com.abrar.motolog.data.routing.DefaultHttpTransport
import com.abrar.motolog.data.routing.GraphHopperGeocodingRepository
import com.abrar.motolog.data.routing.GraphHopperRoutingRepository
import com.abrar.motolog.data.routing.HttpTransport
import com.abrar.motolog.shared.domain.repository.GeocodingRepository
import com.abrar.motolog.shared.domain.repository.RoutingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RoutingModule {

    @Binds
    @Singleton
    abstract fun bindHttpTransport(
        impl: DefaultHttpTransport
    ): HttpTransport

    @Binds
    @Singleton
    abstract fun bindRoutingRepository(
        impl: GraphHopperRoutingRepository
    ): RoutingRepository

    @Binds
    @Singleton
    abstract fun bindGeocodingRepository(
        impl: GraphHopperGeocodingRepository
    ): GeocodingRepository
}
