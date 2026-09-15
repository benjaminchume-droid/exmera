package studio.exmera.engine.core

import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Resource-safe bounded frame retention for computational photography.
 * The payload is owned by the caller and released exactly once when evicted,
 * drained, or explicitly closed.
 */
class MultiFrameBuffer<T : AutoCloseable>(private val capacity: Int) : AutoCloseable {
    init { require(capacity in 1..32) { "capacity must be between 1 and 32" } }

    private val frames = ArrayDeque<T>(capacity)
    private val lock = Any()
    private var closed = false

    fun offer(frame: T): Boolean = synchronized(lock) {
        if (closed) {
            frame.close()
            return false
        }
        if (frames.size == capacity) frames.removeFirst().close()
        frames.addLast(frame)
        true
    }

    fun size(): Int = synchronized(lock) { frames.size }

    fun snapshot(): List<T> = synchronized(lock) { frames.toList() }

    fun drain(): List<T> = synchronized(lock) {
        val result = frames.toList()
        frames.clear()
        result
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            while (frames.isNotEmpty()) frames.removeFirst().close()
        }
    }
}

/** Immutable information used to rank captured frames before expensive fusion. */
data class FrameSelectionInfo(
    val sequence: Long,
    val timestampNs: Long,
    val sharpnessScore: Float = 1f,
    val motionScore: Float = 0f,
    val exposureScore: Float = 1f
) {
    init {
        require(sequence >= 0)
        require(timestampNs >= 0)
        require(sharpnessScore >= 0f && motionScore >= 0f && exposureScore >= 0f)
    }
}

object MultiFrameSelector {
    /** Stable, deterministic ranking; higher is better. */
    fun score(info: FrameSelectionInfo): Float =
        info.sharpnessScore * 0.5f + info.exposureScore * 0.3f + (1f / (1f + info.motionScore)) * 0.2f

    fun select(infos: Collection<FrameSelectionInfo>, limit: Int): List<FrameSelectionInfo> {
        require(limit > 0)
        return infos.sortedByDescending(::score).take(limit)
    }
}
