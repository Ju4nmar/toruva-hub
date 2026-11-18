package com.nexus.gamelauncher.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.gamelauncher.ui.Muted

@Composable
fun GameDetailsScreen(packageName: String, state: LibraryState, model: LibraryViewModel, back: () -> Unit) {
    val app = state.games.firstOrNull { it.packageName == packageName }
    if (app == null) {
        Column(Modifier.padding(20.dp)) {
            Text(if (state.discovery.loading) "Cargando juego…" else "Este juego ya no está disponible.")
            TextButton(onClick = back) { Text("Volver") }
        }
        return
    }
    val entry = state.entries[packageName]
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.changeArtwork(app, ArtworkKind.Cover, uri)
    }
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.changeArtwork(app, ArtworkKind.Background, uri)
    }
    var nameDialog by rememberSaveable(packageName) { mutableStateOf(false) }
    var editingArtwork by rememberSaveable(packageName) { mutableStateOf<ArtworkKind?>(null) }
    var confirmRemoval by rememberSaveable(packageName) { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") }
            Text("Detalles", style = MaterialTheme.typography.titleMedium)
        }
        Box(
            Modifier.fillMaxWidth().heightIn(min = 174.dp).clip(RoundedCornerShape(18.dp))
        ) {
            GameBackdrop(app, entry, model, Modifier.matchParentSize())
            Row(
                Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                GameCover(app, entry, model, Modifier.width(94.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(9.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.title(app), style = MaterialTheme.typography.headlineMedium, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Text(if (entry?.displayName != null) "Nombre original · ${app.name}" else app.packageName,
                        color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { model.launch(app) }) { Text("Jugar") }
                        val favorite = state.isFavorite(app)
                        OutlinedButton(onClick = { model.toggleFavorite(app) }) {
                            Icon(if (favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (favorite) "Quitar favorito" else "Marcar favorito")
                        }
                    }
                }
            }
        }
        Text("Personalizar", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { nameDialog = true }) { Text("Nombre visible") }
            OutlinedButton(onClick = { coverPicker.launch(arrayOf("image/*")) }) {
                Text(if (entry?.coverFile == null) "Elegir carátula" else "Cambiar carátula")
            }
            OutlinedButton(onClick = { backgroundPicker.launch(arrayOf("image/*")) }) {
                Text(if (entry?.backgroundFile == null) "Elegir fondo" else "Cambiar fondo")
            }
        }
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (entry?.coverFile != null) TextButton(onClick = { editingArtwork = ArtworkKind.Cover }) {
                Text("Ajustar carátula")
            }
            if (entry?.backgroundFile != null) TextButton(onClick = { editingArtwork = ArtworkKind.Background }) {
                Text("Ajustar fondo")
            }
            if (entry?.coverFile != null) TextButton(onClick = { model.changeArtwork(app, ArtworkKind.Cover) }) {
                Text("Quitar carátula")
            }
            if (entry?.backgroundFile != null) TextButton(onClick = { model.changeArtwork(app, ArtworkKind.Background) }) {
                Text("Quitar fondo")
            }
            if (entry?.coverFile != null || entry?.backgroundFile != null) {
                TextButton(onClick = { model.changeArtwork(app, null) }) { Text("Restablecer imágenes") }
            }
            TextButton(onClick = { confirmRemoval = true }) {
                Text("Quitar de la biblioteca", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    editingArtwork?.let { kind ->
        val (name, frame) = if (kind == ArtworkKind.Cover) entry?.coverFile to entry?.coverFrame
            else entry?.backgroundFile to entry?.backgroundFrame
        val file = remember(name) { model.artworkFile(name) }
        if (file != null) ArtworkEditor(file, kind, frame ?: ArtworkFrame(), dismiss = { editingArtwork = null }) {
            model.setArtworkFrame(app, kind, it)
            editingArtwork = null
        }
    }
    if (nameDialog) {
        var name by rememberSaveable(packageName) { mutableStateOf(entry?.displayName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { nameDialog = false },
            title = { Text("Nombre visible") },
            text = {
                OutlinedTextField(name, { name = it.take(80) }, singleLine = true,
                    label = { Text("Nombre personalizado") },
                    supportingText = { Text("Déjalo vacío para usar ${app.name}.") })
            },
            confirmButton = {
                TextButton(onClick = { model.setDisplayName(app, name); nameDialog = false }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { nameDialog = false }) { Text("Cancelar") } }
        )
    }
    if (confirmRemoval) {
        AlertDialog(
            onDismissRequest = { confirmRemoval = false },
            title = { Text("¿Quitar de la biblioteca?") },
            text = { Text("${state.title(app)} dejará de aparecer en NEXUS. Podrás agregarlo de nuevo.") },
            confirmButton = {
                TextButton(onClick = { model.setIncluded(app, false); confirmRemoval = false; back() }) { Text("Quitar") }
            },
            dismissButton = { TextButton(onClick = { confirmRemoval = false }) { Text("Cancelar") } }
        )
    }
}
