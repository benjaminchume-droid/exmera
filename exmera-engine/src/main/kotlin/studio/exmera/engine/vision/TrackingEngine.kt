package studio.exmera.engine.vision

import kotlin.math.max
import kotlin.math.min

/** Coordinate spaces used by the tracker-facing APIs. */
enum class TrackingCoordinateSpace { NORMALIZED, VIEW, IMAGE }

data class TrackingPoint(val x: Float, val y: Float)

data class TrackingTransform(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    fun map(point: TrackingPoint): TrackingPoint =
        TrackingPoint(point.x * scaleX + offsetX, point.y * scaleY + offsetY)

    fun map(rect: VisionRect): VisionRect {
        val a = map(TrackingPoint(rect.left, rect.top))
        val b = map(TrackingPoint(rect.right, rect.bottom))
        return VisionRect(
            min(a.x, b.x).coerceIn(0f, 1f), min(a.y, b.y).coerceIn(0f, 1f),
            max(a.x, b.x).coerceIn(0f, 1f), max(a.y, b.y).coerceIn(0f, 1f)
        )
    }
}

enum class TrackState { TENTATIVE, TRACKED, OCCLUDED, LOST, REMOVED }

data class TrackingConfig(
    val maxMissingFrames: Int = 12,
    val minHitsToConfirm: Int = 2,
    val iouThreshold: Float = 0.12f,
    val maxAssociationDistance: Float = 0.25f,
    val velocitySmoothing: Float = 0.65f,
    val accelerationSmoothing: Float = 0.45f,
    val confidenceDecay: Float = 0.90f,
    val reidentificationFrames: Int = 30,
    val reidentificationIouThreshold: Float = 0.03f
) {
    init {
        require(maxMissingFrames >= 1)
        require(minHitsToConfirm >= 1)
        require(iouThreshold in 0f..1f)
        require(maxAssociationDistance > 0f)
        require(velocitySmoothing in 0f..1f)
        require(accelerationSmoothing in 0f..1f)
        require(confidenceDecay in 0f..1f)
        require(reidentificationFrames >= maxMissingFrames)
        require(reidentificationIouThreshold in 0f..1f)
    }
}

data class TrackSnapshot(
    val id: Long,
    val type: VisionObjectType,
    val bounds: VisionRect,
    val predictedBounds: VisionRect,
    val velocityX: Float,
    val velocityY: Float,
    val accelerationX: Float,
    val accelerationY: Float,
    val confidence: Float,
    val ageFrames: Int,
    val hits: Int,
    val missingFrames: Int,
    val state: TrackState
)

/**
 * Production-oriented tracker core. It combines IoU and predicted-center distance,
 * keeps velocity/acceleration state, survives short occlusions, and re-identifies
 * recently lost tracks without changing their public ID.
 */
class TrackingEngine(private val config: TrackingConfig = TrackingConfig()) {
    private data class Track(
        val id: Long,
        var type: VisionObjectType,
        var bounds: VisionRect,
        var velocityX: Float = 0f,
        var velocityY: Float = 0f,
        var accelerationX: Float = 0f,
        var accelerationY: Float = 0f,
        var confidence: Float = 0f,
        var ageFrames: Int = 0,
        var hits: Int = 0,
        var missingFrames: Int = 0,
        var state: TrackState = TrackState.TENTATIVE
    )

    private val tracks = ArrayList<Track>()
    private var nextId = 1L

    @Synchronized
    fun update(detections: List<VisionDetection>, deltaTimeSeconds: Float = 1f / 30f): List<TrackSnapshot> {
        require(deltaTimeSeconds > 0f && deltaTimeSeconds.isFinite())
        val dt = deltaTimeSeconds.coerceIn(1f / 240f, 1f)
        val predicted = tracks.map { predict(it, dt) }
        val assignments = associate(predicted, detections)
        val used = BooleanArray(detections.size)

        for ((trackIndex, detectionIndex) in assignments) {
            if (detectionIndex < 0) continue
            val track = tracks[trackIndex]
            val detection = detections[detectionIndex]
            updateTrack(track, detection, dt)
            used[detectionIndex] = true
        }

        for (i in tracks.indices) {
            if (assignments.none { it.first == i }) markMissing(tracks[i])
        }

        for (i in detections.indices) {
            if (!used[i]) tracks += newTrack(detections[i])
        }

        tracks.removeAll { it.state == TrackState.REMOVED }
        return tracks.map { snapshot(it, dt) }
            .filter { it.state != TrackState.REMOVED }
            .sortedBy { it.id }
    }

    @Synchronized fun reset() {
        tracks.clear()
        nextId = 1L
    }

    private fun predict(track: Track, dt: Float): VisionRect {
        val dx = track.velocityX * dt + 0.5f * track.accelerationX * dt * dt
        val dy = track.velocityY * dt + 0.5f * track.accelerationY * dt * dt
        return translate(track.bounds, dx, dy)
    }

