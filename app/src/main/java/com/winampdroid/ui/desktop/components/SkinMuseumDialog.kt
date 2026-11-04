package com.winampdroid.ui.desktop.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.winampdroid.core.skin.WinampSkin
import com.winampdroid.core.skin.WszSkinParser
import com.winampdroid.core.skin.online.MuseumSkinItem
import com.winampdroid.core.skin.online.SkinMuseumClient
import com.winampdroid.core.storage.WinampPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Winamp Skin Museum Explorer Dialog.
 * Allows searching, previewing, and directly applying over 65,000 classic Winamp skins
 * hosted on Webamp's public CDN (skins.webamp.org).
 */
@Composable
fun SkinMuseumDialog(
    baseSkin: WinampSkin?,
    onDismissRequest: () -> Unit,
    onSkinApplied: (WinampSkin) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var skinsList by remember { mutableStateOf(SkinMuseumClient.FEATURED_CURATED_SKINS) }
    var isLoading by remember { mutableStateOf(false) }
    var downloadingSkinMd5 by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        val onlineSkins = SkinMuseumClient.fetchPopularSkins(offset = 0, count = 24)
        if (onlineSkins.isNotEmpty()) {
            skinsList = onlineSkins
        }
        isLoading = false
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF181824)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // 1. Header with Winamp Retro Branding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WINAMP SKIN MUSEUM",
                            color = Color(0xFF00FF66),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "+65,000 Skins Clásicas Online",
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp
                        )
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text("✕ CERRAR", color = Color(0xFFFF4444), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        scope.launch {
                            isLoading = true
                            skinsList = SkinMuseumClient.searchSkins(query)
                            isLoading = false
                        }
                    },
                    placeholder = { Text("Buscar skins (ej. Cyber, Gold, Mac, Anime)...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FF66),
                        unfocusedBorderColor = Color(0xFF444455),
                        cursorColor = Color(0xFF00FF66)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Grid of Skins or Loading Indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00FF66))
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(skinsList, key = { it.md5 }) { skinItem ->
                            val isDownloading = downloadingSkinMd5 == skinItem.md5

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF333348), RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF101018))
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    // Screenshot Thumbnail via Coil
                                    AsyncImage(
                                        model = skinItem.screenshotUrl,
                                        contentDescription = skinItem.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(85.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = skinItem.title,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Button(
                                        onClick = {
                                            if (isDownloading) return@Button
                                            downloadingSkinMd5 = skinItem.md5
                                            scope.launch {
                                                try {
                                                    val bytes = SkinMuseumClient.downloadSkinBytes(skinItem)
                                                    withContext(Dispatchers.IO) {
                                                        // Guardar archivo permanentemente en almacenamiento local para intercambio en la pestaña Skins
                                                        val skinsDir = File(context.filesDir, "downloaded_skins").apply { mkdirs() }
                                                        val skinFile = File(skinsDir, "${skinItem.md5}.wsz")
                                                        skinFile.writeBytes(bytes)

                                                        val prefs = WinampPreferences(context)
                                                        prefs.saveDownloadedSkin(skinItem.title, skinFile.absolutePath, skinItem.md5)
                                                        prefs.saveActiveSkin(skinFile.absolutePath)

                                                        val parsed = WszSkinParser.parse(
                                                            ByteArrayInputStream(bytes),
                                                            fallbackSkin = baseSkin
                                                        )
                                                        withContext(Dispatchers.Main) {
                                                            onSkinApplied(parsed)
                                                            Toast.makeText(
                                                                context,
                                                                "Skin '${skinItem.title}' descargada y aplicada",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            onDismissRequest()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            context,
                                                            "Error al descargar skin: ${e.message}",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                } finally {
                                                    downloadingSkinMd5 = null
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDownloading) Color.Gray else Color(0xFF00AA44)
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        if (isDownloading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = "APLICAR SKIN",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

