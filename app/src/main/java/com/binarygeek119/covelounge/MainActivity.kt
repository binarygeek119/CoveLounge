package com.binarygeek119.covelounge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                if (useQuickPin) LaunchedEffect(Unit) { quickPinVm.start() }

                when {
                    token == null && !loggedIn && !quickSuccess -> {
                        if (useQuickPin) {
                            QuickPinScreen(quickPinVm) { useQuickPin = false }
                        } else {
                            LoginScreen(loginVm, onLoggedIn = { }, onQuickPin = { useQuickPin = true })
                        }
                    }
                    selectedVideo != null -> PlayerScreen(url!!, token ?: "", "video", selectedVideo!!) { selectedVideo = null }
                    selectedAudio != null -> PlayerScreen(url!!, token ?: "", "audio", selectedAudio!!) { selectedAudio = null }
                    selectedImage != null -> ImageScreen(url!!, token ?: "", selectedImage!!) { selectedImage = null }
                    else -> {
                        homeVm.init(url, token)
                        HomeScreen(homeVm, url ?: "", token ?: "",
                            onPlayVideo = { selectedVideo = it },
                            onPlayAudio = { selectedAudio = it },
                            onViewImage = { selectedImage = it })
                    }
                }
            }
        }
    }
}
