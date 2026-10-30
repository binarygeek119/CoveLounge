package com.binarygeek119.covelounge.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import com.binarygeek119.covelounge.R

object SoundManager {
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<Int, Int>()
    private var startupPlayer: MediaPlayer? = null

    fun init(context: Context) {
        if (soundPool != null) return
        soundPool = SoundPool.Builder().setMaxStreams(4).build()
        soundPool?.let { sp ->
            soundIds[R.raw.nav] = sp.load(context, R.raw.nav, 1)
            soundIds[R.raw.click] = sp.load(context, R.raw.click, 1)
            soundIds[R.raw.back] = sp.load(context, R.raw.back, 1)
        }
    }

    fun playNav() = playShort(R.raw.nav)
    fun playClick() = playShort(R.raw.click)
    fun playBack() = playShort(R.raw.back)

    private fun playShort(resId: Int) {
        val sp = soundPool ?: return
        val id = soundIds[resId] ?: return
        sp.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun playStartup(context: Context, onCompletion: () -> Unit = {}) {
        stopStartup()
        startupPlayer = MediaPlayer.create(context, R.raw.startup)?.apply {
            setOnCompletionListener {
                onCompletion()
                release()
                startupPlayer = null
            }
            start()
        }
    }

    fun stopStartup() {
        startupPlayer?.release()
        startupPlayer = null
    }
}
