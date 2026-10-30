package com.binarygeek119.covelounge

import android.app.Application
import com.binarygeek119.covelounge.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CoveLoungeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CoveLoungeApp)
            modules(appModule)
        }
    }
}
