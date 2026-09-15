package studio.exmera.engine.core

/** A processing unit in the Exmera media graph. */
interface MediaNode {
    val id: String
    val type: NodeType
    fun configure(config: EngineConfig)
    fun process(input: MediaFrame): MediaFrame
    fun reset()
}

enum class NodeType {
    CAPTURE,
    COMPUTE,
    RENDER,
    ENHANCE,
    TRACKING,
    EXPORT
}
