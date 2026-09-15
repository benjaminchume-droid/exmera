package studio.exmera.exm

/**
 * Compiler boundary for future ONNX/TFLite/PyTorch conversion.
 * Phase 3-5 intentionally keeps model transformation separate from the runtime.
 */
data class ExmCompileOptions(
    val quantization: ExmFormat.Quantization = ExmFormat.Quantization.INT8,
    val backends: Set<ExmFormat.Backend> = setOf(ExmFormat.Backend.CPU),
    val minRamMb: Int = 2048,
    val minAndroid: Int = 26,
    val license: String = "UNKNOWN"
)

class ExmPackCompiler {
    fun compile(
        id: String,
        version: String,
        name: String,
        sourcePayload: ByteArray,
        options: ExmCompileOptions
    ): ByteArray {
        // This is a packaging compiler today. Quantization/pruning passes plug in here later.
        val manifest = ExmFormat.Manifest(
            id = id, version = version, name = name,
            quantization = options.quantization, backends = options.backends,
            sha256 = ExmReader.sha256(sourcePayload), sizeBytes = sourcePayload.size.toLong(),
            minRamMb = options.minRamMb, minAndroid = options.minAndroid, license = options.license
        )
        return java.io.ByteArrayOutputStream().also { ExmContainer.write(it, manifest, sourcePayload) }.toByteArray()
    }
}
