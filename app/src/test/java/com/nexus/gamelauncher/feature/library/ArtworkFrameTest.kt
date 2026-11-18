package com.nexus.gamelauncher.feature.library

import org.junit.Assert.*
import org.junit.Test

class ArtworkFrameTest {
    @Test
    fun fitShowsAllOfAPanoramicImageAndFillKeepsItsAspect() {
        val fit = ArtworkFrame(ArtworkFit.Fit.name).place(4000f, 1000f, 400f, 200f)
        val fill = ArtworkFrame().place(4000f, 1000f, 400f, 200f)

        assertEquals(ArtworkPlacement(400f, 100f, 0f, 50f), fit)
        assertEquals(ArtworkPlacement(800f, 200f, -200f, 0f), fill)
    }

    @Test
    fun customPanReachesBothEdgesWithoutRevealingEmptySpace() {
        val frame = ArtworkFrame(ArtworkFit.Custom.name, 2f, 1f, -1f)
        assertEquals(ArtworkPlacement(1600f, 400f, 0f, -200f), frame.place(4000f, 1000f, 400f, 200f))
        assertEquals(
            ArtworkPlacement(1600f, 400f, -1200f, 0f),
            frame.copy(x = -1f, y = 1f).place(4000f, 1000f, 400f, 200f)
        )
    }

    @Test
    fun frameNormalizesInvalidControlsAndRestoresDefault() {
        assertEquals(ArtworkFrame(), ArtworkFrame("legacy-unknown", 3f, 1f, 1f).normalized())
        assertEquals(
            ArtworkFrame(ArtworkFit.Custom.name, 1f, 1f, 0f),
            ArtworkFrame(ArtworkFit.Custom.name, Float.NaN, 4f, Float.POSITIVE_INFINITY).normalized()
        )
    }

    @Test
    fun largePanoramasAreAcceptedWithinPixelBudget() {
        assertTrue(supportedArtworkDimensions(16000, 500))
        assertTrue(supportedArtworkDimensions(10000, 10000))
        assertFalse(supportedArtworkDimensions(16000, 16000))
        assertFalse(supportedArtworkDimensions(16385, 10))
        assertFalse(supportedArtworkDimensions(0, 600))
    }
}
