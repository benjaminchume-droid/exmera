package studio.exmera.engine.core

/**
 * Ordered media-processing graph. Phase 1 intentionally keeps execution synchronous;
 * scheduling and zero-copy buffers can be introduced without changing this contract.
 */
class MediaGraph private constructor(
    private val nodes: List<MediaNode>
) {
    val size: Int get() = nodes.size
    val nodeIds: List<String> get() = nodes.map { it.id }

    fun configure(config: EngineConfig) {
        nodes.forEach { it.configure(config) }
    }

    fun process(input: MediaFrame): MediaFrame = nodes.fold(input) { frame, node -> node.process(frame) }

    fun reset() = nodes.forEach(MediaNode::reset)

    class Builder {
        private val nodes = mutableListOf<MediaNode>()

        fun add(node: MediaNode): Builder {
            require(nodes.none { it.id == node.id }) { "Duplicate media node id: ${node.id}" }
            nodes += node
            return this
        }

        fun build(): MediaGraph = MediaGraph(nodes.toList())
    }
}
