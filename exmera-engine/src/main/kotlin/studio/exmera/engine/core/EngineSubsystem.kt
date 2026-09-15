package studio.exmera.engine.core

/** Shared boundary for the three primary Exmera engine domains. */
interface EngineSubsystem {
    val name: String
    fun start()
    fun stop()
}

interface CaptureSubsystem : EngineSubsystem {
    override val name: String get() = "Capture"
    fun submit(frame: MediaFrame)
}

interface ComputeSubsystem : EngineSubsystem {
    override val name: String get() = "Compute"
    fun process(frame: MediaFrame): MediaFrame
}

interface RenderSubsystem : EngineSubsystem {
    override val name: String get() = "Render"
    fun render(frame: MediaFrame)
}
