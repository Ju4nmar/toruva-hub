package com.nexus.gamelauncher.ui

import android.graphics.PathMeasure
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.nexus.gamelauncher.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.random.Random

internal const val INTRO_MS = 2500

private data class BrandLayer(val path: Path, val tint: Color, val stroke: Float, val phase: Int)
private data class Spark(val target: Offset, val origin: Offset, val tint: Color, val phase: Int, val lag: Float)

private fun brandLayers() = listOf(
    BrandLayer(PathParser().parsePathString("M515,145 C720,38 917,-17 1007,14 C1061,33 1060,78 1012,130 C1021,93 1015,64 976,50 C902,21 733,70 515,145 Z M638,438 C513,501 369,554 289,532 C244,520 248,480 294,422 C273,464 298,490 353,495 C427,505 526,480 638,438 Z").toPath().apply { fillType = PathFillType.EvenOdd }, Color(0xFF327AFF), 0f, 0),
    BrandLayer(PathParser().parsePathString("M150,135 H285 Q304,135 322,154 Q333,164 346,164 H952 Q965,164 978,151 Q989,139 1007,139 H1057 Q1148,140 1188,215 L1219,282 Q1235,315 1216,339 L1182,383 Q1167,405 1135,405 H1046 Q1027,405 1010,422 Q992,443 968,443 H677 Q651,443 632,422 L594,387 Q586,381 574,381 H333 M145,410 H70 Q48,410 33,391 L15,366 Q5,349 18,323 L65,222 Q101,145 150,135").toPath().apply { fillType = PathFillType.EvenOdd }, Color(0xFF51DBFF), 17f, 0),
    BrandLayer(PathParser().parsePathString("M172,428 L220,374 H254 L203,428 Z M228,428 L275,374 H310 L259,428 Z M284,428 L331,374 H366 L315,428 Z").toPath().apply { fillType = PathFillType.EvenOdd }, Color(0xFF367CFF), 0f, 0),
    BrandLayer(PathParser().parsePathString("M128,221 H166 V255 H201 V292 H166 V326 H128 V292 H93 V255 H128 Z").toPath().apply { fillType = PathFillType.EvenOdd }, Color(0xFF22CFFF), 8f, 1),
    BrandLayer(PathParser().parsePathString("M235,217 H351 L345,240 H303 L282,313 H256 L277,240 H229 Z M393,217 H444 Q490,217 481,252 L474,279 Q465,313 423,313 H371 Q330,313 340,279 L348,251 Q358,217 393,217 Z M392,240 Q375,240 371,257 L365,278 Q362,290 377,290 H424 Q442,290 446,274 L451,254 Q455,240 441,240 Z M511,217 H594 Q627,217 621,243 Q615,272 586,280 L612,313 H580 L554,283 H522 L514,313 H487 Z M530,239 L527,261 H578 Q591,261 595,248 Q598,239 584,239 Z M648,217 H674 L656,277 Q652,291 669,291 H704 Q722,291 726,275 L743,217 H770 L753,278 Q744,313 703,313 H660 Q620,313 629,280 Z M779,217 H808 L835,283 L895,217 H926 L840,313 H813 Z M875,313 L953,223 Q963,215 981,217 L1017,313 H989 L962,242 L906,313 Z").toPath().apply { fillType = PathFillType.EvenOdd }, Color(0xFFF4F6FF), 0f, 2),
    BrandLayer(PathParser().parsePathString("M701,341 H718 L712,367 H752 L759,341 H776 L762,401 H745 L751,378 H709 L704,401 H687 Z M797,341 H814 L804,382 Q803,390 813,390 H837 Q846,390 848,382 L858,341 H875 L865,381 Q860,401 837,401 H806 Q783,401 788,383 Z M895,341 H943 Q967,341 961,356 Q959,367 949,369 Q963,372 959,385 Q955,401 933,401 H880 Z M907,352 L904,365 H935 Q945,365 946,359 Q948,352 938,352 Z M901,375 L898,389 H931 Q941,389 943,382 Q945,375 934,375 Z").toPath().apply { fillType = PathFillType.EvenOdd }, Color(0xFFAA49FA), 0f, 2),
    BrandLayer(PathParser().parsePathString("M1086,225 a21,21 0,1 0,42 0 a21,21 0,1 0,-42 0 Z").toPath().apply { fillType = PathFillType.EvenOdd }, Color(0xFFFFCC38), 0f, 1),
    BrandLayer(PathParser().parsePathString("M1044,267 a21,21 0,1 0,42 0 a21,21 0,1 0,-42 0 Z").toPath().apply { fillType = PathFillType.EvenOdd }, Color(0xFF1CB9FF), 0f, 1),
    BrandLayer(PathParser().parsePathString("M1128,267 a21,21 0,1 0,42 0 a21,21 0,1 0,-42 0 Z").toPath().apply { fillType = PathFillType.EvenOdd }, Color(0xFFFF3154), 0f, 1),
    BrandLayer(PathParser().parsePathString("M1086,308 a21,21 0,1 0,42 0 a21,21 0,1 0,-42 0 Z").toPath().apply { fillType = PathFillType.EvenOdd }, Color(0xFF31EC63), 0f, 1)
)

