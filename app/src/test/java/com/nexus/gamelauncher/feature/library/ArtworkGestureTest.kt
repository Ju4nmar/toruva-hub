package com.nexus.gamelauncher.feature.library

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.*
import org.junit.Test

class ArtworkGestureTest {
    private val custom = ArtworkFrame(ArtworkFit.Custom.name)

    @Test
    fun dragsCoverAndBackdropInPixelsWithoutExposingEmptySpace() {
        val cover = custom.applyGesture(gesture(Size(800f, 400f), Size(200f, 300f), pan = Offset(100f, 60f)))
        assertEquals(.5f, cover.x, .0001f)
        assertEquals(0f, cover.y, 0f)
        val background = custom.applyGesture(gesture(Size(400f, 800f), Size(400f, 100f), pan = Offset(70f, -900f)))
        assertEquals(0f, background.x, 0f)
        assertEquals(-1f, background.y, 0f)
        val placement = background.place(400f, 800f, 400f, 100f)
        assertEquals(0f, placement.top + placement.height - 100f, .001f)
    }

    @Test
    fun pinchScalesAroundTheFingersAndKeepsTheSamePointUnderTheCentroid() {
        val image = Size(400f, 200f)
        val viewport = Size(200f, 100f)
        val center = Offset(65f, 35f)
        val zoomed = custom.applyGesture(ArtworkGesture(image, viewport, center, Offset.Zero, 2f))
        val placement = zoomed.place(image.width, image.height, viewport.width, viewport.height)

        assertEquals(2f, zoomed.zoom, 0f)
        assertEquals(-65f, placement.left, .001f)
        assertEquals(-35f, placement.top, .001f)
        val restored = zoomed.applyGesture(ArtworkGesture(image, viewport, center, Offset.Zero, .5f))
        assertEquals(1f, restored.zoom, 0f)
        assertEquals(0f, restored.place(400f, 200f, 200f, 100f).left, 0f)
    }

    @Test
    fun clampsZoomAndIgnoresGesturesInAutomaticModes() {
        val zoomed = custom.applyGesture(gesture(Size(500f, 500f), Size(250f, 250f), zoom = 8f))
        assertEquals(3f, zoomed.zoom, 0f)
        val update = gesture(Size(500f, 500f), Size(250f, 250f), zoom = .01f)
        assertEquals(MIN_ARTWORK_ZOOM, zoomed.applyGesture(update).zoom, 0f)
        val fit = ArtworkFrame(ArtworkFit.Fit.name)
        assertSame(fit, fit.applyGesture(update))
        val fill = ArtworkFrame()
        assertSame(fill, fill.applyGesture(update))
    }

    @Test
    fun reducedImageCanMoveWithinTheMatteWithoutLosingTheSavedZoom() {
        val image = Size(400f, 200f)
        val viewport = Size(200f, 100f)
        val zoomedOut = custom.applyGesture(gesture(image, viewport, zoom = .5f))
        val moved = zoomedOut.applyGesture(gesture(image, viewport, pan = Offset(500f, -500f)))

        assertEquals(.5f, moved.zoom, 0f)
        assertEquals(ArtworkPlacement(100f, 50f, 100f, 0f), moved.place(400f, 200f, 200f, 100f))
        assertEquals(moved, moved.normalized())
    }

    private fun gesture(image: Size, viewport: Size, pan: Offset = Offset.Zero, zoom: Float = 1f) =
        ArtworkGesture(image, viewport, Offset(viewport.width / 2, viewport.height / 2), pan, zoom)
}
