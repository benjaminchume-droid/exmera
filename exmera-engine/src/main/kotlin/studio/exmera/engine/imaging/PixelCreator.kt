package studio.exmera.engine.imaging

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Lightweight semantic description used to steer reconstruction without inventing scene content. */
data class PixelSemanticMap(
    val structure: FloatArray,
    val texture: FloatArray,
    val detail: FloatArray
) {
    init {
        require(structure.size == texture.size && texture.size == detail.size)
        require(structure.all { it in 0f..1f } && texture.all { it in 0f..1f } && detail.all { it in 0f..1f })
    }
}

data class PixelCreatorConfig(
    val structureStrength: Float = 0.30f,
    val textureStrength: Float = 0.18f,
    val detailStrength: Float = 0.24f,
    val localContrast: Float = 0.10f,
    val denoise: Float = 0.08f,
    val preserveColor: Float = 0.88f,
    val radius: Int = 2
) {
    init {
        require(structureStrength in 0f..1f)
        require(textureStrength in 0f..1f)
        require(detailStrength in 0f..1f)
        require(localContrast in 0f..1f)
        require(denoise in 0f..1f)
        require(preserveColor in 0f..1f)
        require(radius in 1..6)
    }
}

data class PixelCreatorResult(
    val image: RgbImage,
    val backend: String,
    val semanticMap: PixelSemanticMap,
    val reconstructionScore: Float
)

/**
 * Pixel Creator backend contract. A learned EXM backend can implement this interface
 * without changing the camera/editor graph.
 */
interface PixelCreatorBackend {
    val name: String
    fun process(input: RgbImage, semantics: PixelSemanticMap, config: PixelCreatorConfig): RgbImage
}

/** Deterministic CPU reconstruction backend used when no learned EXM pack is installed. */
class CpuPixelCreatorBackend : PixelCreatorBackend {
    override val name: String = "cpu-pixel-reconstruction"

    override fun process(input: RgbImage, semantics: PixelSemanticMap, config: PixelCreatorConfig): RgbImage {
        val w = input.width
        val h = input.height
        val output = input.pixels.copyOf()
        val luma = FloatArray(w * h)
        for (p in 0 until w * h) {
            val i = p * 3
            luma[p] = 0.2126f * input.pixels[i] + 0.7152f * input.pixels[i + 1] + 0.0722f * input.pixels[i + 2]
        }

        // Multi-scale local statistics provide stable structure/texture/detail signals.
        val small = boxBlur(luma, w, h, config.radius)
        val broad = boxBlur(luma, w, h, config.radius * 2 + 1)
        for (y in 0 until h) for (x in 0 until w) {
            val p = y * w + x
            val i = p * 3
            val center = luma[p]
            val low = small[p]
            val base = broad[p]
            val structure = semantics.structure[p]
            val texture = semantics.texture[p]
            val detail = semantics.detail[p]

            val edgeResidual = center - low
            val fineResidual = low - base
            val contrast = (center - base) * config.localContrast
            val reconstruction =
                edgeResidual * config.detailStrength * detail +
                fineResidual * config.textureStrength * texture +
                contrast * config.structureStrength * structure

            // Pull very small residuals toward the local field to suppress chroma/noise speckle.
            val suppression = config.denoise * (1f - detail)
            for (c in 0..2) {
                val original = input.pixels[i + c]
                val channelBase = original - (center - base) * channelLumaWeight(c)
                val enhanced = original + reconstruction
                val denoised = enhanced * (1f - suppression) + channelBase * suppression
                output[i + c] = (denoised * config.preserveColor + original * (1f - config.preserveColor)).coerceIn(0f, 1f)
            }
        }
        return RgbImage(w, h, output)
    }

    private fun channelLumaWeight(channel: Int): Float = when (channel) {
        0 -> 0.2126f
        1 -> 0.7152f
        else -> 0.0722f
    }

