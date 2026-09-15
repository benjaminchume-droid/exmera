package studio.exmera.app.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Immutable capabilities discovered from the physical camera and Android camera stack. */
data class CameraCapabilities(
    val cameraId: String,
    val facing: Int,
    val sensorWidth: Int,
    val sensorHeight: Int,
    val maxDigitalZoom: Float,
    val supportsRaw: Boolean,
    val supportsYuv: Boolean,
    val supportsJpeg: Boolean,
    val supportsManualExposure: Boolean,
    val supportsManualFocus: Boolean,
    val supportsOis: Boolean,
    val supportsHdr: Boolean,
    val availableFps: List<Int>,
    val activeArrayWidth: Int,
    val activeArrayHeight: Int
)

data class CameraFrameMetadata(
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val format: Int,
    val rotationDegrees: Int,
    val exposureTimeNs: Long?,
    val sensitivityIso: Int?,
    val focalLengthMm: Float?,
    val aperture: Float?
)

/** Lightweight frame envelope. The ImageProxy remains owned by the consumer until close(). */
data class CameraFrame(
    val image: ImageProxy,
    val metadata: CameraFrameMetadata
)

class CameraCapabilityRepository(private val context: Context) {
    fun discover(facing: Int = CameraCharacteristics.LENS_FACING_BACK): CameraCapabilities? {
        val manager = context.getSystemService(CameraManager::class.java) ?: return null
        val id = manager.cameraIdList.firstOrNull { cameraId ->
            manager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.LENS_FACING) == facing
        } ?: return null
        val c = manager.getCameraCharacteristics(id)
        val map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return null
        val active = c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val pixelArray = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val fpsRanges = c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?.map { it.upper }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
        val formats = map.outputFormats.toSet()
        val capabilities = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toSet().orEmpty()
        return CameraCapabilities(
            cameraId = id,
            facing = facing,
            sensorWidth = pixelArray?.width ?: 0,
            sensorHeight = pixelArray?.height ?: 0,
            maxDigitalZoom = c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f,
            supportsRaw = Build.VERSION.SDK_INT >= 21 &&
                ImageFormat.RAW_SENSOR in formats &&
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW in capabilities,
            supportsYuv = ImageFormat.YUV_420_888 in formats,
            supportsJpeg = ImageFormat.JPEG in formats,
            supportsManualExposure = c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
                ?.contains(CameraCharacteristics.CONTROL_AE_MODE_OFF) == true,
            supportsManualFocus = c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                ?.contains(CameraCharacteristics.CONTROL_AF_MODE_OFF) == true,
            supportsOis = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                ?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) == true,
            supportsHdr = CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES.let { key ->
                c.get(key)?.contains(CameraCharacteristics.CONTROL_SCENE_MODE_HDR) == true
            },
            availableFps = fpsRanges,
            activeArrayWidth = active?.width ?: 0,
            activeArrayHeight = active?.height ?: 0
        )
    }
}

class ExmeraCameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private val _capabilities = MutableStateFlow<CameraCapabilities?>(null)
    val capabilities: StateFlow<CameraCapabilities?> = _capabilities.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var analysis: ImageAnalysis? = null
    private var capture: ImageCapture? = null
    private val executor: ExecutorService = Executors.newFixedThreadPool(2)

    fun bind(
        previewView: PreviewView,
        onFrame: ((CameraFrame) -> Unit)? = null
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val p = future.get()
            provider = p
            val capability = CameraCapabilityRepository(context).discover()
            _capabilities.value = capability

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
            imageAnalysis.setAnalyzer(executor) { proxy ->
                val rotation = proxy.imageInfo.rotationDegrees
                val frame = CameraFrame(
                    proxy,
                    CameraFrameMetadata(
                        timestampNs = proxy.imageInfo.timestamp,
                        width = proxy.width,
                        height = proxy.height,
                        format = proxy.format,
                        rotationDegrees = rotation,
                        exposureTimeNs = null,
                        sensitivityIso = null,
                        focalLengthMm = null,
                        aperture = null
                    )
                )
                if (onFrame != null) onFrame(frame) else proxy.close()
            }
            analysis = imageAnalysis
            capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            p.unbindAll()
            camera = p.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis,
                capture
            )
            _isReady.value = true
        }, ContextCompat.getMainExecutor(context))
    }

    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun setLinearZoom(value: Float) {
        camera?.cameraControl?.setLinearZoom(value.coerceIn(0f, 1f))
    }

    fun shutdown() {
        provider?.unbindAll()
        analysis?.clearAnalyzer()
        analysis = null
        capture = null
        camera = null
        _isReady.value = false
        executor.shutdownNow()
    }
}
