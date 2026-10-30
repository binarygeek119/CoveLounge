package com.binarygeek119.covelounge

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import com.binarygeek119.covelounge.R
import com.binarygeek119.covelounge.audio.SoundManager
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.quickpin.QuickPinScreen
import com.binarygeek119.covelounge.quickpin.QuickPinViewModel
import com.binarygeek119.covelounge.ui.home.HomeScreen
import com.binarygeek119.covelounge.ui.home.HomeViewModel
import com.binarygeek119.covelounge.ui.login.LoginScreen
import com.binarygeek119.covelounge.ui.login.LoginViewModel
import com.binarygeek119.covelounge.ui.media.ImageScreen
import com.binarygeek119.covelounge.ui.media.PlayerScreen
import com.binarygeek119.covelounge.ui.theme.CoveLoungeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ServerStore(applicationContext)
        val loginVm = LoginViewModel(application)
        val homeVm = HomeViewModel(application)
        val quickPinVm = QuickPinViewModel(application)
        SoundManager.init(application)

        setContent {
            CoveLoungeTheme {
                val token by store.accessToken.collectAsState(initial = null)
                val url by store.serverUrl.collectAsState(initial = null)
                val loggedIn by loginVm.success.collectAsState()
                val quickSuccess by quickPinVm.success.collectAsState()
                var useQuickPin by remember { mutableStateOf(false) }
                var selectedVideo by remember { mutableStateOf<Int?>(null) }
                var selectedAudio by remember { mutableStateOf<Int?>(null) }
                var selectedImage by remember { mutableStateOf<Int?>(null) }
                var showSplash by remember { mutableStateOf(true) }
                val infiniteTransition = rememberInfiniteTransition(label = "logo")
                val scale by infiniteTransition.animateFloat(initialValue = 0.85f, targetValue = 1.15f, animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "scale")
                LaunchedEffect(Unit) {
                    SoundManager.playStartup(application) { showSplash = false }
                    delay(4000)
                    showSplash = false
                }
                if (useQuickPin) LaunchedEffect(Unit) { quickPinVm.start() }

                if (showSplash) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "CoveLounge", modifier = androidx.compose.ui.Modifier.fillMaxSize(0.6f).then(androidx.compose.ui.Modifier.scale(scale)), contentScale = ContentScale.Fit)
                    }
                } else when {
                    token == null && !loggedIn && !quickSuccess -> {
                        if (useQuickPin) {
                            QuickPinScreen(quickPinVm) { useQuickPin = false }
                        } else {
                            LoginScreen(loginVm, onLoggedIn = { }, onQuickPin = { useQuickPin = true })
                        }
                    }
                    selectedVideo != null -> PlayerScreen(url!!, token ?: "", "video", selectedVideo!!) { SoundManager.playBack(); selectedVideo = null }
                    selectedAudio != null -> PlayerScreen(url!!, token ?: "", "audio", selectedAudio!!) { SoundManager.playBack(); selectedAudio = null }
                    selectedImage != null -> ImageScreen(url!!, token ?: "", selectedImage!!) { SoundManager.playBack(); selectedImage = null }
                    else -> {
                        BackHandler { SoundManager.playBack(); finish() }
                        homeVm.init(url, token)
                        HomeScreen(homeVm, url ?: "", token ?: "",
                            onPlayVideo = { SoundManager.playClick(); selectedVideo = it },
                            onPlayAudio = { SoundManager.playClick(); selectedAudio = it },
                            onViewImage = { SoundManager.playClick(); selectedImage = it },
                            onOpenFolder = { })
                    }
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> SoundManager.playNav()
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
