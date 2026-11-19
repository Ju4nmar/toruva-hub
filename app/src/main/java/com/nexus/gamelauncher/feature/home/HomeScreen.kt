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
import androidx.compose.ui.platform.LocalDensity
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
    details: (InstalledApp) -> Unit,
    moveSound: () -> Unit
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
                QuickAccessCarousel(state, model, games, active, openLibrary = { openLibrary(false) }, details, moveSound)
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
    details: (InstalledApp) -> Unit,
    moveSound: () -> Unit
) {
    val restoredIndex = remember(games, active.packageName) { games.indexOf(active).coerceAtLeast(0) }
    val pager = rememberPagerState(initialPage = restoredIndex) { games.size }
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }
    val currentGames by rememberUpdatedState(games)
    val libraryState by rememberUpdatedState(state)
    val onMove by rememberUpdatedState(moveSound)
    val dragging by pager.interactionSource.collectIsDraggedAsState()
    val context = LocalContext.current
    val loader = context.imageLoader

    LaunchedEffect(pager) {
        // The package recorded after the last snap takes precedence over a restored scroll offset.
        pager.scrollToPage(restoredIndex)
        ready = true
        var last = currentGames.getOrNull(restoredIndex)?.packageName
        snapshotFlow { pager.centered(currentGames)?.packageName }
            .filterNotNull()
            .collect { packageName ->
                if (packageName != last) onMove()
                last = packageName
                if (model.homeSelection.value != packageName) model.selectForHome(packageName)
            }
    }

    val confirmed = remember(pager) {
        derivedStateOf { if (ready) pager.centered(currentGames) else null }
    }
    val fallback = rememberUpdatedState(active)
    val background = remember(pager) {
        derivedStateOf { if (ready) currentGames.getOrNull(pager.settledPage) ?: fallback.value else fallback.value }
    }
    val displayed = remember(pager) {
        derivedStateOf {
            val focused = if (ready) visuallyFocusedGame(
                currentGames, pager.currentPage, pager.targetPage, pager.currentPageOffsetFraction, dragging
            ) else null
            focused ?: confirmed.value ?: background.value
        }
    }

    LaunchedEffect(pager, loader, context) {
        snapshotFlow {
            val current = pager.centered(currentGames)
            if (!ready || current == null) null
            else {
                val index = pager.settledPage
                val adjacent = currentGames.getOrNull(index + 1) ?: currentGames.getOrNull(index - 1)
                adjacent?.let { libraryState.entries[it.packageName]?.backgroundFile }
            }
        }.collectLatest { name ->
            model.artworkFile(name)?.let { loader.execute(artworkRequest(context, it, HomeBackdropDecode)) }
        }
    }

    CarouselFeature(state, model, games, displayed, background, confirmed, pager, details)

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
        val decode = with(LocalDensity.current) {
            ArtworkDecode(cardWidth.roundToPx().coerceIn(1, 480), 82.dp.roundToPx().coerceIn(1, 256))
        }
        HorizontalPager(
            state = pager,
            pageSize = PageSize.Fixed(cardWidth),
            contentPadding = PaddingValues(horizontal = inset, vertical = 5.dp),
            pageSpacing = 12.dp,
            userScrollEnabled = games.size > 1,
            key = { games[it].packageName }
        ) { page ->
            val game = games[page]
            CarouselCover(
                game, state.title(game), state.isFavorite(game), state.entries[game.packageName],
                model, displayed, decode
            ) {
                if (pager.centered(currentGames)?.packageName != game.packageName) {
                    scope.launch {
                        val target = currentGames.indexOfFirst { it.packageName == game.packageName }
                        if (target >= 0) pager.animateScrollToPage(target, animationSpec = tween(175))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CarouselFeature(
    state: LibraryState,
    model: LibraryViewModel,
    games: List<InstalledApp>,
    displayed: State<InstalledApp>,
    background: State<InstalledApp>,
    confirmed: State<InstalledApp?>,
    pager: PagerState,
    details: (InstalledApp) -> Unit
) {
    val active by displayed
    FeaturedGame(
        state, model, active, background.value, games.indexOf(active), games.size,
        actionsEnabled = confirmed.value?.packageName == active.packageName,
        play = { runForActiveGame(active, pager.centered(games), model::launch) },
        details = { runForActiveGame(active, pager.centered(games), details) }
    )
}

@Composable
private fun CarouselCover(
    game: InstalledApp,
    title: String,
    favorite: Boolean,
    entry: LibraryEntry?,
    model: LibraryViewModel,
    displayed: State<InstalledApp>,
    decode: ArtworkDecode,
    onClick: () -> Unit
) {
    val selected by remember(game.packageName, displayed) {
        derivedStateOf { game.packageName == displayed.value.packageName }
    }
    val emphasis by animateFloatAsState(if (selected) 1f else .88f, tween(170), label = "card emphasis")
    CoverCard(
        game, title, favorite, entry, model, onClick,
        Modifier.fillMaxWidth().height(82.dp).graphicsLayer {
            scaleX = emphasis
            scaleY = emphasis
            alpha = if (selected) 1f else .72f
        },
        selected = selected, compact = true, decode = decode
    )
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
