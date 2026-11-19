package com.nexus.gamelauncher.feature.library

import kotlin.math.max
import kotlin.math.min

enum class ArtworkFit(val label: String) {
    Fill("Rellenar"), Fit("Ajustar completo"), Custom("Personalizado")
}

data class ArtworkFrame(
    val mode: String = ArtworkFit.Fill.name,
    val zoom: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f
) {
    val fit: ArtworkFit get() = ArtworkFit.entries.firstOrNull { it.name == mode } ?: ArtworkFit.Fill

    fun normalized() = if (fit == ArtworkFit.Custom) copy(
        mode = fit.name,
        zoom = zoom.finiteOr(1f).coerceIn(MIN_ARTWORK_ZOOM, 3f),
        x = x.finiteOr(0f).coerceIn(-1f, 1f),
        y = y.finiteOr(0f).coerceIn(-1f, 1f)
    ) else ArtworkFrame(fit.name)

    private fun Float.finiteOr(default: Float) = if (isFinite()) this else default
}

internal data class ArtworkPlacement(val width: Float, val height: Float, val left: Float, val top: Float)

internal fun ArtworkFrame.place(sourceWidth: Float, sourceHeight: Float, width: Float, height: Float): ArtworkPlacement {
    val frame = normalized()
    val ratioX = width / sourceWidth
    val ratioY = height / sourceHeight
    val scale = if (frame.fit == ArtworkFit.Fit) min(ratioX, ratioY) else max(ratioX, ratioY) * frame.zoom
    val renderedWidth = sourceWidth * scale
    val renderedHeight = sourceHeight * scale
    val freeX = (width - renderedWidth) / 2
    val freeY = (height - renderedHeight) / 2
    return ArtworkPlacement(
        renderedWidth, renderedHeight,
        freeX + frame.x * kotlin.math.abs(freeX),
        freeY + frame.y * kotlin.math.abs(freeY)
    )
}

internal const val MIN_ARTWORK_ZOOM = .25f
internal const val ARTWORK_MAX_BYTES = 32L * 1024 * 1024
internal fun supportedArtworkDimensions(width: Int, height: Int) =
    width in 1..16384 && height in 1..16384 && width.toLong() * height <= 100_000_000L
