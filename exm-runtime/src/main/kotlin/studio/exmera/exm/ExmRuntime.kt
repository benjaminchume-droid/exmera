package studio.exmera.exm

import java.io.InputStream

interface ExmBackend {
    fun supports(manifest: ExmFormat.Manifest): Boolean
    fun load(payload: ByteArray, manifest: ExmFormat.Manifest): LoadedExm
}

data class LoadedExm(val manifest: ExmFormat.Manifest, val payload: ByteArray)

class ExmRuntime(private val backends: List<ExmBackend> = emptyList()) {
    fun load(input: InputStream): LoadedExm {
        val (manifest, payload) = ExmContainer.read(input)
        require(backends.isEmpty() || backends.any { it.supports(manifest) }) { "No compatible EXM backend" }
        val backend = backends.firstOrNull { it.supports(manifest) }
        return backend?.load(payload, manifest) ?: LoadedExm(manifest, payload)
    }
}
