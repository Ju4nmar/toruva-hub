# Verificación del arranque y del carrusel

## Cambios

El logotipo anticipado provenía de `windowSplashScreenAnimatedIcon` en `Theme.Nexus.Starting`. El splash nativo usa ahora un drawable transparente y el mismo color de fondo que la animación. Compose prepara el encendido mientras DataStore resuelve el tema. El icono adaptable de TORUVA HUB sigue disponible para el lanzador. Android y la capa del fabricante controlan sus propias superficies y transiciones.

En Inicio, el encabezado y cada tarjeta observan los estados de selección que necesitan. El pager conserva sus claves por paquete y el toque vuelve a resolver el índice por esa clave al iniciar el desplazamiento. La acción de abrir un juego sigue requiriendo que el juego mostrado esté centrado. La decodificación de las carátulas del carrusel se calcula una vez según el tamaño de la tarjeta y tiene un límite de 480 × 256 píxeles. Las peticiones Coil mantienen el tamaño estable y los iconos alternativos reutilizan hasta 32 estados de drawable por exploración del catálogo.

## Medición diagnóstica

Diez gestos de 140 ms con ADB, cinco juegos con carátulas de 1000 × 1500 y fondos de 1600 × 800, en el mismo emulador Android 16 con SwiftShader. Se reiniciaron los contadores de `dumpsys gfxinfo` antes de cada recorrido.

| Compilación de depuración | Fotogramas | Mediana | P95 | Fotogramas con jank |
| --- | ---: | ---: | ---: | ---: |
| Base 0.3.0 | 24 | 500 ms | 1.100 ms | 24 |
| Optimizada | 23 | 450 ms | 800 ms | 23 |

La carga y los avisos de System UI del emulador impiden extrapolar estas cifras al Xiaomi o concluir una mejora cuantitativa. La validación física debe comprobar el gesto repetido, el cambio de juego con arte cargado, el retorno desde segundo plano y la secuencia de encendido con las animaciones del sistema activadas y desactivadas.
