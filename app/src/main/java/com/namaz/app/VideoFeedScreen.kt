package com.namaz.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import android.util.Xml
import java.net.HttpURLConnection
import java.net.URL

private data class ChannelVideo(val id: String, val title: String, val channel: String, val published: String)

private object ChannelFeeds {
    private val channelPattern = Regex("^UC[A-Za-z0-9_-]{22}$")
    fun valid(input: String): Boolean = channelPattern.matches(input.trim())
    fun accepted(input: String): Boolean = runCatching { normalize(input) }.isSuccess
    private fun normalize(input: String): String {
        val raw = input.trim()
        if (valid(raw)) return raw
        val handle = if (raw.startsWith("@")) raw else {
            val uri = Uri.parse(if (raw.contains("://")) raw else "https://$raw")
            require(uri.scheme == "https" && uri.host?.lowercase() in setOf("youtube.com", "www.youtube.com", "m.youtube.com"))
            val path = uri.path.orEmpty().trim('/')
            if (path.startsWith("channel/")) return path.substringAfter("channel/").also { require(valid(it)) }
            require(path.startsWith("@"))
            path.substringBefore('/')
        }
        require(handle.matches(Regex("@[A-Za-z0-9._-]{3,40}")))
        return handle
    }
    fun resolve(input: String): String {
        val normalized = normalize(input)
        if (valid(normalized)) return normalized
        val connection = URL("https://www.youtube.com/" + normalized).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        try {
            require(connection.responseCode == 200) { "Kanal HTTP ${connection.responseCode}" }
            val html = connection.inputStream.bufferedReader().use { it.readText().take(2_000_000) }
            val patterns = listOf(
                Regex("""channelId"\s*:\s*"(UC[A-Za-z0-9_-]{22})"""),
                Regex("""externalId"\s*:\s*"(UC[A-Za-z0-9_-]{22})"""),
                Regex("""youtube\.com/channel/(UC[A-Za-z0-9_-]{22})""")
            )
            return patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1) }
                ?: error("Kanal kimliği otomatik bulunamadı")
        } finally { connection.disconnect() }
    }

    fun fetch(channelId: String): List<ChannelVideo> {
        require(valid(channelId))
        val connection = URL("https://www.youtube.com/feeds/videos.xml?channel_id=$channelId").openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        try {
            require(connection.responseCode == 200) { "Feed HTTP ${connection.responseCode}" }
            val parser = Xml.newPullParser()
            connection.inputStream.use { stream ->
                parser.setInput(stream, "UTF-8")
                val result = mutableListOf<ChannelVideo>()
                var inEntry = false
                var id = ""
                var title = ""
                var channel = ""
                var published = ""
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        when {
                            parser.name == "entry" -> { inEntry = true; id = ""; title = ""; published = "" }
                            parser.name == "name" && !inEntry -> channel = parser.nextText()
                            inEntry && parser.name == "videoId" -> id = parser.nextText()
                            inEntry && parser.name == "title" -> title = parser.nextText()
                            inEntry && parser.name == "published" -> published = parser.nextText()
                        }
                    } else if (event == XmlPullParser.END_TAG && parser.name == "entry") {
                        if (id.matches(Regex("[A-Za-z0-9_-]{11}"))) result.add(ChannelVideo(id, title, channel, published))
                        inEntry = false
                    }
                    event = parser.next()
                }
                return result
            }
        } finally { connection.disconnect() }
    }
}

