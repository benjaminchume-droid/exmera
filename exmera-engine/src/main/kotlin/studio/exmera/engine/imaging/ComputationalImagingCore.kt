package studio.exmera.engine.imaging

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Small, allocation-conscious image plane used by the computational imaging core. */
data class GrayImage(val width: Int, val height: Int, val pixels: FloatArray) {
    init { require(width > 0 && height > 0 && pixels.size == width * height) }
    operator fun get(x: Int, y: Int): Float = pixels[y * width + x]
}

data class RgbImage(val width: Int, val height: Int, val pixels: FloatArray) {
    init { require(width > 0 && height > 0 && pixels.size == width * height * 3) }
    fun index(x: Int, y: Int) = (y * width + x) * 3
}

data class MotionVector(val dx: Float, val dy: Float, val confidence: Float)
data class FrameRegistration(val frameIndex: Int, val motion: MotionVector, val usable: Boolean)

data class ImagingFrame(
    val image: RgbImage,
    val luma: GrayImage,
    val timestampNs: Long,
    val exposure: Float = 1f,
    val motionScore: Float = 0f
)

data class FusionResult(
    val image: RgbImage,
    val registrations: List<FrameRegistration>,
    val rejectedFrames: Int,
    val medianExposure: Float
)

/**
 * Phase 9 registration primitive. It estimates a translation against a reference
 * using a coarse normalized cross-correlation search. This is intentionally
 * deterministic and robust on CPU; an EXM optical-flow backend can replace it later.
 */
object TranslationAligner {
    fun estimate(reference: GrayImage, candidate: GrayImage, radius: Int = 8, step: Int = 4): MotionVector {
        require(reference.width == candidate.width && reference.height == candidate.height)
        val r = max(1, radius)
        val s = max(1, step)
        val marginX = max(2, reference.width / 10)
        val marginY = max(2, reference.height / 10)
        val x0 = marginX + r
        val x1 = reference.width - marginX - r
        val y0 = marginY + r
        val y1 = reference.height - marginY - r
        if (x1 <= x0 || y1 <= y0) return MotionVector(0f, 0f, 0f)

        var best = -Float.MAX_VALUE
        var bestDx = 0
        var bestDy = 0
        for (dy in -r..r step s) for (dx in -r..r step s) {
            var sumA = 0.0; var sumB = 0.0; var sumAA = 0.0; var sumBB = 0.0; var sumAB = 0.0
            var n = 0
            for (y in y0 until y1 step max(1, reference.height / 72)) {
                for (x in x0 until x1 step max(1, reference.width / 72)) {
                    val a = reference[x, y].toDouble()
                    val b = candidate[x + dx, y + dy].toDouble()
                    sumA += a; sumB += b; sumAA += a * a; sumBB += b * b; sumAB += a * b; n++
                }
            }
            val denom = sqrt(max(1e-9, (n * sumAA - sumA * sumA) * (n * sumBB - sumB * sumB)))
            val corr = (n * sumAB - sumA * sumB) / denom
            if (corr > best) { best = corr; bestDx = dx; bestDy = dy }
        }
        return MotionVector(bestDx.toFloat(), bestDy.toFloat(), ((best + 1.0) / 2.0).toFloat().coerceIn(0f, 1f))
    }
}

/** Exposure normalization plus robust temporal fusion with per-pixel outlier rejection. */
object RobustFrameFusion {
    fun fuse(frames: List<ImagingFrame>, registrations: List<FrameRegistration>): FusionResult {
        require(frames.isNotEmpty())
        val ref = frames.first().image
        val w = ref.width; val h = ref.height
        val output = FloatArray(w * h * 3)
        val weights = FloatArray(w * h)
        val exposures = frames.map { it.exposure.coerceAtLeast(0.01f) }.sorted()
        val medianExposure = exposures[exposures.size / 2]

        val usable = frames.indices.filter { i -> registrations.firstOrNull { it.frameIndex == i }?.usable != false }
        val normalized = ArrayList<FloatArray>(usable.size)
        for (i in usable) {
            val src = frames[i].image.pixels
            val gain = (medianExposure / frames[i].exposure.coerceAtLeast(0.01f)).coerceIn(0.5f, 2f)
            normalized += FloatArray(src.size) { k -> (src[k] * gain).coerceIn(0f, 1f) }
        }

        for (p in 0 until w * h) {
            val values = FloatArray(normalized.size)
            val x = p % w; val y = p / w
            for (c in 0..2) {
                var count = 0
                for (j in normalized.indices) {
                    val motion = registrations.firstOrNull { it.frameIndex == usable[j] }?.motion ?: MotionVector(0f, 0f, 0f)
                    val sx = (x + motion.dx).toInt().coerceIn(0, w - 1)
                    val sy = (y + motion.dy).toInt().coerceIn(0, h - 1)
                    values[count++] = normalized[j][(sy * w + sx) * 3 + c]
                }
                values.sort(0, count)
                val median = values[count / 2]
                val mad = values.take(count).map { abs(it - median) }.sorted()[count / 2].coerceAtLeast(0.008f)
                var sum = 0f; var weight = 0f
                for (j in 0 until count) {
                    val d = abs(values[j] - median)
                    val robust = if (d <= 2.5f * mad) 1f else (2.5f * mad / d).coerceIn(0f, 1f)
                    sum += values[j] * robust; weight += robust
                }
                output[p * 3 + c] = if (weight > 0f) sum / weight else median
            }
            weights[p] = 1f
        }
        return FusionResult(RgbImage(w, h, output), registrations, frames.size - usable.size, medianExposure)
    }
}

/** End-to-end CPU imaging graph: align -> reject low-confidence frames -> robust fusion. */
class ComputationalImagingCore(
    private val alignmentRadius: Int = 8,
    private val minimumConfidence: Float = 0.48f
) {
    fun process(frames: List<ImagingFrame>): FusionResult {
        require(frames.isNotEmpty())
        val ref = frames.first()
        val regs = ArrayList<FrameRegistration>(frames.size)
        regs += FrameRegistration(0, MotionVector(0f, 0f, 1f), true)
        for (i in 1 until frames.size) {
            val motion = TranslationAligner.estimate(ref.luma, frames[i].luma, alignmentRadius)
            val usable = motion.confidence >= minimumConfidence && frames[i].motionScore < 0.92f
            regs += FrameRegistration(i, motion, usable)
        }
        return RobustFrameFusion.fuse(frames, regs)
    }
}
