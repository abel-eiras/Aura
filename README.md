# Aura

Aura es una app nativa de Android que graba notas de voz y las sube
automáticamente a una carpeta llamada **"Aura"** en tu propio Google Drive.
Sin nada oculto: la notificación mientras grabas, el icono en Ajustes rápidos
y la pantalla principal siempre dicen claramente si está grabando o no.

Aura no está en Google Play: para usarla te la compilas tú mismo (ver
[Cómo compilarla](#cómo-compilarla) más abajo) con tu propia cuenta de Google
Cloud. Tarda unos 15-20 minutos la primera vez, casi todo dentro de la consola
de Google Cloud, y no hace falta saber programar.

Las notas de voz que graba Aura se pueden transcribir automáticamente, con
diarización (quién habla cuándo) y notas de Obsidian ya redactadas, con
[aura-transcribe](https://github.com/abel-eiras/aura-transcribe) — un
pipeline que corre en tu propio ordenador, no en la nube. No es obligatorio:
Aura simplemente deja los audios en tu Drive; qué hagas con ellos después es
cosa tuya.

## Qué hace

- Toca el círculo de la pantalla principal, o el icono **Aura** en Ajustes
  rápidos, para empezar/parar a grabar.
- Mientras grabas, una notificación fija dice "Aura — Grabando audio" y la
  pantalla principal muestra un contador en directo.
- Las grabaciones se suben solas y se reintentan automáticamente (con
  esperas crecientes) si no hay red, y también al abrir la app.
- Puedes pausar/reanudar una grabación en curso desde la app, desde la
  notificación, o automáticamente si entra una llamada.
- La pantalla **Grabaciones** lista las notas de voz locales con su estado de
  subida, te deja reproducirlas, y reintentar las que hayan fallado.
- El espacio local para grabaciones pendientes de subir tiene un límite: si
  las subidas siguen fallando, se descartan antes las más antiguas en vez de
  llenar el teléfono.
- Inicias sesión con tu propia cuenta de Google; la app solo pide el permiso
  `drive.file`, que le deja ver y crear únicamente los ficheros que *ella
  misma* sube — nunca el resto de tu Drive.

## Permisos que pide

| Permiso | Para qué |
|---|---|
| `RECORD_AUDIO` | Capturar audio del micrófono. |
| `READ_PHONE_STATE` | Detectar llamadas entrantes/en curso para pausar y reanudar la grabación automáticamente. Opcional — si lo deniegas, la grabación sigue igual durante las llamadas. |
| `POST_NOTIFICATIONS` | Mostrar la notificación de "Grabando audio" del servicio en primer plano. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE` | Que la grabación siga funcionando de forma fiable con la app en segundo plano. |
| `INTERNET` | Subir las grabaciones a Google Drive. |

## Cómo está hecha por dentro

Para quien quiera curiosear o tocar el código:

- Solo Kotlin, una única `Activity` con Jetpack Compose (Material3, color
  dinámico en Android 12+).
- `ViewModel` → `UseCase` (dominio) → `Repository` (datos) → fuente de datos
  (`MediaRecorder`, Retrofit/OkHttp, `EncryptedSharedPreferences`).
- Hilt para inyección de dependencias.
- `MediaRecorder` graba en Opus dentro de OGG a ~64 kbps en Android 10+
  (`AudioEncoder.OPUS` exige API 29); en Android 8.0–9.0 cae a AAC dentro de
  M4A, porque OGG/Opus no está disponible en esas versiones.
- Un `Service` en primer plano (`FOREGROUND_SERVICE_TYPE_MICROPHONE`) es
  quien controla el grabador, para que la grabación siga con la app en
  segundo plano.
- La subida va directa a la API REST v3 de Drive (multipart/related, no el
  SDK de Drive para Android) vía Retrofit/OkHttp.
- Una cola con `WorkManager` reintenta las subidas fallidas con esperas
  crecientes: 15s → 30s → 1m → 5m → 15m, con un máximo de 5 intentos.
- Los tokens, el ID de la carpeta de Drive en caché, y similares viven en
  `EncryptedSharedPreferences` (clave maestra AES256-GCM).

## Cómo compilarla

### 1. Crea un proyecto de Google Cloud

1. Ve a la [Consola de Google Cloud](https://console.cloud.google.com/) y
   crea un proyecto nuevo (o usa uno que ya tengas).
2. En **APIs y servicios → Biblioteca**, busca **Google Drive API** y dale a
   **Habilitar**.

### 2. Configura la pantalla de consentimiento OAuth

1. Ve a **APIs y servicios → Pantalla de consentimiento de OAuth**.
2. Elige el tipo de usuario **Externo** (a menos que tengas una organización
   de Google Workspace y quieras **Interno**).
3. Rellena la información obligatoria de la app (nombre, email de soporte).
4. En **Permisos (Scopes)**, añade `.../auth/drive.file`.
5. En **Usuarios de prueba**, añade la(s) cuenta(s) de Google con las que vas
   a iniciar sesión en el móvil. Mientras la app no esté publicada, solo esas
   cuentas pueden iniciar sesión — esto también te evita la pantalla de aviso
   de "app no verificada" de Google mientras la pruebas tú mismo.

### 3. Crea un cliente OAuth y descarga `google-services.json`

1. Ve a **APIs y servicios → Credenciales → Crear credenciales → ID de
   cliente de OAuth**.
2. Tipo de aplicación: **Android**.
3. Nombre del paquete: `com.aura.app`.
4. Huella SHA-1 del certificado, consíguela con:
   ```
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
   (usa la huella SHA-1 de tu keystore de release en vez de esta si vas a
   compilar un APK firmado para publicar).
5. Ve a la [Consola de Firebase](https://console.firebase.google.com/), crea
   o elige un proyecto que apunte al **mismo** proyecto de Google Cloud,
   añade una app Android con el paquete `com.aura.app`, y descarga
   `google-services.json`.

   (Aquí Firebase solo se usa como forma cómoda de generar el
   `google-services.json` que necesita el inicio de sesión con Google — Aura
   no usa ningún otro servicio de Firebase.)
6. Coloca el fichero descargado en `app/google-services.json`. Este fichero
   está en `.gitignore`: nunca subas el tuyo real a ningún sitio.

### 4. Genera el jar del wrapper de Gradle

Este repositorio incluye `gradlew`, `gradlew.bat`, y
`gradle/wrapper/gradle-wrapper.properties` (fijado a Gradle 8.9), pero no el
binario `gradle-wrapper.jar` (los binarios no deberían subirse a mano).
Genéralo una vez, desde una máquina con acceso normal a internet hacia
`dl.google.com` / `services.gradle.org`:

```
gradle wrapper --gradle-version 8.9
```

Ejecuta esto desde la raíz del proyecto — rellenará
`gradle/wrapper/gradle-wrapper.jar` usando el `gradle-wrapper.properties` que
ya está en el repo. Si no tienes un `gradle` instalado en el sistema, abrir el
proyecto en Android Studio y dejar que sincronice genera el mismo fichero
automáticamente.

### 5. Compila e instala

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

O abre el proyecto en Android Studio (Iguana o más reciente) y ejecútalo
directamente en un dispositivo o emulador — Studio se encarga de configurar
el SDK y Gradle por ti.

Al abrirla por primera vez, Aura pide los permisos de **Grabar audio**,
**Estado del teléfono** y **Notificaciones**, explicando en lenguaje llano
por qué hace falta cada uno. Inicia sesión con una cuenta de Google desde la
pantalla de **Ajustes** antes de grabar, para que las subidas puedan
completarse (las grabaciones hechas sin haber iniciado sesión se quedan en
cola y se suben en cuanto inicias sesión).

## Generar una versión firmada (release)

`assembleRelease` corre con minificación R8 y reducción de recursos activadas,
pero se queda sin firmar a menos que `local.properties` (ya está en
`.gitignore`: nunca subas el tuyo real) tenga configurado un keystore de
release:

```
RELEASE_STORE_FILE=/ruta/absoluta/a/tu-release.keystore
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

Genera el keystore una vez, **fuera** de este repositorio (por ejemplo, junto
a tu keystore de debug en `~/.android/`), y guárdalo a buen recaudo: si lo
pierdes, pierdes la capacidad de publicar actualizaciones bajo la misma
identidad de app:

```
keytool -genkeypair -v -keystore /ruta/a/tu-release.keystore \
  -alias tu-alias -keyalg RSA -keysize 2048 -validity 10000
```

La huella SHA-1 de la clave de release es **distinta** de la de tu keystore
de debug, así que el inicio de sesión con Google necesita su propio cliente
OAuth de Android registrado en la Consola de Google Cloud (paso 3 de arriba)
antes de que el inicio de sesión funcione en una compilación firmada para
release. Consíguela con:

```
keytool -list -v -keystore /ruta/a/tu-release.keystore -alias tu-alias
```
