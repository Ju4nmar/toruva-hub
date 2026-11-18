package com.nexus.gamelauncher.feature.library

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.nexus.gamelauncher.ui.Night
import com.nexus.gamelauncher.ui.Panel
import java.io.File

internal data class ArtworkDecode(val width: Int, val height: Int)

internal val HomeBackdropDecode = ArtworkDecode(1280, 640)

internal fun artworkRequest(context: Context, file: File, decode: ArtworkDecode): ImageRequest =
    ImageRequest.Builder(context)
        .data(file)
        .size(decode.width, decode.height)
        .scale(Scale.FIT)
        .precision(Precision.INEXACT)
        .build()

@Composable
internal fun FramedArtwork(
    file: File,
    frame: ArtworkFrame,
    modifier: Modifier = Modifier,
    decode: ArtworkDecode? = null,
    onGesture: ((ArtworkGesture) -> Unit)? = null,
    onError: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val tint = androidx.compose.material3.MaterialTheme.colorScheme.secondary

    BoxWithConstraints(modifier.background(Brush.linearGradient(listOf(Panel, tint.copy(alpha = .19f), Night)))) {
        val target = decode ?: with(density) {
            ArtworkDecode(maxWidth.roundToPx().coerceIn(1, 1440), maxHeight.roundToPx().coerceIn(1, 1440))
        }
        val request = remember(context, file, target) { artworkRequest(context, file, target) }
        val painter = rememberAsyncImagePainter(model = request, onError = { onError() })
        val transform = rememberUpdatedState(onGesture)
        val input = if (onGesture == null) Modifier else Modifier.pointerInput(file, painter) {
            detectTransformGestures(panZoomLock = true) { centroid, pan, zoom, _ ->
                val image = painter.intrinsicSize
                if (image.isSpecified && image.width > 0f && image.height > 0f && size.width > 0 && size.height > 0) {
                    transform.value?.invoke(ArtworkGesture(image, Size(size.width.toFloat(), size.height.toFloat()),
                        centroid, pan, zoom))
                }
            }
        }
        Canvas(Modifier.fillMaxSize().then(input)) {
            val intrinsic = painter.intrinsicSize
            if (intrinsic.isSpecified && intrinsic.width > 0f && intrinsic.height > 0f) {
                val placement = frame.place(intrinsic.width, intrinsic.height, size.width, size.height)
                clipRect {
                    translate(placement.left, placement.top) {
                        with(painter) { draw(Size(placement.width, placement.height)) }
                    }
                }
            }
        }
    }
}
