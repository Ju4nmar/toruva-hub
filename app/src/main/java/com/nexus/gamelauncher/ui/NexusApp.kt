package com.nexus.gamelauncher.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import com.nexus.gamelauncher.audio.AudioSettings
import com.nexus.gamelauncher.audio.Cue
import com.nexus.gamelauncher.audio.ToruvaAudio
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import com.nexus.gamelauncher.R
import com.nexus.gamelauncher.feature.library.*
import com.nexus.gamelauncher.feature.home.HomeScreen
import com.nexus.gamelauncher.feature.home.quickAccess
import com.nexus.gamelauncher.feature.home.restoreActive
import com.nexus.gamelauncher.feature.personalization.*

private enum class Page(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Outlined.Home),
    Library("Biblioteca", Icons.Outlined.CollectionsBookmark),
    Emulators("Emuladores", Icons.Outlined.SportsEsports),
    Stats("Estadísticas", Icons.Outlined.BarChart),
    Themes("Personalización", Icons.Outlined.Tune)
}

val Night = Color(0xFF060C17)
val Panel = Color(0xFF111D31)
val Muted = Color(0xFFB6C4DB)
val Outline = Color(0xFF364864)

fun themeColors(theme: NexusTheme, packageName: String?): Pair<Color, Color> {
    val palette = if (theme == NexusTheme.Dynamic && packageName != null) {
        val choices = listOf(NexusTheme.Dynamic, NexusTheme.Blue, NexusTheme.Purple, NexusTheme.Retro)
        choices[Math.floorMod(packageName.hashCode(), choices.size)]
    } else theme
    return Color(palette.accent) to Color(palette.glow)
}

