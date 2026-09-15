package studio.exmera.engine.core

/** Immutable configuration shared by all Exmera engine subsystems. */
data class EngineConfig(
    val enableCpu: Boolean = true,
    val enableGpu: Boolean = true,
    val enableNpu: Boolean = true,
    val maxInFlightFrames: Int = 3,
    val preferLowLatency: Boolean = true,
    val allowThermalThrottling: Boolean = true
) {
    init {
        require(maxInFlightFrames in 1..8) { "maxInFlightFrames must be between 1 and 8" }
    }
}
