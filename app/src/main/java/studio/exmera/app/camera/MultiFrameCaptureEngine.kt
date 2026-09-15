package studio.exmera.app.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import studio.exmera.engine.core.MultiFrameBuffer
import studio.exmera.engine.imaging.ComputationalImagingCore
import java.util.concurrent.atomic.AtomicLong

/**
 * Phase 8/9 capture coordinator. CameraX analysis frames are retained only while
 * a computational capture is active; otherwise they are immediately released.
 */
class MultiFrameCaptureEngine(
    capacity: Int = 8,
    private val maxWorkingDimension: Int = 1920
) : AutoCloseable {
    enum class State { IDLE, CAPTURING, COMPLETE, PROCESSING, CLOSED }

    data class CapturedFrame(
        val sequence: Long,
        val timestampNs: Long,
        val image: ImageProxy,
        val width: Int,
        val height: Int,
        val rotationDegrees: Int,
        val format: Int
    ) : AutoCloseable { override fun close() = image.close() }

    data class CaptureResult(val frames: List<CapturedFrame>, val startedAtNs: Long, val completedAtNs: Long)

    private val buffer = MultiFrameBuffer<CapturedFrame>(capacity)
    private val sequence = AtomicLong(0)
    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()
    private val imagingCore = ComputationalImagingCore()
    private var targetCount = 0
    private var startedAtNs = 0L

    @Synchronized fun start(targetFrames: Int = 5) {
        check(_state.value != State.CLOSED) { "Capture engine is closed" }
        require(targetFrames in 2..16) { "targetFrames must be between 2 and 16" }
        buffer.drain().forEach(CapturedFrame::close)
        targetCount = targetFrames; startedAtNs = System.nanoTime(); _state.value = State.CAPTURING
    }

    /** Takes ownership of the ImageProxy only when the capture is active. */
    @Synchronized fun accept(image: ImageProxy): Boolean {
        if (_state.value != State.CAPTURING) { image.close(); return false }
        val captured = CapturedFrame(sequence.getAndIncrement(), image.imageInfo.timestamp, image,
            image.width, image.height, image.imageInfo.rotationDegrees, image.format)
        if (!buffer.offer(captured)) return false
        if (buffer.size() >= targetCount) _state.value = State.COMPLETE
        return true
    }

    @Synchronized fun result(): CaptureResult? {
        if (_state.value != State.COMPLETE) return null
        return CaptureResult(buffer.snapshot(), startedAtNs, System.nanoTime())
    }

    /** Runs the first production CPU reconstruction path and releases all CameraX buffers. */
    @Synchronized fun processToBitmap(): Bitmap? {
        if (_state.value != State.COMPLETE) return null
        _state.value = State.PROCESSING
        val captured = buffer.drain()
        return try {
            val frames = captured.map { ImageProxyImagingAdapter.toImagingFrame(it.image, maxWorkingDimension) }
            val result = imagingCore.process(frames)
            ImageProxyImagingAdapter.toBitmap(result.image)
        } finally {
            captured.forEach(CapturedFrame::close)
            _state.value = State.IDLE
        }
    }

    @Synchronized fun reset() {
        check(_state.value != State.CLOSED) { "Capture engine is closed" }
        buffer.drain().forEach(CapturedFrame::close); _state.value = State.IDLE
    }

    @Synchronized override fun close() {
        if (_state.value == State.CLOSED) return
        buffer.close(); _state.value = State.CLOSED
    }
}
