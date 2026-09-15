package studio.exmera.engine.core

/** Lightweight frame descriptor passed through the media graph. Pixel storage stays outside the descriptor. */
data class MediaFrame(
    val id: Long,
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val format: PixelFormat,
    val rotationDegrees: Int = 0,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(width > 0 && height > 0) { "Frame dimensions must be positive" }
        require(rotationDegrees % 90 == 0) { "rotationDegrees must be a multiple of 90" }
    }
}

enum class PixelFormat {
    YUV_420,
    RGBA_8888,
    RGB_10_BIT,
    RAW_SENSOR,
    DEPTH_16,
    UNKNOWN
}
