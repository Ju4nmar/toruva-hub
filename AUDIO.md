# Sonidos de TORUVA HUB

Los seis OGG Vorbis originales se conservan en `app/src/main/res/raw`, en mono a 48 kHz. Verifiqué que el ZIP y la extracción coincidían byte a byte. Las mediciones de nivel corresponden a los WAV PCM de 16 bits del mismo paquete.

| Archivo base | Duración | Pico | RMS | Evento |
| --- | ---: | ---: | ---: | --- |
| startup | 2,50 s | −6,85 dBFS | −19,74 dBFS | Encendido |
| tab_switch | 0,15 s | −12,28 dBFS | −19,74 dBFS | Cambio de pestaña |
| carousel_move | 0,08 s | −11,93 dBFS | −19,74 dBFS | Centrado de un nuevo juego |
| select | 0,20 s | −11,15 dBFS | −19,74 dBFS | Selección |
| launch_game | 0,50 s | −8,53 dBFS | −19,74 dBFS | Apertura de un juego |
| back | 0,15 s | −11,20 dBFS | −19,74 dBFS | Regreso |

SoundPool precarga estos efectos para reducir la latencia. El encendido también cabe en su presupuesto por muestra: 2,5 s de PCM de 16 bits en mono a 48 kHz son unos 240 kB. Se usa una sola voz y un intervalo mínimo de 125 ms entre movimientos de carrusel. El sonido y la animación comparten el fotograma inicial, con el ensamblaje cerca de 1 s, el destello hacia 1,6 s y el pulso final cerca de 2,1 s. Si la muestra de encendido no está lista tras el límite de preparación, se omite ese sonido para mantener la apertura fluida.

Los sonidos del paquete fueron generados por síntesis. Su README autoriza usarlos, modificarlos y redistribuirlos, incluso comercialmente, sin pago ni atribución obligatoria.
