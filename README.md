# ⚡ WinAmpDroid

<p align="center">
  <img src="WinAmpDroid.png" alt="WinAmpDroid Logo" width="180">
</p>

<p align="center">
  <b>El legendario reproductor de música de los años 2000, ahora en tu teléfono Android.</b><br>
  <i>Fiel pixel a pixel a Winamp 2.91, con soporte completo de skins, ecualizador de 10 bandas y visualizador Milkdrop 3D.</i>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Plataforma-Android%208.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Version">
  <img src="https://img.shields.io/badge/Versi%C3%B3n-1.0.0-blue?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/badge/Lenguaje-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
</p>

---

## ✨ ¿Por qué WinAmpDroid?

**WinAmpDroid** no es una página web envuelta ni un clon moderno genérico. Es una aplicación **100% nativa para Android** construida con **Kotlin**, **Jetpack Compose** y el motor de audio **AndroidX Media3**. 

Recrea con exactitud milimétrica la interfaz, los sonidos y la mística del reproductor de audio más querido de la historia, adaptado a la comodidad de las pantallas táctiles actuales.

---

## 🚀 Características Principales

* 📻 **Diseño Retro Clásico de 3 Ventanas:**
  * **MainWindow:** Panel frontal con botones de reproducción, marquesina de texto bitmap, contador de tiempo digital y analizador de espectro.
  * **EqualizerWindow:** Ecualizador analógico de 10 bandas con curva spline reactiva y control de preamplificador (Preamp).
  * **PlaylistWindow:** Lista de canciones interactiva con búsqueda rápida, barra de desplazamiento táctil y seguimiento automático de la canción que suena.
* 🎨 **Skins Clásicas y +65,000 Skins Online:**
  * Compatible con cualquier skin original de PC en formato `.wsz`.
  * **Skin Museum Integrado:** Explora y descarga miles de skins directamente desde la aplicación con un solo toque.
* 🌌 **Visualizador Milkdrop 3D a Pantalla Completa:**
  * Impresionante motor visualizador acelerado por GPU (WebGL 2.0) a 60 FPS con efectos psicodélicos que reaccionan al ritmo de tu música.
* 📊 **Analizador de Espectro FFT Real:**
  * 19 barras dinámicas sincronizadas exactamente con los altavoces de tu teléfono. Puedes alternar con un toque entre las **barras de frecuencia** y el **osciloscopio**.
* 🎚️ **13 Predefinidos Oficiales de Ecualización:**
  * Incluye los preajustes legendarios: *Rock, Techno, Full Bass, Pop, Classical, Club, Dance, Full Treble, Reggae, Ska, Soft, Vocal y Flat*.
  * Guarda tus propias curvas personalizadas con tu nombre favorito.
* 🎧 **Reproducción en Segundo Plano y Pantalla de Bloqueo:**
  * Tu música no se detiene al apagar la pantalla o cambiar de aplicación.
  * Notificación multimedia moderna con controles interactivos (Play, Pausa, Siguiente, Anterior).
* 🔄 **Modo Paisaje (Horizontal) Automático:**
  * Gira tu teléfono en horizontal y la aplicación se transforma automáticamente en un visualizador panorámico Milkdrop con controles flotantes centrados.
* 🔍 **Lupa Táctil Retro (Touch Loupe):**
  * Mantén presionado cualquier botón pequeño para ver una lente de aumento retro con el atajo de teclado original de PC.

---

## 📥 Cómo Instalar en tu Celular

1. Dirígete a la pestaña de **[Releases](../../releases)** de este repositorio.
2. Descarga el archivo **`WinAmpDroid-v1.0.0-release.apk`**.
3. En tu teléfono Android, abre el archivo descargado y presiona **Instalar**. *(Si tu teléfono lo solicita, permite la instalación de aplicaciones de fuentes desconocidas)*.
4. ¡Abre **WinAmpDroid**, carga tu música y disfruta del clásico sonido retro!

---

## 🎮 Gestos y Atajos Útiles

| Gesto / Acción | ¿Qué hace? |
| :--- | :--- |
| **Doble toque en el reloj digital (`00:00`) o en las barras** | Abre el visualizador **Milkdrop a pantalla completa**. |
| **Doble toque en cualquier barra de título** | Alterna el **Modo Windowshade** (contrae la ventana a una barra compacta). |
| **Doble toque en una canción de la Playlist** | Inicia la reproducción inmediata del tema seleccionado. |
| **Toque sobre el Rayo de Winamp** | Abre la ventana de créditos y Acerca de. |
| **Girar el celular a horizontal** | Activa la pantalla panorámica Milkdrop. |

---

## 📱 Requisitos del Sistema

* **Sistema Operativo:** Android 8.0 (Oreo) o superior (compatible con Android 9, 10, 11, 12, 13, 14, 15 y 16).
* **Formatos de Audio:** MP3, AAC, FLAC, WAV, OGG, M4A y emisiones de radio online (Icecast / Shoutcast).
* **Permisos requeridos:** Acceso a audio para leer tu biblioteca musical local y notificación para control en pantalla de bloqueo. **No requiere conexión obligatoria ni cuenta de usuario.**

---

## 🛠️ Para Desarrolladores

Si deseas compilar la aplicación por tu cuenta o contribuir al código:

1. Clona el repositorio:
   ```bash
   git clone https://github.com/RiderCalcina/WinAmpDroid.git
   ```
2. Ábrelo en **Android Studio Ladybug (o superior)** con **JDK 21**.
3. Compila el APK de prueba:
   ```bash
   ./gradlew assembleDebug
   ```

4. Genera el APK Release:
   ```bash
   ./gradlew assembleRelease
   ```

---

<p align="center">
  <i>WinAmpDroid es un proyecto tributo desarrollado con pasión por y para los amantes de la era dorada de la música digital.</i><br>
  <b>It really whips the llama's ass! 🦙⚡</b>
</p>
