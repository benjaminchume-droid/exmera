package studio.exmera.exm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExmExecutionPlannerTest {
    private fun manifest(backends: Set<ExmFormat.Backend>) = ExmFormat.Manifest(
        id = "vision", version = "1.0", name = "Vision", quantization = ExmFormat.Quantization.INT8,
        backends = backends, sha256 = "0".repeat(64), sizeBytes = 1, minRamMb = 2048
    )

    @Test
    fun prefersNpuWhenAvailable() {
        val plan = ExmExecutionPlanner.plan(manifest(setOf(ExmFormat.Backend.CPU, ExmFormat.Backend.NPU)), ExmDeviceProfile(8192, 35, npuAvailable = true))
        assertTrue(plan.loadAllowed)
        assertEquals(ExmFormat.Backend.NPU, plan.backend)
    }

    @Test
    fun rejectsInsufficientRam() {
        val plan = ExmExecutionPlanner.plan(manifest(setOf(ExmFormat.Backend.CPU)), ExmDeviceProfile(1024, 35))
        assertFalse(plan.loadAllowed)
    }
}
