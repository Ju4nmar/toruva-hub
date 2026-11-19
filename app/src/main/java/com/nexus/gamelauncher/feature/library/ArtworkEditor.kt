package com.nexus.gamelauncher.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

@Composable
internal fun ArtworkEditor(
    file: File,
    kind: ArtworkKind,
    initial: ArtworkFrame,
    dismiss: () -> Unit,
    save: (ArtworkFrame) -> Unit
) {
    var mode by rememberSaveable(file.path) { mutableStateOf(initial.mode) }
    var zoom by rememberSaveable(file.path) { mutableFloatStateOf(initial.zoom) }
    var x by rememberSaveable(file.path) { mutableFloatStateOf(initial.x) }
    var y by rememberSaveable(file.path) { mutableFloatStateOf(initial.y) }
    val frame = ArtworkFrame(mode, zoom, x, y).normalized()
    val label = if (kind == ArtworkKind.Cover) "Encuadre de carátula" else "Encuadre de fondo"
    val aspect = if (kind == ArtworkKind.Cover) 2f / 3f else 4f

    Dialog(dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(12.dp).widthIn(max = 780.dp).fillMaxWidth().fillMaxHeight(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 6.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BoxWithConstraints(
                        Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        FramedArtwork(
                            file, frame,
                            Modifier.width(minOf(maxWidth, maxHeight * aspect))
                                .aspectRatio(aspect).clip(RoundedCornerShape(10.dp))
                                .semantics { contentDescription = "Vista previa del encuadre" },
                            onGesture = if (frame.fit == ArtworkFit.Custom) ({ gesture ->
                                val updated = ArtworkFrame(mode, zoom, x, y).applyGesture(gesture)
                                zoom = updated.zoom
                                x = updated.x
                                y = updated.y
                            }) else null
                        )
                    }
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        Text("Ajuste", style = MaterialTheme.typography.titleSmall)
                        ArtworkFit.entries.forEach { option ->
                            FilterChip(
                                frame.fit == option,
                                onClick = { mode = option.name },
                                label = { Text(option.label) }
                            )
                        }
                        if (frame.fit == ArtworkFit.Custom) {
                            Text("Arrastra o pellizca la imagen.", style = MaterialTheme.typography.bodySmall)
                            FrameSlider("Zoom", frame.zoom, MIN_ARTWORK_ZOOM..3f) { zoom = it }
                            FrameSlider("Desplazamiento horizontal", frame.x, -1f..1f) { x = it }
                            FrameSlider("Desplazamiento vertical", frame.y, -1f..1f) { y = it }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        mode = ArtworkFit.Fill.name
                        zoom = 1f
                        x = 0f
                        y = 0f
                    }) { Text("Restablecer ajuste") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = dismiss) { Text("Cancelar") }
                    Button(onClick = { save(frame) }) { Text("Guardar") }
                }
            }
        }
    }
}

@Composable
private fun FrameSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, change: (Float) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Slider(value = value, onValueChange = change, valueRange = range, modifier = Modifier.semantics { contentDescription = label })
}
