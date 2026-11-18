package com.nexus.gamelauncher.feature.library

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

internal data class ArtworkGesture(
    val image: Size,
    val viewport: Size,
    val centroid: Offset,
    val pan: Offset,
    val zoomChange: Float
)

internal fun ArtworkFrame.applyGesture(gesture: ArtworkGesture): ArtworkFrame {
    if (fit != ArtworkFit.Custom) return this
    val current = normalized()
    val (image, viewport, centroid, pan, zoomChange) = gesture

    val before = current.place(image.width, image.height, viewport.width, viewport.height)
    val zoom = (current.zoom * zoomChange).coerceIn(1f, 3f)
    val ratio = zoom / current.zoom
    val left = centroid.x + (before.left - centroid.x) * ratio + pan.x
    val top = centroid.y + (before.top - centroid.y) * ratio + pan.y

    return current.copy(
        zoom = zoom,
        x = relativePan(left, viewport.width, before.width * ratio, current.x),
        y = relativePan(top, viewport.height, before.height * ratio, current.y)
    )
}

private fun relativePan(offset: Float, viewport: Float, image: Float, previous: Float): Float {
    val overflow = (image - viewport) / 2f
    return if (overflow > .001f) ((offset + overflow) / overflow).coerceIn(-1f, 1f) else previous
}
