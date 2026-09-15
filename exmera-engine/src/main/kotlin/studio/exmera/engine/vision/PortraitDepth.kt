package studio.exmera.engine.vision

import studio.exmera.engine.imaging.RgbImage
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class DepthCalibration(
    val near: Float = 0f,
    val far: Float = 1f,
    val confidence: Float = 0.5f
) {
    init { require(near in 0f..1f && far in 0f..1f && far > near && confidence in 0f..1f) }
}

data class PortraitConfig(
    val blurRadius: Int = 18,
    val depthFalloff: Float = 3f,
    val edgeProtection: Float = 0.8f,
    val faceProtection: Float = 0.9f,
    val foregroundBias: Float = 0.55f
) {
    init {
        require(blurRadius in 0..64)
        require(depthFalloff > 0f)
        require(edgeProtection in 0f..1f)
        require(faceProtection in 0f..1f)
        require(foregroundBias in 0f..1f)
    }
}

data class PortraitMask(val width: Int, val height: Int, val alpha: FloatArray, val confidence: Float) {
    init {
        require(width > 0 && height > 0 && alpha.size == width * height)
        require(alpha.all { it in 0f..1f } && confidence in 0f..1f)
    }
}

data class DepthAwareImage(val image: RgbImage, val mask: PortraitMask, val depth: DepthMap)

/** Produces a conservative foreground mask from relative depth and optional detections. */
object PortraitMaskEstimator {
    fun estimate(image: RgbImage, depth: DepthMap, detections: List<VisionDetection> = emptyList(), config: PortraitConfig = PortraitConfig()): PortraitMask {
        require(image.width == depth.width && image.height == depth.height)
        val w = image.width
        val h = image.height
        val alpha = FloatArray(w * h)
        val knownSubjects = detections.filter { it.type == VisionObjectType.FACE || it.type == VisionObjectType.PERSON }
        for (y in 0 until h) for (x in 0 until w) {
            val p = y * w + x
            val d = depth.values[p]
            val depthForeground = smoothStep(config.foregroundBias + 0.18f, config.foregroundBias - 0.18f, d)
            var subject = 0f
            for (det in knownSubjects) if (x / w.toFloat() in det.bounds.left..det.bounds.right && y / h.toFloat() in det.bounds.top..det.bounds.bottom) {
                subject = max(subject, det.confidence)
            }
            alpha[p] = max(depthForeground, subject * config.foregroundBias).coerceIn(0f, 1f)
        }
        edgeSoften(alpha, w, h, config.edgeProtection)
        return PortraitMask(w, h, alpha, confidence = if (knownSubjects.isNotEmpty()) 0.85f else 0.45f)
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun edgeSoften(a: FloatArray, w: Int, h: Int, amount: Float) {
        if (amount <= 0f) return
        val copy = a.copyOf()
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val p = y * w + x
            val local = (abs(copy[p] - copy[p - 1]) + abs(copy[p] - copy[p + 1]) + abs(copy[p] - copy[p - w]) + abs(copy[p] - copy[p + w])) * 0.25f
            if (local > 0.05f) a[p] = copy[p] * (1f - amount * 0.35f) + ((copy[p - 1] + copy[p + 1] + copy[p - w] + copy[p + w]) * 0.25f) * amount * 0.35f
        }
    }
}

/** Creates a depth-aware portrait blur while retaining foreground detail. CPU reference implementation. */
class PortraitDepthEngine(private val config: PortraitConfig = PortraitConfig()) {
    fun process(image: RgbImage, depth: DepthMap, mask: PortraitMask? = null): DepthAwareImage {
        require(image.width == depth.width && image.height == depth.height)
        val resolvedMask = mask ?: PortraitMaskEstimator.estimate(image, depth, emptyList(), config)
        require(resolvedMask.width == image.width && resolvedMask.height == image.height)
        if (config.blurRadius == 0) return DepthAwareImage(image.copy(), resolvedMask, depth)
        val out = FloatArray(image.pixels.size)
        val radius = config.blurRadius
        for (y in 0 until image.height) for (x in 0 until image.width) {
            val p = y * image.width + x
            val foreground = resolvedMask.alpha[p]
            val d = depth.values[p]
            val blurAmount = ((d * config.depthFalloff) * (1f - foreground)).coerceIn(0f, 1f)
            val effective = max(1, (radius * blurAmount).toInt())
            val base = p * 3
            if (effective <= 1) {
                out[base] = image.pixels[base]; out[base + 1] = image.pixels[base + 1]; out[base + 2] = image.pixels[base + 2]
                continue
            }
            var sr = 0f; var sg = 0f; var sb = 0f; var count = 0
            for (dy in -effective..effective) for (dx in -effective..effective) {
                if (dx * dx + dy * dy > effective * effective) continue
                val nx = (x + dx).coerceIn(0, image.width - 1)
                val ny = (y + dy).coerceIn(0, image.height - 1)
                val i = (ny * image.width + nx) * 3
                sr += image.pixels[i]; sg += image.pixels[i + 1]; sb += image.pixels[i + 2]; count++
            }
            val inv = 1f / count
            val blend = blurAmount * (1f - config.edgeProtection * edgeStrength(depth, image.width, image.height, x, y)).coerceIn(0f, 1f)
            out[base] = image.pixels[base] * (1f - blend) + sr * inv * blend
            out[base + 1] = image.pixels[base + 1] * (1f - blend) + sg * inv * blend
            out[base + 2] = image.pixels[base + 2] * (1f - blend) + sb * inv * blend
        }
        return DepthAwareImage(RgbImage(image.width, image.height, out), resolvedMask, depth)
    }

    private fun edgeStrength(depth: DepthMap, w: Int, h: Int, x: Int, y: Int): Float {
        val p = y * w + x
        val c = depth.values[p]
        var sum = 0f
        if (x > 0) sum += abs(c - depth.values[p - 1])
        if (x < w - 1) sum += abs(c - depth.values[p + 1])
        if (y > 0) sum += abs(c - depth.values[p - w])
        if (y < h - 1) sum += abs(c - depth.values[p + w])
        return (sum * 2f).coerceIn(0f, 1f)
    }
}

/** Depth-aware bokeh controls shared by camera and editor pipelines. */
object BokehMath {
    fun blurFactor(depth: Float, subjectAlpha: Float, strength: Float = 1f): Float =
        (depth.coerceIn(0f, 1f) * strength.coerceAtLeast(0f) * (1f - subjectAlpha.coerceIn(0f, 1f))).coerceIn(0f, 1f)

    fun apertureToStrength(fNumber: Float): Float {
        require(fNumber > 0f && fNumber.isFinite())
        return (1f / fNumber).coerceIn(0.02f, 1f)
    }
}

/** Converts relative depth into a normalized display-friendly map with calibration metadata. */
object DepthProcessor {
    fun calibrate(depth: DepthMap, calibration: DepthCalibration = DepthCalibration()): DepthMap {
        val range = calibration.far - calibration.near
        val values = FloatArray(depth.values.size) { i -> ((depth.values[i] - calibration.near) / range).coerceIn(0f, 1f) }
        return DepthMap(depth.width, depth.height, values)
    }

    fun confidence(depth: DepthMap): Float {
        var mean = 0f
        for (v in depth.values) mean += v
        mean /= depth.values.size
        var variance = 0f
        for (v in depth.values) { val d = v - mean; variance += d * d }
        variance /= depth.values.size
        return (1f - sqrt(variance).coerceIn(0f, 1f)).coerceIn(0f, 1f)
    }
}
