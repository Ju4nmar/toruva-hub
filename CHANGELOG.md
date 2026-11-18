# Notas de versión

## 0.2.1 · Cierre de fase 2

- El modo **Personalizado** del editor permite arrastrar con un dedo y pellizcar con dos dedos directamente en la vista previa de carátulas y fondos. El pellizco se centra en los dedos y los límites impiden desplazar la imagen fuera del encuadre.
- Las barras de zoom y desplazamiento, **Restablecer ajuste**, **Rellenar** y **Ajustar completo** siguen disponibles. **Guardar** usa el modelo de encuadre de Room v4 y las imágenes privadas existentes.
- La fase 2 incluye carrusel central de acceso rápido, biblioteca de juegos con búsqueda y favoritos, detalles con arte y nombre visible personalizados, seis temas y arranque con el tema guardado.

### Validación manual reportada

| Versión | Dispositivo | Resultado | Alcance |
| --- | --- | --- | --- |
| 0.2.1 antes del editor táctil | Xiaomi Redmi Note 13 Pro 4G | 11 de 11 satisfactorias | El usuario comunicó el resultado global. El detalle individual de los casos está pendiente de registro. |

### Verificación automatizada

`.gradlew.bat assembleDebug` completó la compilación. Pasaron 21 pruebas unitarias y 7 instrumentadas en el emulador Android 16. Las pruebas del editor enviaron gestos de arrastre y pellizco a la vista previa de cada tipo de imagen y comprobaron Guardar, reabrir y Cancelar. Los dos casos del editor también pasaron con el emulador en horizontal. Las pruebas de Room comprobaron la persistencia del encuadre tras reabrir la base.

### Verificación pendiente en el Xiaomi

1. En una carátula y un fondo panorámico, arrastra y pellizca en ambos sentidos. Comprueba los bordes y los límites de zoom.
2. Guarda un encuadre distinto para cada imagen. Sal de Detalles, reinicia NEXUS y vuelve a abrir ambos editores para comparar el encuadre.
3. Comprueba **Cancelar**, **Restablecer ajuste**, las barras, **Rellenar** y **Ajustar completo**. Verifica el gesto Atrás del sistema y el desplazamiento del panel de controles mientras el diálogo está abierto.
