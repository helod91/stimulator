package com.stim.stimulator.di

import com.stim.stimulator.ui.devices.DevicesActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [ViewModelModule::class])
abstract class ActivityModule {

    @ContributesAndroidInjector
    abstract fun devicesActivity(): DevicesActivity
}