package studio.exmera.exm

data class ExmDeviceProfile(
    val ramMb: Int,
    val androidApi: Int,
    val cpuAvailable: Boolean = true,
    val gpuAvailable: Boolean = false,
    val npuAvailable: Boolean = false,
    val thermalThrottled: Boolean = false
)

data class ExmExecutionPlan(
    val backend: ExmFormat.Backend,
    val quantization: ExmFormat.Quantization,
    val loadAllowed: Boolean,
    val reason: String
)

object ExmExecutionPlanner {
    fun plan(manifest: ExmFormat.Manifest, device: ExmDeviceProfile): ExmExecutionPlan {
        require(device.ramMb >= 0)
        if (device.androidApi < manifest.minAndroid) return denied("Android API ${device.androidApi} < ${manifest.minAndroid}", manifest)
        if (device.ramMb < manifest.minRamMb) return denied("RAM ${device.ramMb}MB < ${manifest.minRamMb}MB", manifest)

        val candidates = when {
            !device.thermalThrottled && device.npuAvailable -> listOf(ExmFormat.Backend.NPU, ExmFormat.Backend.GPU, ExmFormat.Backend.CPU)
            device.gpuAvailable -> listOf(ExmFormat.Backend.GPU, ExmFormat.Backend.CPU, ExmFormat.Backend.NPU)
            else -> listOf(ExmFormat.Backend.CPU, ExmFormat.Backend.GPU, ExmFormat.Backend.NPU)
        }
        val selected = candidates.firstOrNull { backendAvailable(it, manifest, device) }
            ?: return denied("No compatible execution backend", manifest)
        val thermalNote = if (device.thermalThrottled) "; thermal state is throttled" else ""
        return ExmExecutionPlan(selected, manifest.quantization, true, "Selected precompiled ${manifest.quantization} variant on ${selected.name}$thermalNote")
    }

    private fun backendAvailable(backend: ExmFormat.Backend, manifest: ExmFormat.Manifest, device: ExmDeviceProfile): Boolean =
        backend in manifest.backends && when (backend) {
            ExmFormat.Backend.CPU -> device.cpuAvailable
            ExmFormat.Backend.GPU -> device.gpuAvailable
            ExmFormat.Backend.NPU -> device.npuAvailable
        }

    private fun denied(reason: String, manifest: ExmFormat.Manifest) =
        ExmExecutionPlan(ExmFormat.Backend.CPU, manifest.quantization, false, reason)
}
