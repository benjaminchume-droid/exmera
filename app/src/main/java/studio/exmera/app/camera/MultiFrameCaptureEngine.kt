package studio.exmera.app.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import studio.exmera.engine.core.MultiFrameBuffer
import studio.exmera.engine.imaging.ComputationalImagingCore
import studio.exmera.engine.imaging.SuperResolutionConfig
import studio.exmera.engine.imaging.SuperResolutionEngine
import studio.exmera.engine.imaging.SuperResolutionScale
import java.util.concurrent.atomic.AtomicLong

/** Phase 8-10 capture coordinator: capture -> align/fuse -> super-resolve. */
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
    private val superResolution = SuperResolutionEngine()
    private var targetCount = 0
    private var startedAtNs = 0L

    @Synchronized fun start(targetFrames: Int = 5) {
        check(_state.value != State.CLOSED) { "Capture engine is closed" }
        require(targetFrames in 2..16) { "targetFrames must be between 2 and 16" }
        buffer.drain().forEach(CapturedFrame::close)
        targetCount = targetFrames
        startedAtNs = System.nanoTime()
        _state.value = State.CAPTURING
    }

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

    /** Reconstructs and optionally super-resolves the captured frames on a worker thread. */
    @Synchronized fun processToBitmap(scale: SuperResolutionScale = SuperResolutionScale.X2): Bitmap? {
        if (_state.value != State.COMPLETE) return null
        _state.value = State.PROCESSING
        val captured = buffer.drain()
        return try {
            val frames = captured.map { ImageProxyImagingAdapter.toImagingFrame(it.image, maxWorkingDimension) }
            val fused = imagingCore.process(frames)
            val resolved = superResolution.process(
                fused.image,
                SuperResolutionConfig(scale = scale, tileSize = 256, overlap = 16, sharpen = 0.22f)
            )
            ImageProxyImagingAdapter.toBitmap(resolved.image)
        } finally {
            captured.forEach(CapturedFrame::close)
            _state.value = State.IDLE
        }
    }

    @Synchronized fun reset() {
        check(_state.value != State.CLOSED) { "Capture engine is closed" }
        buffer.drain().forEach(CapturedFrame::close)
        _state.value = State.IDLE
    }

    @Synchronized override fun close() {
        if (_state.value == State.CLOSED) return
        buffer.close()
        _state.value = State.CLOSED
    }
}
