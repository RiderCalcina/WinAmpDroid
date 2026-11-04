package com.winampdroid.ui.desktop.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.winampdroid.core.storage.DownloadedSkin

/**
 * Categorías de gestión retro: Playlist y Skins (que incluye museo online y skins descargadas).
 */
enum class RetroMenuTab(val title: String, val icon: String) {
    PLAYLIST("PLAYLIST", "📋"),
    SKINS("SKINS", "🎨");

    companion object {
        val LOCAL_SKINS = SKINS
        val SKIN_MUSEUM = SKINS
    }
}

/**
 * Diálogo de Administración Retro para WinAmpDroid.
 * Diseño fiel a la interfaz clásica de Winamp 2.91 con paleta metálica oscura,
 * tipografía monospace y pestañas dedicadas: Administrador de Playlist y Skins (con Museo Online y Descargas).
 */
@Composable
fun RetroControlCenterDialog(
    isOpen: Boolean,
    initialTab: RetroMenuTab = RetroMenuTab.PLAYLIST,
    onDismissRequest: () -> Unit,
    // Acciones de Playlist
    isSearchActive: Boolean = false,
    onToggleSearch: () -> Unit = {},
    onSortTracks: (mode: String) -> Unit = {},
    onScanDeviceAudio: () -> Unit = {},
    onAddAudioFiles: () -> Unit = {},
    onClearTracks: () -> Unit = {},
    onResetDefaultPlaylist: () -> Unit = {},
    // Acciones de Skins
    builtInSkins: List<Pair<String, String>> = emptyList(),
    downloadedSkins: List<DownloadedSkin> = emptyList(),
    activeSkinPath: String? = null,
    onSelectBuiltInSkin: (name: String, path: String) -> Unit = { _, _ -> },
    onSelectDownloadedSkin: (DownloadedSkin) -> Unit = {},
    onDeleteDownloadedSkin: (DownloadedSkin) -> Unit = {},
    onImportLocalSkin: () -> Unit = {},
    // Winamp Skin Museum
    onOpenSkinMuseum: () -> Unit = {}
) {
    if (!isOpen) return

    var currentTab by remember(initialTab, isOpen) { mutableStateOf(initialTab) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(onClick = onDismissRequest),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.82f)
                    .clickable(enabled = false) {}, // Evita cerrar al hacer click dentro
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF141520),
                border = BorderStroke(2.dp, Color(0xFF383a54))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Barra de Título Retro con Branding Winamp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF222338))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ ", color = Color(0xFF00FF66), fontSize = 13.sp)
                            Text(
                                text = "WINAMP DROID - CONTROL",
                                color = Color(0xFF00FF66),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            onClick = onDismissRequest,
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF2d3046),
                            border = BorderStroke(1.dp, Color(0xFF505474))
                        ) {
                            Text(
                                text = " ✕ ",
                                color = Color(0xFFFF5555),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 2. Selector de Pestañas Retro (Estilo Tabs Clásicas)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF191a27))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RetroTabButton(
                            label = "📋 PLAYLIST",
                            isSelected = currentTab == RetroMenuTab.PLAYLIST,
                            modifier = Modifier.weight(1f)
                        ) {
                            currentTab = RetroMenuTab.PLAYLIST
                        }
                        RetroTabButton(
                            label = "🎨 SKINS",
                            isSelected = currentTab == RetroMenuTab.SKINS,
                            modifier = Modifier.weight(1f)
                        ) {
                            currentTab = RetroMenuTab.SKINS
                        }
                    }

                    // Divisor Retro
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF383a54))
                    )

                    // 3. Contenido de la Pestaña Activa
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        when (currentTab) {
                            RetroMenuTab.PLAYLIST -> {
                                PlaylistTabContent(
                                    isSearchActive = isSearchActive,
                                    onToggleSearch = onToggleSearch,
                                    onSortTracks = onSortTracks,
                                    onAddAudioFiles = onAddAudioFiles,
                                    onResetDefaultPlaylist = onResetDefaultPlaylist,
                                    onClearTracks = onClearTracks,
                                    onDismiss = onDismissRequest
                                )
                            }
                            RetroMenuTab.SKINS -> {
                                SkinsTabContent(
                                    builtInSkins = builtInSkins,
                                    downloadedSkins = downloadedSkins,
                                    activeSkinPath = activeSkinPath,
                                    onSelectBuiltInSkin = onSelectBuiltInSkin,
                                    onSelectDownloadedSkin = onSelectDownloadedSkin,
                                    onDeleteDownloadedSkin = onDeleteDownloadedSkin,
                                    onImportLocalSkin = onImportLocalSkin,
                                    onOpenMuseum = onOpenSkinMuseum,
                                    onDismiss = onDismissRequest
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetroTabButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                if (isSelected) Color(0xFF282a42) else Color(0xFF141520),
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                if (isSelected) Color(0xFF00E5FF) else Color(0xFF2e3046),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF8e90aa),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PlaylistTabContent(
    isSearchActive: Boolean,
    onToggleSearch: () -> Unit,
    onSortTracks: (mode: String) -> Unit,
    onAddAudioFiles: () -> Unit,
    onResetDefaultPlaylist: () -> Unit,
    onClearTracks: () -> Unit,
    onDismiss: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            RetroSectionHeader("─── 🔍 BÚSQUEDA & SALTO ───")
        }
        item {
            RetroMenuItem(
                icon = if (isSearchActive) "✕" else "🔍",
                title = if (isSearchActive) "Ocultar Buscador (Jump to Track)" else "Activar Buscador Rápido (Jump to Track)",
                color = Color(0xFF00FF66)
            ) {
                onToggleSearch()
                onDismiss()
            }
        }

        item {
            RetroSectionHeader("─── 🔀 ORDENACIÓN & MEZCLA ───")
        }
        item {
            RetroMenuItem(
                icon = "🔀",
                title = "Mezclar Lista Aleatoriamente (Shuffle)"
            ) {
                onSortTracks("shuffle")
                onDismiss()
            }
        }
        item {
            RetroMenuItem(
                icon = "🔤",
                title = "Ordenar por Título (A-Z)"
            ) {
                onSortTracks("title")
                onDismiss()
            }
        }
        item {
            RetroMenuItem(
                icon = "🎤",
                title = "Ordenar por Artista (A-Z)"
            ) {
                onSortTracks("artist")
                onDismiss()
            }
        }
        item {
            RetroMenuItem(
                icon = "🔄",
                title = "Invertir Orden Actual de la Lista"
            ) {
                onSortTracks("reverse")
                onDismiss()
            }
        }

        item {
            RetroSectionHeader("─── 📁 ALMACENAMIENTO DE AUDIO ───")
        }
        item {
            RetroMenuItem(
                icon = "🎵",
                title = "Añadir Canciones desde Almacenamiento",
                color = Color(0xFF00E5FF)
            ) {
                onAddAudioFiles()
                onDismiss()
            }
        }

        item {
            RetroSectionHeader("─── ⚠️ GESTIÓN DE LISTA ───")
        }
        item {
            RetroMenuItem(
                icon = "↺",
                title = "Restablecer Lista por Defecto (Intro + Radios)",
                color = Color(0xFF00FF66)
            ) {
                onResetDefaultPlaylist()
                onDismiss()
            }
        }
        item {
            RetroMenuItem(
                icon = "🧹",
                title = "Vaciar / Limpiar Toda la Lista",
                color = Color(0xFFFF5555)
            ) {
                onClearTracks()
                onDismiss()
            }
        }
    }
}

