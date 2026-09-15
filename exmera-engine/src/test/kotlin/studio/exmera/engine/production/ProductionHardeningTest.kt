package studio.exmera.engine.production

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductionHardeningTest {
    @Test fun integrityHashIsDeterministic() {
        val data="exmera".toByteArray()
        val hash=IntegrityHash.sha256(data)
        assertEquals(64,hash.length)
        assertTrue(IntegrityHash.verify(data,hash))
        assertFalse(IntegrityHash.verify("different".toByteArray(),hash))
    }

    @Test fun thermalPolicyDegradesSafely() {
        val normal=ProcessingBudgetPolicy.choose(12288,ThermalLevel.NOMINAL)
        val critical=ProcessingBudgetPolicy.choose(12288,ThermalLevel.CRITICAL)
        assertTrue(normal.maxWidth>critical.maxWidth)
        assertTrue(normal.maxInFlightFrames>critical.maxInFlightFrames)
        assertFalse(critical.allowHeavyModels)
    }

    @Test fun cancellationIsObservable() {
        val token=CancellationToken()
        assertFalse(token.isCancelled())
        token.cancel()
        assertTrue(token.isCancelled())
    }

    @Test fun quotaNeverGoesNegative() {
        val quota=StorageQuota(1000,1200)
        assertEquals(0,quota.remainingBytes)
        assertFalse(quota.canAllocate(1))
    }

    @Test fun releaseReportRequiresEveryGate() {
        val report=ReleaseGateEvaluator.evaluate("1.0.0",listOf(ReleaseGate("tests",true,"ok"),ReleaseGate("devices",false,"pending")))
        assertFalse(report.passed)
    }
}
