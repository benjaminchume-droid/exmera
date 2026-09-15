package studio.exmera.engine.core

/** Lifecycle state of the Exmera processing engine. */
enum class EngineState {
    CREATED,
    READY,
    RUNNING,
    PAUSED,
    STOPPED,
    CLOSED
}