@Composable
private fun SkinsTabContent(
    builtInSkins: List<Pair<String, String>>,
    downloadedSkins: List<DownloadedSkin>,
    activeSkinPath: String?,
    onSelectBuiltInSkin: (name: String, path: String) -> Unit,
    onSelectDownloadedSkin: (DownloadedSkin) -> Unit,
    onDeleteDownloadedSkin: (DownloadedSkin) -> Unit,
    onImportLocalSkin: () -> Unit,
    onOpenMuseum: () -> Unit,
    onDismiss: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 1. Skin Base Preinstalada (Winamp 2.91 Clásico)
        item {
            RetroSectionHeader("─── ⭐ SKIN BASE CLÁSICA ───")
        }

        items(builtInSkins) { (skinName, assetPath) ->
            val isActive = assetPath == activeSkinPath
            Surface(
                onClick = {
                    onSelectBuiltInSkin(skinName, assetPath)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = if (isActive) Color(0xFF222a36) else Color(0xFF1c1d2c),
                border = BorderStroke(
                    1.dp,
                    if (isActive) Color(0xFF00FF66) else Color(0xFF33354c)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⭐ $skinName",
                        color = if (isActive) Color(0xFF00FF66) else Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isActive) {
                        Text(
                            text = "[ ACTIVA ✔ ]",
                            color = Color(0xFF00FF66),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Skins Descargadas desde Winamp Skin Museum
        item {
            RetroSectionHeader("─── 💾 SKINS DESCARGADAS (MUSEUM) ───")
        }
        if (downloadedSkins.isEmpty()) {
            item {
                Text(
                    text = "Aún no has descargado skins del museo. Abre Skin Museum abajo para descargar e intercambiar skins con 1 toque.",
                    color = Color(0xFF888b9e),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        } else {
            items(downloadedSkins) { dlSkin ->
                val isActive = dlSkin.filePath == activeSkinPath
                Surface(
                    onClick = {
                        onSelectDownloadedSkin(dlSkin)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = if (isActive) Color(0xFF222a36) else Color(0xFF1c1d2c),
                    border = BorderStroke(
                        1.dp,
                        if (isActive) Color(0xFF00FF66) else Color(0xFF33354c)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "🎨 ${dlSkin.title}",
                                color = if (isActive) Color(0xFF00FF66) else Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                            if (isActive) {
                                Text(
                                    text = "[ ACTIVA ✔ ]",
                                    color = Color(0xFF00FF66),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // Botón de eliminar skin descargada
                        Surface(
                            onClick = { onDeleteDownloadedSkin(dlSkin) },
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF3b1d1d),
                            border = BorderStroke(1.dp, Color(0xFFFF5555))
                        ) {
                            Text(
                                text = " ✕ ",
                                color = Color(0xFFFF5555),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Skins Externas (.wsz / .zip)
        item {
            RetroSectionHeader("─── 📁 SKINS EXTERNAS ───")
        }
        item {
            RetroMenuItem(
                icon = "📁",
                title = "Cargar Archivo .wsz Local desde el Teléfono",
                color = Color(0xFFFFCC00)
            ) {
                onImportLocalSkin()
                onDismiss()
            }
        }

        // 4. Winamp Skin Museum (+65,000 Skins Online)
        item {
            RetroSectionHeader("─── 🌐 ARCHIVO ONLINE (+65,000 SKINS) ───")
        }
        item {
            Surface(
                onClick = {
                    onDismiss()
                    onOpenMuseum()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF00384d),
                border = BorderStroke(1.dp, Color(0xFF00E5FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚀 ABRIR WINAMP SKIN MUSEUM",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        item {
            Text(
                text = "Explora y aplica más de 65,000 skins clásicas (1997-2005) alojadas en skins.webamp.org",
                color = Color(0xFF888b9e),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun RetroMenuItem(
    icon: String,
    title: String,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF1b1c2b),
        border = BorderStroke(1.dp, Color(0xFF303248))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = color,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RetroSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF6e7188),
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
