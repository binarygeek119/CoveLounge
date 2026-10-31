package com.binarygeek119.covelounge

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.binarygeek119.covelounge.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CoveLoungeApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CoveLoungeApp)
            modules(appModule)
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024) // 512 MB persistent image cache
                    .build()
            }
            // Cache Cove images regardless of server cache headers so they load instantly on revisit/restart
            .respectCacheHeaders(false)
            .build()
}