@Composable
fun NexusApp(library: LibraryViewModel, themes: ThemeViewModel, theme: NexusTheme, audio: ToruvaAudio, sound: AudioSettings?) {
    val state by library.state.collectAsStateWithLifecycle()
    val selection by library.homeSelection.collectAsStateWithLifecycle()
    val quickAccess = remember(state) { state.quickAccess() }
    val selected = quickAccess.restoreActive(selection)
    val palette = remember(theme, selected?.packageName) { themeColors(theme, selected?.packageName) }
    val accent by animateColorAsState(palette.first, label = "accent")
    val glow by animateColorAsState(palette.second, label = "glow")
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val activeRoute = entry?.destination?.route.let { route ->
        if (route == "game/{packageName}") nav.previousBackStackEntry?.destination?.route else route
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(library) { library.feedback.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(themes) { themes.feedback.collect { snackbar.showSnackbar(it) } }

    fun open(page: Page) {
        if (activeRoute != page.name) audio.play(Cue.Tab)
        nav.navigate(page.name) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun details(app: InstalledApp) {
        audio.play(Cue.Select)
        nav.navigate("game/${Uri.encode(app.packageName)}") { launchSingleTop = true }
    }

    fun back() {
        if (nav.popBackStack()) audio.play(Cue.Back)
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = accent, onPrimary = Night, secondary = glow, background = Night,
        surface = Panel, onSurface = Color(0xFFF2F6FF), onSurfaceVariant = Muted, outline = Outline
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = Night) {
            Box(Modifier.fillMaxSize()) {
                Atmosphere(accent, glow, Modifier.matchParentSize())
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
                    BoxWithConstraints(
                        Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF142138), Color(0xFF080E1A))))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        val showBrand = maxWidth >= 660.dp
                        val showLabels = maxWidth >= 520.dp
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (showBrand) {
                                Icon(painterResource(R.drawable.toruva_brand), "TORUVA HUB",
                                    Modifier.width(114.dp).height(42.dp).padding(end = 8.dp), tint = Color.Unspecified)
                            }
                            Page.entries.forEach { page ->
                                val selected = (activeRoute ?: Page.Home.name) == page.name
                                val fill by animateColorAsState(
                                    if (selected) glow.copy(alpha = .22f) else Color.Transparent, label = page.name
                                )
                                val shape = RoundedCornerShape(10.dp)
                                Button(
                                    onClick = { open(page) },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                        .padding(horizontal = 2.dp)
                                        .semantics { this.selected = selected }
                                        .border(1.dp, if (selected) glow.copy(alpha = .7f) else Color.Transparent, shape),
                                    shape = shape,
                                    contentPadding = PaddingValues(horizontal = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = fill, contentColor = if (selected) accent else Muted
                                    )
                                ) {
                                    Icon(page.icon, if (showLabels) null else page.label, Modifier.size(17.dp))
                                    if (showLabels) Text(
                                        page.label, Modifier.padding(start = 4.dp), fontSize = 11.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Outline)
                    NavHost(
                        navController = nav,
                        startDestination = Page.Home.name,
                        modifier = Modifier.weight(1f),
                        enterTransition = { fadeIn(tween(180)) },
                        exitTransition = { fadeOut(tween(120)) },
                        popEnterTransition = { fadeIn(tween(180)) },
                        popExitTransition = { fadeOut(tween(120)) }
                    ) {
                        composable(Page.Home.name) {
                            HomeScreen(state, library, quickAccess, selected, openLibrary = { add ->
                                open(Page.Library)
                                if (add) nav.currentBackStackEntry?.savedStateHandle?.set("showAddGame", true)
                            }, details = ::details, moveSound = { audio.play(Cue.Carousel) })
                        }
                        composable(Page.Library.name) { libraryEntry ->
                            val showPicker by libraryEntry.savedStateHandle.getStateFlow("showAddGame", false)
                                .collectAsStateWithLifecycle()
                            LibraryScreen(state, library, showPicker,
                                pickerOpened = { libraryEntry.savedStateHandle["showAddGame"] = false },
                                details = ::details)
                        }
                        composable("game/{packageName}", arguments = listOf(navArgument("packageName") {
                            type = NavType.StringType
                        })) { detailEntry ->
                            val packageName = detailEntry.arguments?.getString("packageName").orEmpty()
                            GameDetailsScreen(packageName, state, library, back = ::back)
                        }
                        composable(Page.Emulators.name) {
                            Placeholder("Emuladores", "Tu colección de consolas", Icons.Outlined.SportsEsports,
                                "Aún no hay emuladores", "Aquí aparecerán las consolas y juegos que configures.")
                        }
                        composable(Page.Stats.name) {
                            Placeholder("Estadísticas", "Tu actividad de juego", Icons.Outlined.BarChart,
                                "Aún no hay estadísticas", "Aquí podrás consultar tus sesiones y tiempo de juego.")
                        }
                        composable(Page.Themes.name) { PersonalizationScreen(theme, { audio.play(Cue.Select); themes.choose(it) }, sound) { audio.update(it); themes.chooseAudio(it) } }
                    }
                }
                BackHandler(enabled = nav.previousBackStackEntry != null && entry != null, onBack = ::back)
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
fun Atmosphere(accent: Color, glow: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Brush.linearGradient(listOf(Night, Panel, Night)))
        drawRect(Brush.radialGradient(
            listOf(glow.copy(alpha = .40f), Color.Transparent),
            Offset(size.width * .76f, size.height * .31f), size.width.coerceAtLeast(1f) * .64f
        ))
        repeat(5) { i ->
            val shift = i * size.height * .041f
            val wave = Path().apply {
                moveTo(0f, size.height * .84f + shift)
                cubicTo(size.width * .48f, size.height * .94f + shift, size.width * .52f,
                    size.height * .08f + shift, size.width, size.height * .23f + shift)
            }
            drawPath(wave, Brush.horizontalGradient(
                listOf(Color.Transparent, accent.copy(alpha = .15f), glow.copy(alpha = .28f), Color.Transparent)
            ), style = androidx.compose.ui.graphics.drawscope.Stroke((1 + i).dp.toPx()))
        }
    }
}

@Composable
fun PageHeading(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable
fun EmptyPanel(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier,
               action: @Composable () -> Unit = {}) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(Panel.copy(alpha = .82f))
            .border(1.dp, Outline, shape).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, fontWeight = FontWeight.Bold)
        Text(text, color = Muted, style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        action()
    }
}

@Composable
private fun Placeholder(title: String, subtitle: String, icon: ImageVector, message: String, detail: String) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { PageHeading(title, subtitle) }
        item { EmptyPanel(icon, message, detail) }
    }
}
