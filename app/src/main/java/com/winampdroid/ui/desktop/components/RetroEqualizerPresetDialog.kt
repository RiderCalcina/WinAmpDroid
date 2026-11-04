package com.winampdroid.ui.desktop.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.winampdroid.core.audio.WinampEqualizerPreset

/**
 * Ventana Retro de Presets del Ecualizador Winamp.
 *
 * Reemplaza los diálogos genéricos de Android por una ventana flotante auténtica
 * con la estética cyberpunk/retro de WinAmpDroid:
 * - Cabecera retro con branding "⚡ WINAMP EQUALIZER - PRESETS"
 * - Grilla de 13 presets oficiales de fábrica con selección instantánea
 * - Gestión completa de presets personalizados (cargar y eliminar)
 * - Módulo integrado para guardar la curva actual de 10 bandas
 * - Cierre rápido por tap en fondo o botón [✕]
 */
@Composable
fun RetroEqualizerPresetDialog(
    customPresets: List<WinampEqualizerPreset>,
    onSelectPreset: (WinampEqualizerPreset) -> Unit,
    onSaveCustomPreset: (name: String) -> Unit,
    onDeleteCustomPreset: (name: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var isSavingNewPreset by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

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
                    .fillMaxWidth(0.90f)
                    .wrapContentHeight()
                    .clickable(enabled = false) {}, // Evita cerrar al tocar el interior
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF141520), // Fondo retro oscuro
                border = BorderStroke(2.dp, Color(0xFF383A54)), // Marco gris azulado retro
                shadowElevation = 20.dp
            ) {
                Column(modifier = Modifier.wrapContentHeight()) {
                    // 1. Barra de Título Retro con Branding Winamp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF222338))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ ", color = Color(0xFFFFB000), fontSize = 13.sp)
                            Text(
                                text = "WINAMP EQUALIZER - PRESETS",
                                color = Color(0xFF00FF66),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            onClick = onDismissRequest,
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF2D3046),
                            border = BorderStroke(1.dp, Color(0xFF505474))
                        ) {
                            Text(
                                text = " ✕ ",
                                color = Color(0xFFFF5555),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 2. Contenido Scrollable con Presets y Acciones
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Grilla de 2 columnas de los 13 presets oficiales
                        val officialChunks = WinampEqualizerPreset.DEFAULT_PRESETS.chunked(2)
                        items(officialChunks) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (preset in pair) {
                                    RetroPresetButton(
                                        name = preset.name,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            onSelectPreset(preset)
                                            onDismissRequest()
                                        }
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // Sección: Presets Personalizados (si existen)
                        if (customPresets.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "── 💾 PRESETS PERSONALIZADOS (${customPresets.size}) ──",
                                    color = Color(0xFFFFB000),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            items(customPresets) { preset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF191B28), RoundedCornerShape(4.dp))
                                        .border(BorderStroke(1.dp, Color(0xFF2C3048)), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                onSelectPreset(preset)
                                                onDismissRequest()
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎛️ ",
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = preset.name,
                                            color = Color(0xFF00FFCC),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Botón de eliminar preset personalizado
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0x33FF0055), shape = RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0x66FF0055), shape = RoundedCornerShape(4.dp))
                                            .clickable { onDeleteCustomPreset(preset.name) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✕",
                                            color = Color(0xFFFF4466),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Sección: Guardar Ajuste Actual como Preset
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            if (!isSavingNewPreset) {
                                Surface(
                                    onClick = { isSavingNewPreset = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1A261D),
                                    border = BorderStroke(1.dp, Color(0xFF00FF66))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("➕ ", color = Color(0xFF00FF66), fontSize = 12.sp)
                                        Text(
                                            text = "GUARDAR CURVA ACTUAL COMO PRESET",
                                            color = Color(0xFF00FF66),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            } else {
                                // Formulario inline retro para ingresar el nombre
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF181C28), RoundedCornerShape(6.dp))
                                        .border(BorderStroke(1.dp, Color(0xFF00FF66)), RoundedCornerShape(6.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "NOMBRE DEL NUEVO PRESET:",
                                        color = Color(0xFF00FFCC),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    BasicTextField(
                                        value = newPresetName,
                                        onValueChange = { newPresetName = it },
                                        textStyle = TextStyle(
                                            color = Color(0xFF00FF66),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        cursorBrush = SolidColor(Color(0xFF00FF66)),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0A0C14), RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0xFF335544), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            onClick = {
                                                isSavingNewPreset = false
                                                newPresetName = ""
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF262838),
                                            border = BorderStroke(1.dp, Color(0xFF444860))
                                        ) {
                                            Text(
                                                text = "CANCELAR",
                                                color = Color(0xFF8888AA),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }

                                        Surface(
                                            onClick = {
                                                if (newPresetName.isNotBlank()) {
                                                    onSaveCustomPreset(newPresetName.trim())
                                                    newPresetName = ""
                                                    isSavingNewPreset = false
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF143820),
                                            border = BorderStroke(1.dp, Color(0xFF00FF66))
                                        ) {
                                            Text(
                                                text = "GUARDAR",
                                                color = Color(0xFF00FF66),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
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

/**
 * Botón individual para un preset del ecualizador.
 */
@Composable
private fun RetroPresetButton(
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF1C1E2E),
        border = BorderStroke(1.dp, Color(0xFF2F3248))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "▶",
                color = Color(0xFF00FF66),
                fontSize = 9.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = name,
                color = Color(0xFFDDE0F0),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

