package studio.exmera.exm

/** Versioned EXM container primitives. Binary encoding is deterministic and self-describing. */
object ExmFormat {
    const val MAGIC = "EXM1"
    const val VERSION = 2
    const val MAX_STRING_BYTES = 1_048_576
    const val MAX_PAYLOAD_BYTES = 512L * 1024L * 1024L

    enum class Quantization { FP32, FP16, INT8, INT4 }
    enum class Backend { CPU, GPU, NPU }
    enum class Compression { NONE, DEFLATE }

    data class Manifest(
        val id: String,
        val version: String,
        val name: String,
        val quantization: Quantization,
        val backends: Set<Backend>,
        val sha256: String,
        val sizeBytes: Long,
        val minRamMb: Int = 2048,
        val minAndroid: Int = 26,
        val license: String = "UNKNOWN",
        val compression: Compression = Compression.NONE,
        val originalSizeBytes: Long = sizeBytes
    )
}
