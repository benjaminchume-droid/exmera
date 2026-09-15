package studio.exmera.exm

object ExmValidator {
    fun validate(manifest: ExmFormat.Manifest, androidSdk: Int, ramMb: Long, backend: ExmFormat.Backend): List<String> = buildList {
        if (androidSdk < manifest.minAndroid) add("Android ${manifest.minAndroid}+ required")
        if (ramMb < manifest.minRamMb) add("At least ${manifest.minRamMb} MB RAM required")
        if (backend !in manifest.backends) add("Backend $backend is not supported by this model")
        if (manifest.sha256.length != 64) add("Invalid SHA-256")
    }
}
