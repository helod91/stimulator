package com.stim.stimulator.di

import android.app.Application
import com.stim.data.di.RepositoryModule
import com.stim.stimulator.StimulatorApplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AndroidInjectionModule::class,
        ActivityModule::class,
        RepositoryModule::class
    ]
)
interface AppComponent : AndroidInjector<StimulatorApplication> {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }
}