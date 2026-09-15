package studio.exmera.exm

/** Versioned EXM container primitives. Binary encoding is intentionally kept deterministic. */
object ExmFormat {
    const val MAGIC = "EXM1"
    const val VERSION = 1
    const val HEADER_SIZE = 32

    enum class Quantization { FP32, FP16, INT8, INT4 }
    enum class Backend { CPU, GPU, NPU }

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
        val license: String = "UNKNOWN"
    )
}
