package studio.exmera.exm

/** Small registry descriptor used by the distribution/cache layer. */
data class ModelDescriptor(
    val id: String,
    val version: String,
    val displayName: String,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val requiredEngineVersion: String,
    val minAndroid: Int,
    val minRamMb: Int,
    val backends: Set<ExmFormat.Backend>
)
