# Mi Pizarra (Android nativo, Kotlin + Jetpack Compose)

Aplicación local y sin conexión para organizar pendientes en notas tipo corcho.

## Funciones
- Pizarra de notas adhesivas de seis colores.
- Crear y editar título, detalles, categoría y prioridad.
- Fecha límite opcional.
- Marcar como terminada: sale de la pizarra y pasa al historial con fecha y hora.
- Recuperar una tarea terminada para devolverla a pendientes.
- Eliminar notas o elementos del historial.
- Persistencia local mediante SharedPreferences/JSON; no requiere cuenta ni Internet.

## Generar APK
Se requiere Android Studio (JDK 17 y Android SDK 35) o un entorno CI con Android SDK.

1. Abre esta carpeta en Android Studio.
2. Espera la sincronización de Gradle.
3. Ejecuta **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. El APK de depuración quedará en `app/build/outputs/apk/debug/app-debug.apk`.

También se incluye el workflow `.github/workflows/build-apk.yml`: súbelo a un repositorio GitHub y ejecuta **Actions > Construir APK > Run workflow**. Descarga el artefacto `Mi-Pizarra-APK` al terminar.

El APK de depuración se instala en Android habilitando la instalación desde esa fuente. Para distribuir una versión pública, genera un APK firmado de lanzamiento desde Android Studio.
