package studio.exmera.engine.imaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuperResolutionTest {
    private fun gradient(w: Int = 8, h: Int = 6): RgbImage {
        val pixels = FloatArray(w * h * 3)
        for (y in 0 until h) for (x in 0 until w) {
            val p = (y * w + x) * 3
            pixels[p] = x.toFloat() / (w - 1)
            pixels[p + 1] = y.toFloat() / (h - 1)
            pixels[p + 2] = (x + y).toFloat() / (w + h - 2)
        }
        return RgbImage(w, h, pixels)
    }

    @Test fun x2ProducesExpectedDimensionsAndBoundedPixels() {
        val result = SuperResolutionEngine().process(gradient(), SuperResolutionConfig(SuperResolutionScale.X2, tileSize = 32))
        assertEquals(16, result.image.width)
        assertEquals(12, result.image.height)
        assertTrue(result.image.pixels.all { it in 0f..1f })
        assertEquals("cpu-reconstruction", result.backend)
    }

    @Test fun x4ProducesExpectedDimensions() {
        val input = gradient(7, 5)
        val output = SuperResolutionEngine().process(input, SuperResolutionConfig(SuperResolutionScale.X4, tileSize = 32)).image
        assertEquals(28, output.width)
        assertEquals(20, output.height)
    }

    @Test fun constantImageRemainsConstant() {
        val input = RgbImage(5, 5, FloatArray(75) { 0.37f })
        val output = SuperResolutionEngine().process(input, SuperResolutionConfig(sharpen = 1f)).image
        assertTrue(output.pixels.all { kotlin.math.abs(it - 0.37f) < 1e-4f })
    }

    @Test fun qualityMetricsAreDeterministic() {
        val a = gradient(4, 4)
        assertEquals(0f, ImageQualityMetrics.meanAbsoluteError(a, a), 1e-7f)
        assertTrue(ImageQualityMetrics.psnr(a, a).isInfinite())
        assertTrue(ImageQualityMetrics.edgeEnergy(a) > 0f)
    }
}