    private fun boxBlur(input: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val r = max(1, radius)
        val horizontal = FloatArray(input.size)
        val output = FloatArray(input.size)
        for (y in 0 until h) {
            var sum = 0f
            for (x in -r..r) sum += input[y * w + x.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                horizontal[y * w + x] = sum / (2 * r + 1)
                val removeX = (x - r).coerceIn(0, w - 1)
                val addX = (x + r + 1).coerceIn(0, w - 1)
                sum += input[y * w + addX] - input[y * w + removeX]
            }
        }
        for (x in 0 until w) {
            var sum = 0f
            for (y in -r..r) sum += horizontal[y.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                output[y * w + x] = sum / (2 * r + 1)
                val removeY = (y - r).coerceIn(0, h - 1)
                val addY = (y + r + 1).coerceIn(0, h - 1)
                sum += horizontal[addY * w + x] - horizontal[removeY * w + x]
            }
        }
        return output
    }
}

/** Deterministic scene-independent analysis. It never fabricates semantic objects. */
object PixelSemanticAnalyzer {
    fun analyze(input: RgbImage): PixelSemanticMap {
        val w = input.width
        val h = input.height
        val luma = FloatArray(w * h)
        for (p in 0 until w * h) {
            val i = p * 3
            luma[p] = 0.2126f * input.pixels[i] + 0.7152f * input.pixels[i + 1] + 0.0722f * input.pixels[i + 2]
        }
        val structure = FloatArray(luma.size)
        val texture = FloatArray(luma.size)
        val detail = FloatArray(luma.size)
        for (y in 0 until h) for (x in 0 until w) {
            val p = y * w + x
            val gx = luma[y * w + (x + 1).coerceAtMost(w - 1)] - luma[y * w + (x - 1).coerceAtLeast(0)]
            val gy = luma[(y + 1).coerceAtMost(h - 1) * w + x] - luma[(y - 1).coerceAtLeast(0) * w + x]
            val gradient = sqrt(gx * gx + gy * gy).coerceIn(0f, 1f)
            val variance = localVariance(luma, w, h, x, y)
            structure[p] = (gradient * 2.5f).coerceIn(0f, 1f)
            texture[p] = (variance * 12f).coerceIn(0f, 1f)
            detail[p] = (gradient * 1.6f + variance * 5f).coerceIn(0f, 1f)
        }
        return PixelSemanticMap(structure, texture, detail)
    }

    private fun localVariance(data: FloatArray, w: Int, h: Int, x: Int, y: Int): Float {
        var sum = 0f
        var sumSq = 0f
        var n = 0
        for (dy in -1..1) for (dx in -1..1) {
            val xx = (x + dx).coerceIn(0, w - 1)
            val yy = (y + dy).coerceIn(0, h - 1)
            val v = data[yy * w + xx]
            sum += v
            sumSq += v * v
            n++
        }
        val mean = sum / n
        return max(0f, sumSq / n - mean * mean)
    }
}

class PixelCreatorEngine(
    private val backends: List<PixelCreatorBackend> = listOf(CpuPixelCreatorBackend())
) {
    fun process(input: RgbImage, config: PixelCreatorConfig = PixelCreatorConfig()): PixelCreatorResult {
        require(input.width > 0 && input.height > 0)
        val semantics = PixelSemanticAnalyzer.analyze(input)
        val backend = backends.firstOrNull() ?: error("No Pixel Creator backend installed")
        val output = backend.process(input, semantics, config)
        val score = reconstructionScore(input, output, semantics)
        return PixelCreatorResult(output, backend.name, semantics, score)
    }

    private fun reconstructionScore(original: RgbImage, output: RgbImage, map: PixelSemanticMap): Float {
        var preserved = 0f
        var total = 0f
        for (p in map.structure.indices) {
            val i = p * 3
            val delta = abs(output.pixels[i] - original.pixels[i]) +
                abs(output.pixels[i + 1] - original.pixels[i + 1]) +
                abs(output.pixels[i + 2] - original.pixels[i + 2])
            val confidence = 0.5f + 0.5f * map.structure[p]
            preserved += confidence * (1f - (delta / 3f).coerceIn(0f, 1f))
            total += confidence
        }
        return if (total == 0f) 1f else (preserved / total).coerceIn(0f, 1f)
    }
}
