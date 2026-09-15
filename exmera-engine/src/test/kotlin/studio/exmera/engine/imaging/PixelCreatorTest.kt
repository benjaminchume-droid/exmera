package studio.exmera.engine.imaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PixelCreatorTest {
    private fun image(w: Int = 12, h: Int = 10): RgbImage {
        val pixels = FloatArray(w * h * 3)
        for (y in 0 until h) for (x in 0 until w) {
            val i = (y * w + x) * 3
            val edge = if (x >= w / 2) 0.75f else 0.25f
            val texture = ((x * 17 + y * 11) % 7) / 100f
            pixels[i] = (edge + texture).coerceIn(0f, 1f)
            pixels[i + 1] = (edge * 0.9f + texture).coerceIn(0f, 1f)
            pixels[i + 2] = (edge * 0.8f + texture).coerceIn(0f, 1f)
        }
        return RgbImage(w, h, pixels)
    }

    @Test fun preservesDimensionsAndBounds() {
        val input = image()
        val result = PixelCreatorEngine().process(input)
        assertEquals(input.width, result.image.width)
        assertEquals(input.height, result.image.height)
        assertTrue(result.image.pixels.all { it in 0f..1f })
        assertEquals("cpu-pixel-reconstruction", result.backend)
        assertTrue(result.reconstructionScore in 0f..1f)
    }

    @Test fun semanticMapsMatchImageSize() {
        val input = image(9, 7)
        val map = PixelSemanticAnalyzer.analyze(input)
        assertEquals(63, map.structure.size)
        assertEquals(63, map.texture.size)
        assertEquals(63, map.detail.size)
        assertTrue(map.structure.any { it > 0f })
    }

    @Test fun constantImageIsStable() {
        val input = RgbImage(8, 8, FloatArray(8 * 8 * 3) { 0.42f })
        val output = PixelCreatorEngine().process(input, PixelCreatorConfig(
            structureStrength = 1f,
            textureStrength = 1f,
            detailStrength = 1f,
            localContrast = 1f,
            denoise = 1f
        )).image
        assertTrue(output.pixels.all { abs(it - 0.42f) < 1e-4f })
    }

    @Test fun customBackendCanReplaceCpuImplementation() {
        val backend = object : PixelCreatorBackend {
            override val name = "test-backend"
            override fun process(input: RgbImage, semantics: PixelSemanticMap, config: PixelCreatorConfig): RgbImage = input
        }
        val result = PixelCreatorEngine(listOf(backend)).process(image())
        assertEquals("test-backend", result.backend)
        assertEquals(0f, result.image.pixels.zip(image().pixels).sumOf { abs(it.first - it.second).toDouble() }.toFloat(), 1e-6f)
    }
}
