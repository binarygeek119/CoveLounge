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
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
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
import com.binarygeek119.covelounge.data.MediaCard
import com.binarygeek119.covelounge.data.MediaKind
import com.binarygeek119.covelounge.BuildConfig
import com.binarygeek119.covelounge.data.ServerStore
import com.binarygeek119.covelounge.util.UpdateChecker
import com.binarygeek119.covelounge.quickpin.QuickPinScreen
import com.binarygeek119.covelounge.quickpin.QuickPinViewModel
import com.binarygeek119.covelounge.ui.detail.MediaDetailScreen
import com.binarygeek119.covelounge.ui.detail.MediaDetailViewModel
import com.binarygeek119.covelounge.ui.home.HomeScreen
import com.binarygeek119.covelounge.ui.medialist.MediaListScreen
import com.binarygeek119.covelounge.ui.medialist.MediaListViewModel
import com.binarygeek119.covelounge.ui.home.HomeViewModel
import com.binarygeek119.covelounge.ui.login.LoginScreen
import com.binarygeek119.covelounge.ui.login.LoginViewModel
import com.binarygeek119.covelounge.ui.media.ImageScreen
import com.binarygeek119.covelounge.ui.media.PlayerScreen
import com.binarygeek119.covelounge.ui.settings.SettingsScreen
import com.binarygeek119.covelounge.ui.search.SearchScreen
import com.binarygeek119.covelounge.ui.search.SearchViewModel
import com.binarygeek119.covelounge.ui.theme.CoveLoungeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ServerStore(applicationContext)
        val loginVm = LoginViewModel(application)
        val homeVm = HomeViewModel(application)
        val quickPinVm = QuickPinViewModel(application)
        SoundManager.init(application)

        val backCallback = object : androidx.activity.OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                SoundManager.playBack()
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

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
                var selectedPreview by remember { mutableStateOf<Int?>(null) }
                var detailKind by remember { mutableStateOf<MediaKind?>(null) }
                var detailId by remember { mutableStateOf<Int?>(null) }
                var showSettings by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                var updateInfo by remember { mutableStateOf<UpdateChecker.ReleaseInfo?>(null) }
                var downloading by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    val info = UpdateChecker.checkLatest()
                    if (info != null && UpdateChecker.isNewer(BuildConfig.VERSION_NAME, info.tag)) {
                        updateInfo = info
                    }
                }
                var showSearch by remember { mutableStateOf(false) }
                var selectedFolder by remember { mutableStateOf<MediaKind?>(null) }
                val atHome = detailKind == null && selectedVideo == null && selectedAudio == null && selectedImage == null && selectedPreview == null && selectedFolder == null && !showSettings && !showSearch
                LaunchedEffect(atHome) { backCallback.isEnabled = atHome }
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
                    detailKind != null && detailId != null -> {
                        val kind = detailKind!!
                        val id = detailId!!
                        val vm = MediaDetailViewModel(application, kind, id)
                        vm.init(url, token)
                        MediaDetailScreen(vm, url ?: "", token ?: "", onBack = { detailKind = null; detailId = null }, onPlay = {
                            SoundManager.playClick()
                            when (kind) {
                                MediaKind.VIDEO -> selectedVideo = id
                                MediaKind.AUDIO -> selectedAudio = id
                                MediaKind.IMAGE -> selectedImage = id
                                MediaKind.GALLERY -> selectedImage = id
                            }
                            detailKind = null
                            detailId = null
                        }, onPreview = {
                            SoundManager.playClick()
                            selectedPreview = id
                            detailKind = null
                            detailId = null
                        })
                    }
                    selectedPreview != null -> PlayerScreen(url!!, token ?: "", "preview", selectedPreview!!) { SoundManager.playBack(); selectedPreview = null }
                    selectedVideo != null -> PlayerScreen(url!!, token ?: "", "video", selectedVideo!!) { SoundManager.playBack(); selectedVideo = null }
                    selectedAudio != null -> PlayerScreen(url!!, token ?: "", "audio", selectedAudio!!) { SoundManager.playBack(); selectedAudio = null }
                    selectedImage != null -> ImageScreen(url!!, token ?: "", selectedImage!!) { SoundManager.playBack(); selectedImage = null }
                    selectedFolder != null -> {
                        val kind = selectedFolder!!
                        val vm = MediaListViewModel(application, kind)
                        MediaListScreen(vm, url ?: "", token ?: "", onBack = { selectedFolder = null }) { card ->
                            SoundManager.playClick()
                            when (kind) {
                                MediaKind.VIDEO -> { detailKind = MediaKind.VIDEO; detailId = card.id }
                                MediaKind.AUDIO -> { detailKind = MediaKind.AUDIO; detailId = card.id }
                                MediaKind.IMAGE -> { detailKind = MediaKind.IMAGE; detailId = card.id }
                                MediaKind.GALLERY -> { detailKind = MediaKind.GALLERY; detailId = card.id }
                            }
                        }
                    }
                    showSearch -> {
                        val vm = SearchViewModel(application)
                        vm.init(url, token)
                        SearchScreen(vm, url ?: "", token ?: "", onBack = { showSearch = false }) { card ->
                            SoundManager.playClick()
                            when (card.kind) {
                                MediaKind.VIDEO -> { detailKind = MediaKind.VIDEO; detailId = card.id }
                                MediaKind.AUDIO -> { detailKind = MediaKind.AUDIO; detailId = card.id }
                                MediaKind.IMAGE -> { detailKind = MediaKind.IMAGE; detailId = card.id }
                                MediaKind.GALLERY -> { detailKind = MediaKind.GALLERY; detailId = card.id }
                            }
                            showSearch = false
                        }
                    }
                    showSettings -> SettingsScreen(onBack = { showSettings = false }, onLogout = { scope.launch { store.clear() } })
                    else -> {
                        BackHandler { SoundManager.playBack() }
                        homeVm.init(url, token)
                        HomeScreen(homeVm, url ?: "", token ?: "",
                            onOpenItem = { kind, id -> SoundManager.playClick(); detailKind = kind; detailId = id },
                            onOpenFolder = { kind -> selectedFolder = kind },
                            onSettingsClick = { showSettings = true },
                            onSearchClick = { showSearch = true })
                    }
                }

            updateInfo?.let { info ->
                AlertDialog(
                    onDismissRequest = { updateInfo = null },
                    title = { Text("Update available") },
                    text = { Text("Version ${info.tag} is available. Download and install?") },
                    confirmButton = {
                        TextButton(onClick = {
                            if (info.apkUrl != null && !downloading) {
                                downloading = true
                                scope.launch {
                                    val file = UpdateChecker.downloadApk(application, info.apkUrl)
                                    downloading = false
                                    if (file != null) UpdateChecker.installApk(application, file)
                                    updateInfo = null
                                }
                            }
                        }) { Text(if (downloading) "Downloading..." else "Download") }
                    },
                    dismissButton = {
                        TextButton(onClick = { updateInfo = null }) { Text("Later") }
                    }
                )
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
