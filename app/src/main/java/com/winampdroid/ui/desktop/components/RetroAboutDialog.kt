package com.winampdroid.ui.desktop.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.winampdroid.R

/**
 * Ventana About Retro de WinAmpDroid.
 *
 * Se abre al pulsar el emblemático rayo (logo) de Winamp en la ventana principal.
 * Diseño minimalista y compacto, semitransparente con estética retro futurista.
 * Se cierra al hacer tap en cualquier parte de la pantalla.
 */
@Composable
fun RetroAboutDialog(
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight()
                .padding(16.dp)
                .clickable { onDismiss() },
            shape = RoundedCornerShape(14.dp),
            color = Color(0xEE0D111A), // Semitransparente oscuro estilo retro glass
            border = BorderStroke(1.dp, Color(0xFF00FF66)), // Borde verde fósforo
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícono Oficial de la App (WinAmpDroid.png)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(Color(0x33000000), shape = RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0x5500FF66), shape = RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_winamp_droid),
                        contentDescription = "WinAmpDroid Icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "WinAmpDroid",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Leyenda solicitada por el usuario
                Text(
                    text = "De un fan para fans de WinAmp",
                    color = Color(0xFF00FFCC),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Enlace web www.qwertyaserty.com
                Text(
                    text = "www.qwertyaserty.com",
                    color = Color(0xFFFFB000),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color(0x22FFB000), shape = RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0x44FFB000), shape = RoundedCornerShape(6.dp))
                        .clickable {
                            val url = "https://www.qwertyaserty.com"
                            try {
                                uriHandler.openUri(url)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}
