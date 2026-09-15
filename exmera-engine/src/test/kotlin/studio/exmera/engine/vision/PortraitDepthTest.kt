package studio.exmera.engine.vision

import studio.exmera.engine.imaging.RgbImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PortraitDepthTest {
    private fun image(w: Int = 12, h: Int = 10): RgbImage = RgbImage(w, h, FloatArray(w * h * 3) { i -> ((i % 3) * .25f + (i / 3 % w) / w.toFloat() * .5f).coerceIn(0f, 1f) })

    @Test fun `mask dimensions and values are valid`() {
        val img = image()
        val depth = RelativeDepthEstimator.estimate(img)
        val mask = PortraitMaskEstimator.estimate(img, depth)
        assertEquals(img.width, mask.width)
        assertEquals(img.height, mask.height)
        assertTrue(mask.alpha.all { it in 0f..1f })
        assertTrue(mask.confidence in 0f..1f)
    }

    @Test fun `depth aware processing preserves dimensions and range`() {
        val img = image()
        val depth = RelativeDepthEstimator.estimate(img)
        val result = PortraitDepthEngine(PortraitConfig(blurRadius = 3)).process(img, depth)
        assertEquals(img.width, result.image.width)
        assertEquals(img.height, result.image.height)
        assertTrue(result.image.pixels.all { it in 0f..1f })
    }

    @Test fun `zero blur is stable`() {
        val img = image()
        val depth = RelativeDepthEstimator.estimate(img)
        val result = PortraitDepthEngine(PortraitConfig(blurRadius = 0)).process(img, depth)
        assertTrue(result.image.pixels.contentEquals(img.pixels))
    }

    @Test fun `bokeh math is bounded`() {
        assertEquals(1f, BokehMath.blurFactor(1f, 0f, 2f))
        assertEquals(0f, BokehMath.blurFactor(1f, 1f))
        assertTrue(BokehMath.apertureToStrength(2f) in 0f..1f)
    }

    @Test fun `depth calibration remains normalized`() {
        val depth = DepthMap(2, 1, floatArrayOf(.2f, .8f))
        val calibrated = DepthProcessor.calibrate(depth, DepthCalibration(.2f, .8f))
        assertEquals(0f, calibrated.values[0])
        assertEquals(1f, calibrated.values[1])
    }
}
