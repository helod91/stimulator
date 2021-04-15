package com.stim.data.di

import com.stim.data.repositories.StimulatorUdpRepository
import com.stim.domain.repositories.UdpRepository
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun udpRepository(repository: StimulatorUdpRepository): UdpRepository
}