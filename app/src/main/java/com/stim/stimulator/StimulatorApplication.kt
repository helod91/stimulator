package com.stim.stimulator

import com.stim.stimulator.di.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication

class StimulatorApplication : DaggerApplication() {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.builder()
            .application(this)
            .build()
    }
}