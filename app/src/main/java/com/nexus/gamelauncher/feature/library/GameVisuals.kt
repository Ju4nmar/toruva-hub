package com.nexus.gamelauncher.feature.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.gamelauncher.ui.*

@Composable
internal fun GameCover(app: InstalledApp, entry: LibraryEntry?, model: LibraryViewModel, modifier: Modifier = Modifier, decode: ArtworkDecode? = null) {
    var failed by remember(entry?.coverFile) { mutableStateOf(false) }
    val image = remember(entry?.coverFile) { model.artworkFile(entry?.coverFile) }
    Box(modifier.background(Panel), contentAlignment = Alignment.Center) {
        if (image != null && !failed) {
            FramedArtwork(image, entry?.coverFrame ?: ArtworkFrame(), Modifier.matchParentSize(), decode, onError = { failed = true })
        } else {
            Atmosphere(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, Modifier.matchParentSize())
            AppArtwork(app, model, Modifier.fillMaxWidth(.62f).aspectRatio(1f))
        }
    }
}

@Composable
internal fun GameBackdrop(
    app: InstalledApp?,
    entry: LibraryEntry?,
    model: LibraryViewModel,
    modifier: Modifier = Modifier,
    decode: ArtworkDecode? = null
) {
    var failed by remember(entry?.backgroundFile) { mutableStateOf(false) }
    val image = remember(entry?.backgroundFile) { model.artworkFile(entry?.backgroundFile) }
    Box(modifier.background(Night)) {
        if (image != null && !failed) {
            FramedArtwork(image, entry?.backgroundFrame ?: ArtworkFrame(), Modifier.matchParentSize(), decode, onError = { failed = true })
        } else {
            Atmosphere(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, Modifier.matchParentSize())
            if (app != null) {
                AppArtwork(app, model,
                    Modifier.align(Alignment.CenterEnd).padding(end = 48.dp).fillMaxHeight(.74f).aspectRatio(1f))
            }
        }
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            listOf(Night.copy(alpha = .98f), Night.copy(alpha = .86f), Color.Transparent)
        )))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            listOf(Color.Transparent, Night.copy(alpha = .20f), Night.copy(alpha = .88f))
        )))
    }
}

@Composable
fun CoverCard(
    app: InstalledApp,
    state: LibraryState,
    model: LibraryViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    compact: Boolean = false
) {
    CoverCard(app, state.title(app), state.isFavorite(app), state.entries[app.packageName], model,
        onClick, modifier, selected, compact)
}

@Composable
internal fun CoverCard(
    app: InstalledApp,
    title: String,
    favorite: Boolean,
    entry: LibraryEntry?,
    model: LibraryViewModel,
    onClick: () -> Unit,
    modifier: Modifier,
    selected: Boolean,
    compact: Boolean,
    decode: ArtworkDecode? = null
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.035f else 1f, tween(150), label = "focus")
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(13.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .semantics { this.selected = selected },
        shape = shape,
        color = Panel,
        border = BorderStroke(if (focused || selected) 2.dp else 1.dp, if (focused || selected) accent else Outline)
    ) {
        Box(Modifier.clip(shape)) {
            GameCover(app, entry, model, Modifier.matchParentSize(), decode)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                0f to Color.Transparent, .50f to Color.Transparent, 1f to Night
            )))
            Text(
                title,
                Modifier.align(Alignment.BottomStart).padding(horizontal = 8.dp, vertical = 7.dp),
                fontWeight = FontWeight.Bold,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
            if (favorite) {
                Icon(
                    Icons.Outlined.Star, "Favorito",
                    Modifier.align(Alignment.TopEnd).padding(7.dp).size(18.dp),
                    tint = Color(0xFFFFCF76)
                )
            }
        }
    }
}