    private fun associate(predicted: List<VisionRect>, detections: List<VisionDetection>): List<Pair<Int, Int>> {
        if (predicted.isEmpty() || detections.isEmpty()) return emptyList()
        data class Candidate(val ti: Int, val di: Int, val score: Float)
        val candidates = ArrayList<Candidate>()
        for (ti in tracks.indices) for (di in detections.indices) {
            if (tracks[ti].type != VisionObjectType.UNKNOWN && detections[di].type != VisionObjectType.UNKNOWN && tracks[ti].type != detections[di].type) continue
            val iou = iou(predicted[ti], detections[di].bounds)
            val distance = centerDistance(predicted[ti], detections[di].bounds)
            if (iou >= config.iouThreshold || distance <= config.maxAssociationDistance) {
                val score = iou * 0.72f + (1f - (distance / config.maxAssociationDistance).coerceIn(0f, 1f)) * 0.28f
                candidates += Candidate(ti, di, score)
            }
        }
        val usedTracks = HashSet<Int>()
        val usedDetections = HashSet<Int>()
        return candidates.sortedByDescending { it.score }.mapNotNull {
            if (usedTracks.add(it.ti) && usedDetections.add(it.di)) it.ti to it.di else null
        }
    }

    private fun updateTrack(track: Track, detection: VisionDetection, dt: Float) {
        val oldCx = track.bounds.centerX
        val oldCy = track.bounds.centerY
        val newCx = detection.bounds.centerX
        val newCy = detection.bounds.centerY
        val measuredVx = (newCx - oldCx) / dt
        val measuredVy = (newCy - oldCy) / dt
        val nextVx = lerp(track.velocityX, measuredVx, config.velocitySmoothing)
        val nextVy = lerp(track.velocityY, measuredVy, config.velocitySmoothing)
        val measuredAx = (nextVx - track.velocityX) / dt
        val measuredAy = (nextVy - track.velocityY) / dt
        track.accelerationX = lerp(track.accelerationX, measuredAx, config.accelerationSmoothing)
        track.accelerationY = lerp(track.accelerationY, measuredAy, config.accelerationSmoothing)
        track.velocityX = nextVx
        track.velocityY = nextVy
        track.bounds = detection.bounds
        track.type = if (detection.type != VisionObjectType.UNKNOWN) detection.type else track.type
        track.confidence = max(track.confidence * 0.35f, detection.confidence).coerceIn(0f, 1f)
        track.ageFrames++
        track.hits++
        track.missingFrames = 0
        track.state = if (track.hits >= config.minHitsToConfirm) TrackState.TRACKED else TrackState.TENTATIVE
    }

    private fun markMissing(track: Track) {
        track.missingFrames++
        track.ageFrames++
        track.bounds = translate(track.bounds, track.velocityX / 30f, track.velocityY / 30f)
        track.confidence = (track.confidence * config.confidenceDecay).coerceIn(0f, 1f)
        track.state = when {
            track.missingFrames > config.reidentificationFrames -> TrackState.REMOVED
            track.missingFrames > config.maxMissingFrames -> TrackState.LOST
            else -> TrackState.OCCLUDED
        }
    }

    private fun newTrack(detection: VisionDetection) = Track(
        id = nextId++, type = detection.type, bounds = detection.bounds,
        confidence = detection.confidence, ageFrames = 1, hits = 1
    )

    private fun snapshot(track: Track, dt: Float): TrackSnapshot = TrackSnapshot(
        id = track.id,
        type = track.type,
        bounds = track.bounds,
        predictedBounds = predict(track, dt),
        velocityX = track.velocityX,
        velocityY = track.velocityY,
        accelerationX = track.accelerationX,
        accelerationY = track.accelerationY,
        confidence = track.confidence,
        ageFrames = track.ageFrames,
        hits = track.hits,
        missingFrames = track.missingFrames,
        state = track.state
    )

    private fun translate(rect: VisionRect, dx: Float, dy: Float) = VisionRect(
        (rect.left + dx).coerceIn(0f, 1f), (rect.top + dy).coerceIn(0f, 1f),
        (rect.right + dx).coerceIn(0f, 1f), (rect.bottom + dy).coerceIn(0f, 1f)
    )

    private fun centerDistance(a: VisionRect, b: VisionRect): Float {
        val dx = a.centerX - b.centerX
        val dy = a.centerY - b.centerY
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun iou(a: VisionRect, b: VisionRect): Float {
        val l = max(a.left, b.left)
        val t = max(a.top, b.top)
        val r = min(a.right, b.right)
        val bottom = min(a.bottom, b.bottom)
        if (r <= l || bottom <= t) return 0f
        val intersection = (r - l) * (bottom - t)
        return intersection / (a.area + b.area - intersection)
    }

    private fun lerp(a: Float, b: Float, amount: Float) = a + (b - a) * amount
}

/** Stateless helper for mapping tracking results between normalized coordinate spaces. */
object TrackingCoordinateMapper {
    fun map(bounds: VisionRect, transform: TrackingTransform): VisionRect = transform.map(bounds)
    fun map(point: TrackingPoint, transform: TrackingTransform): TrackingPoint = transform.map(point)
}
