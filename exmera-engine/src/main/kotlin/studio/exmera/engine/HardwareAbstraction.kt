package studio.exmera.engine

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaCodecList
import android.os.Build

/** Hardware capabilities exposed to the rest of Exmera without Android API coupling. */
data class HardwareCapabilities(
    val cpuCores: Int,
    val ramMb: Long,
    val hasGpu: Boolean,
    val hasNpu: Boolean,
    val cameraCount: Int,
    val maxCameraWidth: Int,
    val maxCameraHeight: Int,
    val maxVideoFps: Int,
    val hevcSupported: Boolean,
    val av1Supported: Boolean,
    val vulkanSupported: Boolean
)

interface HardwareAbstractionLayer {
    fun capabilities(): HardwareCapabilities
}

class AndroidHardwareAbstractionLayer(private val context: Context) : HardwareAbstractionLayer {
    override fun capabilities(): HardwareCapabilities {
        val profile = DeviceProfile.detect(context)
        val cameraManager = context.getSystemService(CameraManager::class.java)
        var cameras = 0
        var maxWidth = 0
        var maxHeight = 0
        var maxFps = 30
        if (cameraManager != null) {
            runCatching {
                for (id in cameraManager.cameraIdList) {
                    cameras++
                    val c = cameraManager.getCameraCharacteristics(id)
                    c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.let { map ->
                        map.getOutputSizes(android.graphics.ImageFormat.JPEG)?.maxByOrNull { it.width.toLong() * it.height }?.let {
                            maxWidth = maxOf(maxWidth, it.width); maxHeight = maxOf(maxHeight, it.height)
                        }
                        map.getOutputMinFrameDuration(android.graphics.ImageFormat.PRIVATE, map.getOutputSizes(android.graphics.ImageFormat.PRIVATE).firstOrNull() ?: return@let)?.let { duration ->
                            if (duration > 0) maxFps = maxOf(maxFps, (1_000_000_000L / duration).toInt())
                        }
                    }
                }
            }
        }
        val codecs = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
        val hevc = codecs.any { it.isEncoder && it.supportedTypes.any { t -> t.equals("video/hevc", true) } }
        val av1 = codecs.any { it.isEncoder && it.supportedTypes.any { t -> t.equals("video/av01", true) } }
        val vulkan = Build.VERSION.SDK_INT >= 24
        return HardwareCapabilities(profile.cpuCores, profile.ramMb, true, profile.hasNeuralAccelerator,
            cameras, maxWidth, maxHeight, maxFps, hevc, av1, vulkan)
    }
}
