package com.binarygeek119.covelounge.util

import android.content.Context
import coil.Coil
import java.io.File

object ImageCache {
    /** Directory of the persistent Coil disk cache (see CoveLoungeApp). */
    fun dir(context: Context): File = context.cacheDir.resolve("image_cache")

    /** Total bytes currently stored in the image disk cache. */
    fun sizeBytes(context: Context): Long {
        val d = dir(context)
        if (!d.exists()) return 0L
        return d.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    }

    /** Clears both the in-memory cache and the on-disk image cache. */
    fun clear(context: Context) {
        val loader = Coil.imageLoader(context)
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
        dir(context).deleteRecursively()
    }
}
