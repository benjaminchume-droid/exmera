package studio.exmera.engine

import android.content.Context
import studio.exmera.engine.core.EngineConfig
import studio.exmera.engine.core.EngineState
import studio.exmera.engine.core.MediaFrame
import studio.exmera.engine.core.MediaGraph

/**
 * Top-level owner of Exmera Core. Feature modules depend on this stable boundary,
 * while hardware-specific implementations remain behind subsystem interfaces.
 */
class ExmeraEngine(
    context: Context,
    val config: EngineConfig = EngineConfig()
) {
    val device: DeviceProfile = DeviceProfile.detect(context)
    var state: EngineState = EngineState.CREATED
        private set

    private var graph: MediaGraph? = null

    fun installGraph(mediaGraph: MediaGraph) {
        check(state != EngineState.CLOSED) { "Engine is closed" }
        check(state != EngineState.RUNNING) { "Cannot replace graph while engine is running" }
        mediaGraph.configure(config)
        graph = mediaGraph
        if (state == EngineState.CREATED) state = EngineState.READY
    }

    fun start() {
        check(state == EngineState.READY || state == EngineState.PAUSED) { "Engine is not ready: $state" }
        state = EngineState.RUNNING
    }

    fun pause() {
        check(state == EngineState.RUNNING) { "Engine is not running" }
        state = EngineState.PAUSED
    }

    fun stop() {
        check(state != EngineState.CLOSED) { "Engine is closed" }
        graph?.reset()
        state = EngineState.STOPPED
    }

    fun process(frame: MediaFrame): MediaFrame {
        check(state == EngineState.RUNNING) { "Engine must be running to process frames" }
        return graph?.process(frame) ?: frame
    }

    fun close() {
        if (state == EngineState.CLOSED) return
        graph?.reset()
        graph = null
        state = EngineState.CLOSED
    }

    fun profileName(): String = when (device.tier) {
        DeviceProfile.Tier.LITE -> "Efficient"
        DeviceProfile.Tier.STANDARD -> "Balanced"
        DeviceProfile.Tier.PRO -> "Pro"
        DeviceProfile.Tier.ULTRA -> "Maximum"
    }
}
