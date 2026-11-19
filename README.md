# TORUVA HUB

Launcher Android en horizontal y modo inmersivo. La versión **0.3.0** incorpora la nueva identidad, encendido con partículas y efectos de sonido sobre la base de NEXUS 0.2.1.

## Funcionalidades

- **Inicio** reúne favoritos y recientes sin duplicados en un carrusel con centrado automático. El juego centrado determina el título, el fondo y las acciones **Jugar** y **Detalles**.
- **Biblioteca** muestra juegos reconocidos por Android y juegos agregados expresamente. Incluye carátulas, búsqueda por nombre original o visible, favoritos y un selector para agregar juegos. Los emuladores conocidos se separan de la detección automática de juegos.
- **Detalles** permite abrir un juego, editar su nombre visible y administrar su carátula y fondo. En modo **Personalizado**, arrastra o pellizca la vista previa. También puedes usar las barras. **Guardar** conserva el encuadre y **Restablecer ajuste** vuelve a **Rellenar**. **Ajustar completo** conserva la imagen entera.
- **Personalización** incluye seis temas. El primer fotograma de la interfaz usa el tema guardado. Personalización incluye interruptores independientes para efectos y encendido, y un control de volumen. Consulta el [inventario de audio](AUDIO.md).
- Room guarda biblioteca, historial y encuadres. DataStore guarda el tema. NEXUS importa copias privadas de las imágenes elegidas en el selector de Android.

## Compilar en Windows

Usa JDK 17 y Android SDK 34. Configura la ruta de tu SDK en `local.properties`. El proyecto usa Kotlin 2.1.21, AGP 8.7.2 y el Gradle Wrapper 8.9.

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

APK de depuración en `app/build/outputs/apk/debug/app-debug.apk`. Para ejecutar las pruebas instrumentadas, usa `.\gradlew.bat connectedDebugAndroidTest` con un dispositivo o emulador de pruebas conectado.

## Validación de la fase 2

El usuario reportó **11 de 11 pruebas manuales satisfactorias** para NEXUS 0.2.1 en el **Xiaomi Redmi Note 13 Pro 4G**, antes del editor táctil. Las comprobaciones específicas del nuevo editor están en las [notas de versión](CHANGELOG.md). El [backlog](BACKLOG.md) conserva el registro del encendido.
