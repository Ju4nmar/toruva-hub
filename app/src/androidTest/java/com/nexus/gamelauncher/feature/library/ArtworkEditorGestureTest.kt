package com.nexus.gamelauncher.feature.library

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class ArtworkEditorGestureTest {
    @get:Rule val compose = createComposeRule()

    @Test fun gesturesSaveCoverWithoutActivatingNavigation() = verifyEditor(ArtworkKind.Cover)

    @Test fun gesturesSaveBackdropWithoutActivatingNavigation() = verifyEditor(ArtworkKind.Background)

    private fun verifyEditor(kind: ArtworkKind) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("editor-test-", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()

        val visible = mutableStateOf(true)
        val initial = ArtworkFrame(ArtworkFit.Custom.name)
        var saved = initial
        var navigationClicks = 0
        try {
            compose.setContent {
                MaterialTheme {
                    TextButton(onClick = { navigationClicks++; visible.value = true }) { Text("Abrir editor") }
                    if (visible.value) ArtworkEditor(file, kind, saved, dismiss = { visible.value = false }) {
                        saved = it
                        visible.value = false
                    }
                }
            }

            val preview = compose.onNodeWithContentDescription("Vista previa del encuadre")
            val zoom = compose.onNodeWithContentDescription("Zoom")
            fun sliderValue() = zoom.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current

            preview.assertIsDisplayed()
            repeat(4) {
                if (sliderValue() <= 1f) preview.performTouchInput {
                    val middle = center
                    pinch(
                        start0 = middle - Offset(width * .12f, 0f),
                        end0 = middle - Offset(width * .34f, 0f),
                        start1 = middle + Offset(width * .12f, 0f),
                        end1 = middle + Offset(width * .34f, 0f)
                    )
                }
            }
            assertTrue(sliderValue() > 1f)
            preview.performTouchInput { swipe(center, center + Offset(width * .25f, 0f), durationMillis = 300L) }
            preview.assertIsDisplayed()
            val drag = compose.onNodeWithContentDescription("Desplazamiento horizontal")
                .fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
            assertTrue(drag > 0f)

            compose.onNodeWithText("Guardar").performClick()
            preview.assertDoesNotExist()
            compose.runOnIdle {
                assertEquals(0, navigationClicks)
                assertEquals(drag, saved.x, 0f)
                assertTrue(saved.zoom > 1f)
            }

            compose.onNodeWithText("Abrir editor").performClick()
            assertEquals(saved.zoom, sliderValue(), 0f)
            compose.onNodeWithText("Restablecer ajuste").performClick()
            compose.onNodeWithText("Cancelar").performClick()
            compose.onNodeWithText("Abrir editor").performClick()
            assertEquals(saved.zoom, sliderValue(), 0f)
        } finally {
            file.delete()
        }
    }
}
