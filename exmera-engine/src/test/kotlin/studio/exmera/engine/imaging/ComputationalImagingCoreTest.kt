package studio.exmera.engine.imaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputationalImagingCoreTest {
    @Test fun identicalFramesAlignAtZero() {
        val pixels = FloatArray(64) { (it % 8) / 8f }
        val image = GrayImage(8, 8, pixels)
        val motion = TranslationAligner.estimate(image, image, radius = 4, step = 1)
        assertEquals(0f, motion.dx, 0.01f)
        assertEquals(0f, motion.dy, 0.01f)
        assertTrue(motion.confidence > 0.9f)
    }

    @Test fun robustFusionSuppressesSingleOutlier() {
        fun frame(value: Float) = ImagingFrame(
            RgbImage(2, 2, FloatArray(12) { value }),
            GrayImage(2, 2, FloatArray(4) { value }),
            timestampNs = 1L
        )
        val result = ComputationalImagingCore().process(listOf(frame(0.4f), frame(0.4f), frame(1f)))
        assertTrue(result.image.pixels.all { it < 0.7f })
    }

    @Test fun emptyInputRejected() {
        try { ComputationalImagingCore().process(emptyList()); throw AssertionError("expected exception") }
        catch (_: IllegalArgumentException) { }
    }
}
