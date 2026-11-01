package com.abrar.motolog.di

import com.abrar.motolog.data.routing.DefaultHttpTransport
import com.abrar.motolog.data.routing.GraphHopperGeocodingRepository
import com.abrar.motolog.data.routing.GraphHopperRoutingRepository
import com.abrar.motolog.data.routing.HttpTransport
import com.abrar.motolog.shared.domain.repository.GeocodingRepository
import com.abrar.motolog.shared.domain.repository.RoutingRepository
import com.abrar.motolog.shared.domain.engine.RouteProgressEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
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

    @Binds
    @Singleton
    abstract fun bindVoiceAnnouncer(
        impl: com.abrar.motolog.service.NavigationVoiceAnnouncer
    ): com.abrar.motolog.shared.domain.voice.VoiceAnnouncer

    companion object {
        @Provides
        fun provideRouteProgressEngine(): RouteProgressEngine = RouteProgressEngine()

        @Provides
        fun provideNavigationVoiceEngine(): com.abrar.motolog.shared.domain.engine.NavigationVoiceEngine =
            com.abrar.motolog.shared.domain.engine.NavigationVoiceEngine()

        @Provides
        @Singleton
        fun provideNavigationDiagnostics(): com.abrar.motolog.shared.domain.diagnostics.NavigationDiagnostics =
            com.abrar.motolog.shared.domain.diagnostics.NavigationDiagnostics()
    }
}
