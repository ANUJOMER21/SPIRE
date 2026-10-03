package com.example.spire

import android.app.Application
import com.example.spire.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SpireApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SpireApp)
            modules(appModules)
        }
    }
}