@Composable
internal fun VideoFeedScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("video_channels", 0) }
    var channels by remember { mutableStateOf(prefs.getString("ids", "").orEmpty()) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var videos by remember { mutableStateOf<List<ChannelVideo>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    val ids = remember(channels) { channels.lines().map(String::trim).filter(String::isNotBlank).distinct().take(10) }
    LaunchedEffect(channels, revision) {
        videos = emptyList()
        if (ids.isEmpty()) return@LaunchedEffect
        loading = true
        error = ""
        try {
            val result = withContext(Dispatchers.IO) {
                val loaded = mutableListOf<ChannelVideo>()
                val failures = mutableListOf<String>()
                ids.forEach { id ->
                    try { loaded.addAll(ChannelFeeds.fetch(ChannelFeeds.resolve(id))) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (e: Exception) { failures.add("${id.take(45)}: ${e.javaClass.simpleName}: ${e.message.orEmpty()}") }
                }
                loaded to failures
            }
            videos = result.first.distinctBy { it.id }.sortedByDescending { it.published }
            if (result.second.isNotEmpty()) error = result.second.joinToString("\n")
        } catch (cancelled: CancellationException) { throw cancelled }
        finally { loading = false }
    }
    if (selected != null) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { selected = null }) { Text("‹ Videolara dön") }
            Text("Video", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            YoutubeEmbed(selected!!)
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$selected")))
            }) { Text("YouTube'da aç") }
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Videolar", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Seçtiğin YouTube kanallarının son yüklemeleri otomatik listelenir. Shorts ayrımı henüz yapılmıyor.",
                style = MaterialTheme.typography.bodySmall)
        }
        item {
            OutlinedButton(onClick = { editing = !editing }, modifier = Modifier.fillMaxWidth()) {
                Text("Kanalları yönet (${ids.size}/10)")
            }
        }
        if (editing) item {
            var draft by rememberSaveable { mutableStateOf(channels) }
            OutlinedTextField(draft, { draft = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("YouTube kanal linki veya @kullanıcıadı") },
                minLines = 3)
            Text("Örnek: https://youtube.com/@mikrofonyorumdaa. Her satıra bir kanal; en fazla 10.",
                style = MaterialTheme.typography.bodySmall)
            Button(enabled = draft.lines().map(String::trim).filter(String::isNotBlank).let {
                it.size <= 10 && it.all(ChannelFeeds::accepted)
            }, onClick = {
                channels = draft.lines().map(String::trim).filter(String::isNotBlank).distinct().joinToString("\n")
                prefs.edit().putString("ids", channels).apply()
                editing = false
            }) { Text("Kanalları kaydet") }
        }
        if (ids.isEmpty()) item { Text("Başlamak için 'Kanalları yönet' bölümünden bir YouTube kanal kimliği ekle.") }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
        if (ids.isNotEmpty()) item {
            OutlinedButton(onClick = { revision++ }) { Text("Videoları yenile") }
        }
        items(videos, key = { it.id }) { video ->
            DashboardCard(video.title, video.channel, "▶") { selected = video.id }
        }
        if (!loading && ids.isNotEmpty() && videos.isEmpty() && error.isBlank()) item {
            Text("Bu kanallardan henüz video bulunamadı.")
        }
        item { Text("Videolar YouTube'un resmi gömülü oynatıcısında açılır. Bazı içerikler gömülmeye kapalı olabilir.",
            style = MaterialTheme.typography.bodySmall) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YoutubeEmbed(videoId: String) {
    val context = LocalContext.current
    key(videoId) {
        AndroidView(factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                // YouTube requires an identifiable embedding origin (Error 153 otherwise).
                // loadDataWithBaseURL supplies a real HTTPS origin for the iframe.
                val origin = "https://com.namaz.app"
                val html = """
                    <!doctype html><html><head>
                    <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
                    <style>html,body{margin:0;width:100%;height:100%;background:#000}
                    iframe{border:0;width:100%;height:100%}</style>
                    </head><body>
                    <iframe src="https://www.youtube.com/embed/$videoId?playsinline=1&origin=$origin"
                      title="YouTube video" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture"
                      allowfullscreen referrerpolicy="strict-origin-when-cross-origin"></iframe>
                    </body></html>
                """.trimIndent()
                loadDataWithBaseURL("$origin/", html, "text/html", "UTF-8", null)
            }
        }, modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f),
            onRelease = { it.stopLoading(); it.destroy() })
    }
}
