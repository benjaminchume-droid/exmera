package studio.exmera.engine.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaGraphTest {
    @Test
    fun graph_preserves_declared_order() {
        val graph = MediaGraph.Builder()
            .add(TestNode("capture", NodeType.CAPTURE))
            .add(TestNode("compute", NodeType.COMPUTE))
            .add(TestNode("render", NodeType.RENDER))
            .build()

        assertEquals(listOf("capture", "compute", "render"), graph.nodeIds)
        assertEquals(3, graph.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun graph_rejects_duplicate_ids() {
        MediaGraph.Builder()
            .add(TestNode("same", NodeType.CAPTURE))
            .add(TestNode("same", NodeType.COMPUTE))
    }

    private class TestNode(
        override val id: String,
        override val type: NodeType
    ) : MediaNode {
        override fun configure(config: EngineConfig) = Unit
        override fun process(input: MediaFrame): MediaFrame = input
        override fun reset() = Unit
    }
}