private fun sparks(layers: List<BrandLayer>): List<Spark> {
    val random = Random(217)
    val result = mutableListOf<Spark>()
    val coords = FloatArray(2)
    layers.forEachIndexed { index, layer ->
        val measure = PathMeasure(layer.path.asAndroidPath(), false)
        var contourLength: Float
        do {
            contourLength = measure.length
            val count = if (contourLength > 0f) (contourLength / 22f).toInt().coerceIn(3, 110) else 0
            repeat(count) { point ->
                if (measure.getPosTan(contourLength * point / count, coords, null)) {
                    val target = Offset(coords[0], coords[1])
                    val origin = Offset(random.nextFloat() * 1540f - 150f, random.nextFloat() * 820f - 140f)
                    val tint = if (index >= 6) layer.tint else if (target.x < 600f) Color(0xFF45CAFF) else Color(0xFFD364FF)
                    result += Spark(target, origin, tint, layer.phase, random.nextFloat() * .15f)
                }
            }
        } while (measure.nextContour())
    }
    return result
}

private fun phase(time: Float, start: Float, end: Float) = ((time - start) / (end - start)).coerceIn(0f, 1f)

@Composable
internal fun ToruvaIntro(prepare: suspend () -> Unit, play: () -> Unit, finished: () -> Unit) {
    val startupBackground = colorResource(R.color.toruva_icon_background)
    val time = remember { mutableFloatStateOf(0f) }
    val layers = remember { brandLayers() }
    val particles = remember(layers) { sparks(layers) }
    val share = remember { PathMeasure(layers[1].path.asAndroidPath(), false) }
    val contour = remember { android.graphics.Path() }
    val neon = remember { Brush.horizontalGradient(listOf(Color(0xFF45EEFF), Color(0xFF275AFF), Color(0xFFE72CFF)), 0f, 1240f) }
    LaunchedEffect(Unit) {
        prepare()
        val start = withFrameNanos { now ->
            play()
            now
        }
        do {
            val now = withFrameNanos { it }
            time.floatValue = ((now - start) / (INTRO_MS * 1_000_000f)).coerceAtMost(1f)
        } while (time.floatValue < 1f)
        finished()
    }

    Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Encendido TORUVA HUB" }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) awaitPointerEvent().changes.forEach { it.consume() }
        }
    }) {
        val t = time.floatValue
        val opacity = 1f - phase(t, .89f, 1f)
        drawRect(startupBackground, alpha = opacity)
        val scale = minOf(size.width * .83f / 1240f, size.height * .82f / 540f)
        withTransform({
            translate((size.width - 1240f * scale) / 2, (size.height - 540f * scale) / 2)
            scale(scale, scale, Offset.Zero)
        }) {
            particles.forEachIndexed { index, particle ->
                val start = particle.phase * .08f + particle.lag
                val stop = .44f + particle.phase * .10f
                val progress = phase(t, start, stop)
                val eased = progress * progress * (3f - 2f * progress)
                val point = particle.origin + (particle.target - particle.origin) * eased
                val alpha = (1f - phase(t, stop, stop + .14f)) * opacity
                if (alpha > 0f) drawCircle(particle.tint, if (index % 5 == 0) 4.2f else 2.7f, point, alpha)
            }
            val pulse = 1f - kotlin.math.abs(phase(t, .77f, .89f) * 2f - 1f)
            layers.forEachIndexed { index, layer ->
                val start = .36f + .1f * layer.phase
                val alpha = phase(t, start, start + .13f) * opacity
                if (alpha > 0f) {
                    val style = if (layer.stroke > 0f) Stroke(layer.stroke, cap = StrokeCap.Round) else androidx.compose.ui.graphics.drawscope.Fill
                    if (index in 0..1) drawPath(layer.path, neon, alpha = alpha, style = style)
                    else drawPath(layer.path, layer.tint, alpha = alpha, style = style)
                    if (pulse > 0f && (index == 1 || index >= 6)) {
                        drawPath(layer.path, layer.tint, alpha = pulse * opacity * .45f, style = Stroke(if (index == 1) 28f else 9f))
                    }
                }
            }
            if (t in .62f..0.78f) {
                val pass = phase(t, .62f, .78f)
                contour.reset()
                if (share.getSegment(share.length * (pass - .15f).coerceAtLeast(0f), share.length * pass, contour, true)) {
                    drawPath(contour.asComposePath(), Color.White, alpha = opacity, style = Stroke(7f, cap = StrokeCap.Round))
                }
            }
        }
    }
}
