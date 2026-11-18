package com.nexus.gamelauncher.feature.personalization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.gamelauncher.ui.*

@Composable
fun PersonalizationScreen(theme: NexusTheme, choose: (NexusTheme) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PageHeading("Personalización", "Temas visuales")
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
