package studio.exmera.app.camera

import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import studio.exmera.engine.core.MultiFrameBuffer
import java.util.concurrent.atomic.AtomicLong

/**
 * Phase 8 capture coordinator. CameraX analysis frames are retained only while a
 * computational capture is active; otherwise they are immediately released.
 */
class MultiFrameCaptureEngine(
    capacity: Int = 8
) : AutoCloseable {
    enum class State { IDLE, CAPTURING, COMPLETE, CLOSED }

    data class CapturedFrame(
        val sequence: Long,
        val timestampNs: Long,
        val image: ImageProxy,
        val width: Int,
        val height: Int,
        val rotationDegrees: Int,
        val format: Int
    ) : AutoCloseable {
        override fun close() = image.close()
    }

    data class CaptureResult(
        val frames: List<CapturedFrame>,
        val startedAtNs: Long,
        val completedAtNs: Long
    )

    private val buffer = MultiFrameBuffer<CapturedFrame>(capacity)
    private val sequence = AtomicLong(0)
    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    private var targetCount = 0
    private var startedAtNs = 0L

    @Synchronized
    fun start(targetFrames: Int = 5) {
        check(_state.value != State.CLOSED) { "Capture engine is closed" }
        require(targetFrames in 2..16) { "targetFrames must be between 2 and 16" }
        buffer.drain().forEach(CapturedFrame::close)
        targetCount = targetFrames
        startedAtNs = System.nanoTime()
        _state.value = State.CAPTURING
    }

    /** Takes ownership of the ImageProxy only when the capture is active. */
    @Synchronized
    fun accept(image: ImageProxy): Boolean {
        if (_state.value != State.CAPTURING) {
            image.close()
            return false
        }
        val captured = CapturedFrame(
            sequence = sequence.getAndIncrement(),
            timestampNs = image.imageInfo.timestamp,
            image = image,
            width = image.width,
            height = image.height,
            rotationDegrees = image.imageInfo.rotationDegrees,
            format = image.format
        )
        if (!buffer.offer(captured)) return false
        if (buffer.size() >= targetCount) _state.value = State.COMPLETE
        return true
    }

    @Synchronized
    fun result(): CaptureResult? {
        if (_state.value != State.COMPLETE) return null
        return CaptureResult(buffer.snapshot(), startedAtNs, System.nanoTime())
    }

    @Synchronized
    fun reset() {
        check(_state.value != State.CLOSED) { "Capture engine is closed" }
        buffer.drain().forEach(CapturedFrame::close)
        _state.value = State.IDLE
    }

    @Synchronized
    override fun close() {
        if (_state.value == State.CLOSED) return
        buffer.close()
        _state.value = State.CLOSED
    }
}
