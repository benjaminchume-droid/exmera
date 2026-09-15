package studio.exmera.engine.imaging

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Requested output scale for the super-resolution pipeline. */
enum class SuperResolutionScale(val factor: Int) {
    X2(2),
    X4(4)
}

data class SuperResolutionConfig(
    val scale: SuperResolutionScale = SuperResolutionScale.X2,
    val tileSize: Int = 256,
    val overlap: Int = 16,
    val sharpen: Float = 0.22f
) {
    init {
        require(tileSize >= 32)
        require(overlap >= 0 && overlap * 2 < tileSize)
        require(sharpen in 0f..1f)
    }
}

data class SuperResolutionResult(
    val image: RgbImage,
    val scale: SuperResolutionScale,
    val backend: String,
    val inputWidth: Int,
    val inputHeight: Int
)

/** Backend boundary used by EXM learned models when they become available. */
interface SuperResolutionBackend {
    val name: String
    fun supports(scale: SuperResolutionScale): Boolean
    fun upscale(input: RgbImage, config: SuperResolutionConfig): RgbImage
}

/**
 * Deterministic CPU reconstruction backend. It is deliberately model-free: no
 * hallucinated detail is introduced from a prompt or remote service. Bicubic
 * reconstruction supplies the missing samples and a restrained edge-aware
 * residual restores local acutance.
 */
class CpuSuperResolutionBackend : SuperResolutionBackend {
    override val name: String = "cpu-reconstruction"

    override fun supports(scale: SuperResolutionScale): Boolean = true

    override fun upscale(input: RgbImage, config: SuperResolutionConfig): RgbImage {
        val factor = config.scale.factor
        val outW = input.width * factor
        val outH = input.height * factor
        val output = FloatArray(outW * outH * 3)

        // Bicubic sampling gives smooth, stable reconstruction and avoids the
        // block boundaries that a naive nearest-neighbour implementation creates.
        for (y in 0 until outH) {
            val srcY = y.toFloat() / factor
            val yBase = floor(srcY).toInt()
            val fy = srcY - yBase
            for (x in 0 until outW) {
                val srcX = x.toFloat() / factor
                val xBase = floor(srcX).toInt()
                val fx = srcX - xBase
                val oi = (y * outW + x) * 3
                for (c in 0..2) {
                    output[oi + c] = bicubic(input, xBase, yBase, fx, fy, c)
                }
            }
        }

        if (config.sharpen > 0f) {
            applyEdgeResidual(output, outW, outH, config.sharpen)
        }
        return RgbImage(outW, outH, output)
    }

    private fun bicubic(image: RgbImage, x: Int, y: Int, fx: Float, fy: Float, c: Int): Float {
        var value = 0f
        var total = 0f
        for (j in -1..2) {
            val wy = cubicWeight(j - fy)
            for (i in -1..2) {
                val wx = cubicWeight(i - fx)
                val weight = wx * wy
                val sx = (x + i).coerceIn(0, image.width - 1)
                val sy = (y + j).coerceIn(0, image.height - 1)
                value += image.pixels[(sy * image.width + sx) * 3 + c] * weight
                total += weight
            }
        }
        return if (abs(total) > 1e-6f) (value / total).coerceIn(0f, 1f) else 0f
    }

    private fun cubicWeight(x: Float): Float {
        val a = -0.5f
        val t = abs(x)
        return when {
            t <= 1f -> (a + 2f) * t * t * t - (a + 3f) * t * t + 1f
            t < 2f -> a * t * t * t - 5f * a * t * t + 8f * a * t - 4f * a
            else -> 0f
        }
    }

    private fun applyEdgeResidual(pixels: FloatArray, width: Int, height: Int, amount: Float) {
        if (width < 3 || height < 3) return
        val source = pixels.copyOf()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = (y * width + x) * 3
                var edge = 0f
                for (c in 0..2) {
                    val up = source[((y - 1) * width + x) * 3 + c]
                    val down = source[((y + 1) * width + x) * 3 + c]
                    val left = source[(y * width + x - 1) * 3 + c]
                    val right = source[(y * width + x + 1) * 3 + c]
                    val laplacian = source[center + c] - (up + down + left + right) * 0.25f
                    edge += abs(laplacian)
                    pixels[center + c] = (source[center + c] + laplacian * amount).coerceIn(0f, 1f)
                }
                if (edge < 0.015f) {
                    for (c in 0..2) pixels[center + c] = source[center + c]
                }
            }
        }
    }
}

/**
 * Public Phase 10 engine. A learned EXM backend can be supplied later without
 * changing the camera or editor APIs. CPU reconstruction remains the guaranteed
 * fallback on every supported Android device.
 */
class SuperResolutionEngine(
    private val backends: List<SuperResolutionBackend> = listOf(CpuSuperResolutionBackend())
) {
    fun process(input: RgbImage, config: SuperResolutionConfig = SuperResolutionConfig()): SuperResolutionResult {
        require(input.width > 0 && input.height > 0)
        val backend = backends.firstOrNull { it.supports(config.scale) }
            ?: error("No super-resolution backend supports ${config.scale}")
        val output = backend.upscale(input, config)
        return SuperResolutionResult(output, config.scale, backend.name, input.width, input.height)
    }
}

/** Lightweight objective metrics used by the quality/benchmark layer. */
object ImageQualityMetrics {
    fun meanAbsoluteError(a: RgbImage, b: RgbImage): Float {
        require(a.width == b.width && a.height == b.height)
        var sum = 0.0
        for (i in a.pixels.indices) sum += abs(a.pixels[i] - b.pixels[i])
        return (sum / a.pixels.size).toFloat()
    }

    fun psnr(a: RgbImage, b: RgbImage): Float {
        require(a.width == b.width && a.height == b.height)
        var mse = 0.0
        for (i in a.pixels.indices) {
            val d = (a.pixels[i] - b.pixels[i]).toDouble()
            mse += d * d
        }
        mse /= a.pixels.size
        if (mse <= 1e-12) return Float.POSITIVE_INFINITY
        return (10.0 * kotlin.math.log10(1.0 / mse)).toFloat()
    }

    fun edgeEnergy(image: RgbImage): Float {
        if (image.width < 2 || image.height < 2) return 0f
        var sum = 0.0
        var count = 0
        for (y in 0 until image.height - 1) for (x in 0 until image.width - 1) {
            val i = (y * image.width + x) * 3
            val j = ((y + 1) * image.width + x) * 3
            val k = (y * image.width + x + 1) * 3
            for (c in 0..2) {
                val dy = image.pixels[i + c] - image.pixels[j + c]
                val dx = image.pixels[i + c] - image.pixels[k + c]
                sum += sqrt((dx * dx + dy * dy).toDouble())
                count++
            }
        }
        return (sum / max(1, count)).toFloat()
    }
}
