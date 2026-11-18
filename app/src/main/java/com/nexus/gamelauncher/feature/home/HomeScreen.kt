package com.nexus.gamelauncher.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.gamelauncher.feature.library.*
import com.nexus.gamelauncher.ui.*
import coil.imageLoader
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    state: LibraryState,
    model: LibraryViewModel,
    games: List<InstalledApp>,
    active: InstalledApp?,
    openLibrary: (Boolean) -> Unit,
    details: (InstalledApp) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 10.dp)) {
        if (active == null) {
            EmptyPanel(
                Icons.Outlined.SportsEsports,
                if (state.discovery.loading) "Buscando juegos…" else "Tu biblioteca está vacía",
                "Agrega juegos para tenerlos a mano desde Inicio.",
                modifier = Modifier.padding(horizontal = 18.dp)
            ) {
                if (!state.discovery.loading) Button(onClick = { openLibrary(true) }) { Text("Agregar juego") }
            }
        } else {
            val packages = remember(games) { games.map(InstalledApp::packageName) }
            // Recreation by catalog identity prevents a moved index from becoming the active game.
            key(packages) {
                QuickAccessCarousel(state, model, games, active, openLibrary = { openLibrary(false) }, details)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickAccessCarousel(
    state: LibraryState,
    model: LibraryViewModel,
    games: List<InstalledApp>,
    active: InstalledApp,
    openLibrary: () -> Unit,
    details: (InstalledApp) -> Unit
) {
    val restoredIndex = games.indexOfFirst { it.packageName == active.packageName }
    val pager = rememberPagerState(initialPage = restoredIndex) { games.size }
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }
    val currentGames by rememberUpdatedState(games)
    val libraryState by rememberUpdatedState(state)
    val dragging by pager.interactionSource.collectIsDraggedAsState()
    val context = LocalContext.current
    val loader = context.imageLoader

    LaunchedEffect(pager) {
        // The package recorded after the last snap takes precedence over a restored scroll offset.
        pager.scrollToPage(restoredIndex)
        ready = true
        snapshotFlow { pager.centered(currentGames)?.packageName }
            .filterNotNull()
            .collect { model.selectForHome(it) }
    }

    val confirmed by remember(pager) {
        derivedStateOf { if (ready) pager.centered(currentGames) else null }
    }
    val focused by remember(pager) {
        derivedStateOf {
            if (ready) visuallyFocusedGame(
                currentGames, pager.currentPage, pager.targetPage, pager.currentPageOffsetFraction, dragging
            ) else null
        }
    }
    val background = if (ready) games.getOrNull(pager.settledPage) ?: active else active
    val displayed = focused ?: confirmed ?: background
    val displayedIndex = games.indexOfFirst { it.packageName == displayed.packageName }

    LaunchedEffect(pager, loader, context) {
        snapshotFlow {
            val current = pager.centered(currentGames)
            if (!ready || current == null) null
            else {
                val index = currentGames.indexOf(current)
                val adjacent = currentGames.getOrNull(index + 1) ?: currentGames.getOrNull(index - 1)
                adjacent?.let { libraryState.entries[it.packageName]?.backgroundFile }
            }
        }.collectLatest { name ->
            model.artworkFile(name)?.let { loader.execute(artworkRequest(context, it, HomeBackdropDecode)) }
        }
    }

    FeaturedGame(
        state, model, displayed, background, displayedIndex, games.size,
        actionsEnabled = confirmed?.packageName == displayed.packageName,
        play = { runForActiveGame(displayed, pager.centered(currentGames), model::launch) },
        details = { runForActiveGame(displayed, pager.centered(currentGames), details) }
    )

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Acceso rápido", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        TextButton(onClick = openLibrary) { Text("Ver biblioteca") }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = minOf(160.dp, maxWidth * .56f)
        val inset = (maxWidth - cardWidth) / 2
        HorizontalPager(
            state = pager,
            pageSize = PageSize.Fixed(cardWidth),
            contentPadding = PaddingValues(horizontal = inset, vertical = 5.dp),
            pageSpacing = 12.dp,
            userScrollEnabled = games.size > 1,
            key = { games[it].packageName }
        ) { page ->
            val game = games[page]
            val selected = game.packageName == displayed.packageName
            val emphasis by animateFloatAsState(if (selected) 1f else .88f, tween(170), label = "card emphasis")
            CoverCard(
                game, state, model,
                onClick = {
                    if (pager.centered(currentGames)?.packageName != game.packageName) {
                        scope.launch { pager.animateScrollToPage(page, animationSpec = tween(175)) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(82.dp).graphicsLayer {
                    scaleX = emphasis
                    scaleY = emphasis
                    alpha = if (selected) 1f else .72f
                },
                selected = selected,
                compact = true
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun PagerState.centered(games: List<InstalledApp>) =
    centeredGame(games, currentPage, settledPage, currentPageOffsetFraction, isScrollInProgress)

@Composable
private fun FeaturedGame(
    state: LibraryState,
    model: LibraryViewModel,
    active: InstalledApp,
    background: InstalledApp,
    index: Int,
    count: Int,
    actionsEnabled: Boolean,
    play: () -> Unit,
    details: () -> Unit
) {
    Surface(
        modifier = Modifier.padding(horizontal = 18.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Outline),
        color = Night
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 132.dp)) {
            Crossfade(
                targetState = Triple(background, state.entries[background.packageName]?.backgroundFile,
                    state.entries[background.packageName]?.backgroundFrame),
                modifier = Modifier.matchParentSize(),
                animationSpec = tween(125),
                label = "game background"
            ) { (app, file, frame) ->
                GameBackdrop(
                    app, LibraryEntry(app.packageName, backgroundFile = file, backgroundFrame = frame),
                    model, Modifier.fillMaxSize(), HomeBackdropDecode
                )
            }
            Column(
                Modifier.align(Alignment.CenterStart).fillMaxWidth(.84f).padding(horizontal = 16.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    "${index + 1} de $count",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(state.title(active), style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = play, enabled = actionsEnabled) {
                        Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Jugar")
                    }
                    OutlinedButton(onClick = details, enabled = actionsEnabled) { Text("Detalles") }
                }
            }
        }
    }
}
