package kr.woorihaksik.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.woorihaksik.app.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 데이터 가져오기 순서
 * 1) 휴대폰에 저장해 둔 마지막 데이터(캐시) → 바로 화면에 표시
 * 2) 없으면 앱에 들어 있는 샘플
 * 3) 그 다음 인터넷(GitHub 저장소의 data/)에서 최신본을 받아 캐시를 갱신
 */
class Repository(private val context: Context) {

    enum class Origin { NETWORK, CACHE, SAMPLE }

    data class Loaded(val data: MenuData, val origin: Origin)

    private val baseUrl: String = BuildConfig.DATA_BASE_URL.trim().let {
        if (it.isEmpty() || it.endsWith("/")) it else "$it/"
    }

    val hasRemote: Boolean get() = baseUrl.isNotEmpty()

    suspend fun localSchools(): List<School> = withContext(Dispatchers.IO) {
        val text = readCache(SCHOOLS_FILE) ?: readAsset("sample/$SCHOOLS_FILE")
        text?.let { runCatching { JsonParser.schools(it) }.getOrNull() }.orEmpty()
    }

    suspend fun remoteSchools(): List<School>? = withContext(Dispatchers.IO) {
        val text = fetch(SCHOOLS_FILE) ?: return@withContext null
        val parsed = runCatching { JsonParser.schools(text) }.getOrNull() ?: return@withContext null
        writeCache(SCHOOLS_FILE, text)
        parsed
    }

    suspend fun localMenu(school: School): Loaded? = withContext(Dispatchers.IO) {
        readCache(menuCacheName(school))?.let { text ->
            runCatching { JsonParser.menu(text) }.getOrNull()?.let { return@withContext Loaded(it, Origin.CACHE) }
        }
        readAsset("sample/${school.dataPath}")?.let { text ->
            runCatching { JsonParser.menu(text) }.getOrNull()?.let { return@withContext Loaded(it, Origin.SAMPLE) }
        }
        null
    }

    suspend fun remoteMenu(school: School): MenuData? = withContext(Dispatchers.IO) {
        val text = fetch(school.dataPath) ?: return@withContext null
        val parsed = runCatching { JsonParser.menu(text) }.getOrNull() ?: return@withContext null
        writeCache(menuCacheName(school), text)
        parsed
    }

    private fun menuCacheName(school: School) = "menu_${school.id}.json"

    private fun fetch(path: String): String? = if (!hasRemote) null else runCatching {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.useCaches = false
        conn.setRequestProperty("Cache-Control", "no-cache")
        try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun readAsset(path: String): String? = runCatching {
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()

    private fun cacheFile(name: String) = File(context.filesDir, name)

    private fun readCache(name: String): String? =
        cacheFile(name).takeIf { it.exists() }?.readText(Charsets.UTF_8)

    private fun writeCache(name: String, text: String) {
        runCatching { cacheFile(name).writeText(text, Charsets.UTF_8) }
    }

    private companion object {
        const val SCHOOLS_FILE = "schools.json"
    }
}
