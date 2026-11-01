package com.binarygeek119.covelounge.ui.performerdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.binarygeek119.covelounge.data.MediaCard


private fun countryFlag(name: String?): String {
    val code = when (name?.trim()?.lowercase()) {
        "united states", "usa", "us", "united states of america" -> "US"
        "united kingdom", "uk", "great britain", "england" -> "GB"
        "canada" -> "CA"
        "australia" -> "AU"
        "germany" -> "DE"
        "france" -> "FR"
        "italy" -> "IT"
        "spain" -> "ES"
        "japan" -> "JP"
        "brazil" -> "BR"
        "mexico" -> "MX"
        "russia" -> "RU"
        "china" -> "CN"
        "south korea", "korea" -> "KR"
        "netherlands" -> "NL"
        "sweden" -> "SE"
        "norway" -> "NO"
        "denmark" -> "DK"
        "poland" -> "PL"
        "czech republic", "czechia" -> "CZ"
        "hungary" -> "HU"
        "romania" -> "RO"
        "ukraine" -> "UA"
        "israel" -> "IL"
        "india" -> "IN"
        "philippines" -> "PH"
        "thailand" -> "TH"
        "vietnam" -> "VN"
        "new zealand" -> "NZ"
        "south africa" -> "ZA"
        "argentina" -> "AR"
        "colombia" -> "CO"
        else -> null
    }
    if (code == null) return ""
    val first = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
    val second = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}


private fun countryCode(name: String?): String? = when (name?.trim()?.lowercase()) {
    "united states", "usa", "us", "united states of america" -> "us"
    "united kingdom", "uk", "great britain", "england" -> "gb"
    "canada" -> "ca"
    "australia" -> "au"
    "germany" -> "de"
    "france" -> "fr"
    "italy" -> "it"
    "spain" -> "es"
    "japan" -> "jp"
    "brazil" -> "br"
    "mexico" -> "mx"
    "russia" -> "ru"
    "china" -> "cn"
    "south korea", "korea" -> "kr"
    "netherlands" -> "nl"
    "sweden" -> "se"
    "norway" -> "no"
    "denmark" -> "dk"
    "poland" -> "pl"
    "czech republic", "czechia" -> "cz"
    "hungary" -> "hu"
    "romania" -> "ro"
    "ukraine" -> "ua"
    "israel" -> "il"
    "india" -> "in"
    "philippines" -> "ph"
    "thailand" -> "th"
    "vietnam" -> "vn"
    "new zealand" -> "nz"
    "south africa" -> "za"
    "argentina" -> "ar"
    "colombia" -> "co"
    else -> null
}

@Composable
fun PerformerDetailScreen(
    viewModel: PerformerDetailViewModel,
    baseUrl: String,
    token: String,
    onBack: () -> Unit,
    onOpenItem: (MediaCard) -> Unit
) {
    val videos by viewModel.videos.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val born by viewModel.born.collectAsState()
    val country by viewModel.country.collectAsState()
    val ethnicity by viewModel.ethnicity.collectAsState()
    val height by viewModel.height.collectAsState()
    val measurements by viewModel.measurements.collectAsState()
    val eyeColor by viewModel.eyeColor.collectAsState()
    val hairColor by viewModel.hairColor.collectAsState()
    val fakeTits by viewModel.fakeTits.collectAsState()
    val tattoos by viewModel.tattoos.collectAsState()
    val piercings by viewModel.piercings.collectAsState()
    val career by viewModel.career.collectAsState()

    LaunchedEffect(Unit) { viewModel.init(baseUrl, token) }
    BackHandler { onBack() }

    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)) {
        // Top row: image on left, metadata on right
        Row(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = viewModel.performerImage,
                contentDescription = viewModel.performerName,
                modifier = Modifier.width(180.dp).height(320.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                // Performer image as background of metadata, gray fading into image on the right
                AsyncImage(
                    model = viewModel.performerImage,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    alpha = 0.4f
                )
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(Color(0xCC1A1A1A), Color.Transparent)
                            )
                        )
                )
                Column(modifier = Modifier.padding(8.dp)) {
                Text(viewModel.performerName ?: "Performer", color = Color.White, fontWeight = FontWeight.Bold)
                val meta = listOfNotNull(
                    gender?.let { "Gender: $it" },
                    born?.let { "Born: $it" },
                    country?.let { "Country: ${countryFlag(it)} $it".trim() },
                    ethnicity?.let { "Ethnicity: $it" },
                    height?.let { "Height: $it" },
                    measurements?.let { "Measurements: $it" },
                    eyeColor?.let { "Eye Color: $it" },
                    hairColor?.let { "Hair Color: $it" },
                    fakeTits?.let { "Fake Tits: $it" },
                    tattoos?.let { "Tattoos: $it" },
                    piercings?.let { "Piercings: $it" },
                    career?.let { "Career: $it" }
                )
                meta.forEach { line ->
                    Text(line, color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                country?.let { c ->
                    val code = countryCode(c)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        if (code != null) {
                            AsyncImage(
                                model = "https://flagcdn.com/w40/$code.png",
                                contentDescription = c,
                                modifier = Modifier.size(24.dp, 16.dp).clip(RoundedCornerShape(2.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text("  Country: $c", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
                }
            }
        }
        // Media rows below
        Text("Videos", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        if (loading) {
            CircularProgressIndicator(color = Color(0xFF2196F3))
        } else if (videos.isEmpty()) {
            Text("No videos", color = Color.Gray)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(videos) { card ->
                    var focused by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .width(160.dp)
                            .onFocusChanged { focused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                    (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                     event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
                                     event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                                    onOpenItem(card)
                                    true
                                } else false
                            }
                            .focusable()
                            .then(if (focused) Modifier.border(3.dp, Color(0xFF2196F3), RoundedCornerShape(8.dp)) else Modifier)
                            .clickable { onOpenItem(card) }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = card.posterUrl,
                            contentDescription = card.title,
                            modifier = Modifier.height(100.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(card.title ?: "Untitled", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        listOf("Images", "Galleries", "Audios", "Texts", "Groups", "Likes").forEach { label ->
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Text("No ${label.lowercase()}", color = Color.Gray)
        }
    }
}
