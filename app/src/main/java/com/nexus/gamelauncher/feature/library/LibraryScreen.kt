package com.nexus.gamelauncher.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.nexus.gamelauncher.ui.*

@Composable
fun LibraryScreen(
    state: LibraryState,
    model: LibraryViewModel,
    openPicker: Boolean,
    pickerOpened: () -> Unit,
    details: (InstalledApp) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(openPicker) {
        if (openPicker) {
            showPicker = true
            pickerOpened()
        }
    }
    val games = state.searchGames(query, favoritesOnly)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PageHeading("Biblioteca", "${state.games.size} en la biblioteca", Modifier.weight(1f))
                Button(onClick = { showPicker = true }) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar juego")
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = !favoritesOnly, onClick = { favoritesOnly = false }, label = { Text("Todos") })
                FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = true }, label = { Text("Favoritos") })
                SearchField(query, { query = it }, "Buscar juegos", Modifier.weight(1f))
            }
        }
        if (state.discovery.loading) item(span = { GridItemSpan(maxLineSpan) }) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (state.discovery.failed) item(span = { GridItemSpan(maxLineSpan) }) {
            EmptyPanel(Icons.Outlined.WarningAmber, "No se pudo actualizar la biblioteca",
                "Vuelve a intentar la búsqueda de aplicaciones.") {
                OutlinedButton(onClick = model::refresh) { Text("Reintentar") }
            }
        }
        if (games.isEmpty() && !state.discovery.loading && !state.discovery.failed) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val (title, detail) = when {
                    query.isNotBlank() -> "Sin resultados" to "Prueba con otro nombre."
                    favoritesOnly -> "Sin favoritos" to "Marca con la estrella los juegos que quieras destacar."
                    else -> "Tu biblioteca está vacía" to "Usa Agregar juego para elegir entre las aplicaciones instaladas."
                }
                EmptyPanel(Icons.Outlined.CollectionsBookmark, title, detail)
            }
        }
        items(games, key = InstalledApp::packageName) { app ->
            CoverCard(
                app, state, model,
                onClick = {
                    selectedPackage = app.packageName
                    details(app)
                },
                modifier = Modifier.aspectRatio(2f / 3f),
                selected = selectedPackage == app.packageName
            )
        }
    }

    if (showPicker) AddGameDialog(state, model, onDismiss = { showPicker = false })
}

@Composable
private fun SearchField(query: String, change: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = change,
        modifier = modifier,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun AddGameDialog(state: LibraryState, model: LibraryViewModel, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val candidates = state.addCandidates(query)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout).imePadding().padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().fillMaxHeight(),
                color = Panel,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Agregar juego", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Cerrar selector") }
                    }
                    SearchField(query, { query = it }, "Buscar aplicaciones instaladas", Modifier.fillMaxWidth())
                    if (state.discovery.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 6.dp)) {
                        if (state.discovery.failed) item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("No se pudo actualizar la lista.", Modifier.weight(1f))
                                TextButton(onClick = model::refresh) { Text("Reintentar") }
                            }
                        }
                        if (candidates.isEmpty() && !state.discovery.loading && !state.discovery.failed) item {
                            Text(
                                if (query.isBlank()) "No quedan aplicaciones disponibles para agregar."
                                else "Ninguna aplicación coincide con la búsqueda.",
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Muted
                            )
                        }
                        items(candidates, key = InstalledApp::packageName) { app ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 58.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppArtwork(app, model, Modifier.size(38.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    app.emulator?.let {
                                        Text("Emulador · ${it.displayName}", color = Muted,
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                TextButton(onClick = { model.setIncluded(app, true) }) { Text("Agregar") }
                            }
                            HorizontalDivider(color = Outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppArtwork(app: InstalledApp, model: LibraryViewModel, modifier: Modifier = Modifier) {
    val icon by produceState<android.graphics.drawable.Drawable?>(null, app) {
        value = model.icon(app.packageName)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (icon == null) {
            Icon(if (app.isGame) Icons.Outlined.SportsEsports else Icons.Outlined.Apps, null,
                Modifier.fillMaxSize(.65f), tint = MaterialTheme.colorScheme.primary)
        } else {
            AsyncImage(model = icon, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}
