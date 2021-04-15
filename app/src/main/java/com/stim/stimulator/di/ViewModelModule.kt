package com.stim.stimulator.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stim.stimulator.ui.devices.DevicesViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(DevicesViewModel::class)
    abstract fun devicesViewModel(viewMode: DevicesViewModel): ViewModel
}