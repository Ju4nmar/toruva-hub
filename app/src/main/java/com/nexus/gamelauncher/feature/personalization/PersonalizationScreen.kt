package com.nexus.gamelauncher.feature.personalization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.contentDescription
import com.nexus.gamelauncher.audio.AudioSettings
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.gamelauncher.ui.*

@Composable
fun PersonalizationScreen(theme: NexusTheme, choose: (NexusTheme) -> Unit, audio: AudioSettings?, update: (AudioSettings) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PageHeading("Personalización", "Temas y sonido")
        }
        if (audio != null) item(span = { GridItemSpan(maxLineSpan) }, key = "audio") {
            AudioControls(audio, update)
        }
        items(NexusTheme.entries, key = { it.name }) { option ->
            val (accent, glow) = themeColors(option, null)
            Surface(
                onClick = { choose(option) },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(if (theme == option) 2.dp else 1.dp, if (theme == option) accent else Outline),
                modifier = Modifier.height(86.dp).semantics { selected = theme == option }
            ) {
                Box {
                    Atmosphere(accent, glow, Modifier.matchParentSize())
                    Text(option.label, Modifier.align(Alignment.BottomStart).padding(12.dp), fontWeight = FontWeight.Bold)
                    if (theme == option) Icon(Icons.Outlined.CheckCircle, null,
                        Modifier.align(Alignment.TopEnd).padding(8.dp), tint = accent)
                }
            }
        }
    }
}

@Composable
private fun AudioControls(audio: AudioSettings, update: (AudioSettings) -> Unit) {
    var volume by remember(audio.volume) { mutableFloatStateOf(audio.volume.toFloat()) }
    Surface(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Outline)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text("Sonido", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("Efectos de sonido", Modifier.weight(1f))
                    Switch(audio.effects, { update(audio.copy(effects = it)) },
                        modifier = Modifier.semantics { contentDescription = "Efectos de sonido" })
                }
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sonido de encendido", Modifier.weight(1f))
                    Switch(audio.startup, { update(audio.copy(startup = it)) },
                        modifier = Modifier.semantics { contentDescription = "Sonido de encendido" })
                }
            }
            Text("Volumen · ${volume.roundToInt()} %")
            Slider(volume, { volume = it }, valueRange = 0f..100f,
                onValueChangeFinished = { update(audio.copy(volume = volume.roundToInt())) },
                modifier = Modifier.semantics { contentDescription = "Volumen de efectos" })
        }
    }
}
