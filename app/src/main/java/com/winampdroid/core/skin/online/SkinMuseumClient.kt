package com.winampdroid.core.skin.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Model representing a skin cataloged in the Winamp Skin Museum (skins.webamp.org).
 */
data class MuseumSkinItem(
    val md5: String,
    val filename: String,
    val title: String = filename.removeSuffix(".wsz").removeSuffix(".zip").replace('_', ' '),
    val screenshotUrl: String = "https://r2.webampskins.org/screenshots/$md5.png",
    val downloadUrl: String = "https://r2.webampskins.org/skins/$md5.wsz"
)

/**
 * Lightweight HTTP client for browsing and downloading skins from the Winamp Skin Museum API.
 */
object SkinMuseumClient {
    private const val GRAPHQL_ENDPOINT = "https://skins.webamp.org/graphql"

    /**
     * Handpicked Hall-of-Fame classic skins that work immediately offline or when network is slow.
     */
    val FEATURED_CURATED_SKINS = listOf(
        MuseumSkinItem("698c1737e5c1410cd044de80c8ba4043", "Mac_OS_X_Aqua.wsz", "Mac OS X Aqua"),
        MuseumSkinItem("9b15bc235cf571fe4cb3494bb945d947", "Classix_10.wsz", "Classix 10"),
        MuseumSkinItem("fa51e34e5a95914c62b665dfb12e1281", "Nucleo_NLog_v102.wsz", "Nucleo NLog"),
        MuseumSkinItem("e1d7465aa93d0ba6e7b5cb38d9d80315", "Zaxon_Amp.wsz", "Zaxon Cyber Amp"),
        MuseumSkinItem("f7ad5a242d5ba94e7c7a346e492215c0", "Pioneer_DEH_P8400MP.wsz", "Pioneer Car Audio"),
        MuseumSkinItem("20401b3ba74d81f211ce97d84fbb3a20", "Sonar_Winamp.wsz", "Sonar Radar Green"),
        MuseumSkinItem("3a1c2d5893a90be4df0882e75e5e3170", "Retro_Golden_Amp.wsz", "Retro Gold Edition"),
        MuseumSkinItem("10ff96055bc7f4ff5c868eb219bf6244", "Silent_Hill_Radio.wsz", "Silent Hill Radio"),
        MuseumSkinItem("512cf19ec1d07c08ecfe61a0f5a2a68c", "EVA_01_Test_Type.wsz", "Evangelion Unit 01"),
        MuseumSkinItem("6a1c87e4112e75da7e94df07a098075f", "Cyberpunk_2077_Arasaka.wsz", "Cyberpunk Arasaka"),
        MuseumSkinItem("c0082b24d7751752b0d00f607dcb2c11", "Sony_Walkman_Classic.wsz", "Sony Walkman Retro"),
        MuseumSkinItem("02ba14a873138b7244ec5e7f22718104", "Anime_Neon_Genesis.wsz", "Anime Neon Dream")
    )

    /**
     * Query popular skins from the live Webamp GraphQL endpoint.
     */
    suspend fun fetchPopularSkins(offset: Int = 0, count: Int = 24): List<MuseumSkinItem> = withContext(Dispatchers.IO) {
        val query = "{ skins(sort: MUSEUM, first: $count, offset: $offset) { nodes { ... on ClassicSkin { md5 filename } } } }"
        val url = "$GRAPHQL_ENDPOINT?query=${URLEncoder.encode(query, "UTF-8")}"
        try {
            val responseText = httpGet(url)
            parseNodes(responseText)
        } catch (_: Exception) {
            FEATURED_CURATED_SKINS
        }
    }

    /**
     * Search skins by keyword (e.g. "cyber", "gold", "matrix", "anime", "car").
     */
    suspend fun searchSkins(queryText: String, count: Int = 24): List<MuseumSkinItem> = withContext(Dispatchers.IO) {
        if (queryText.isBlank()) return@withContext fetchPopularSkins(0, count)
        val query = "{ search_skins(query: \"$queryText\", first: $count) { ... on ClassicSkin { md5 filename } } }"
        val url = "$GRAPHQL_ENDPOINT?query=${URLEncoder.encode(query, "UTF-8")}"
        try {
            val responseText = httpGet(url)
            val json = JSONObject(responseText)
            val data = json.optJSONObject("data")
            val nodes = data?.optJSONArray("search_skins")
            val list = mutableListOf<MuseumSkinItem>()
            if (nodes != null) {
                for (i in 0 until nodes.length()) {
                    val node = nodes.optJSONObject(i) ?: continue
                    val md5 = node.optString("md5")
                    val fn = node.optString("filename")
                    if (md5.isNotBlank()) {
                        list.add(MuseumSkinItem(md5, fn))
                    }
                }
            }
            if (list.isNotEmpty()) list else FEATURED_CURATED_SKINS.filter { it.title.contains(queryText, ignoreCase = true) }
        } catch (_: Exception) {
            FEATURED_CURATED_SKINS.filter { it.title.contains(queryText, ignoreCase = true) }
        }
    }

    /**
     * Downloads skin WSZ binary bytes.
     */
    suspend fun downloadSkinBytes(skin: MuseumSkinItem): ByteArray = withContext(Dispatchers.IO) {
        val url = URL(skin.downloadUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "WinAmpDroid/2.0")
        conn.inputStream.use { it.readBytes() }
    }

    private fun httpGet(urlStr: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "WinAmpDroid/2.0")
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseNodes(jsonStr: String): List<MuseumSkinItem> {
        val json = JSONObject(jsonStr)
        val data = json.optJSONObject("data") ?: return FEATURED_CURATED_SKINS
        val skins = data.optJSONObject("skins") ?: return FEATURED_CURATED_SKINS
        val nodes = skins.optJSONArray("nodes") ?: return FEATURED_CURATED_SKINS
        val list = mutableListOf<MuseumSkinItem>()
        for (i in 0 until nodes.length()) {
            val node = nodes.optJSONObject(i) ?: continue
            val md5 = node.optString("md5")
            val fn = node.optString("filename")
            if (md5.isNotBlank()) {
                list.add(MuseumSkinItem(md5, fn))
            }
        }
        return if (list.isNotEmpty()) list else FEATURED_CURATED_SKINS
    }
}

