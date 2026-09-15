package studio.exmera.engine.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiFrameBufferTest {
    private class TestFrame(val id: Int, private val onClose: () -> Unit) : AutoCloseable {
        override fun close() = onClose()
    }

    @Test
    fun buffer_evicts_oldest_and_closes_it() {
        val closed = mutableListOf<Int>()
        val buffer = MultiFrameBuffer<TestFrame>(2)
        buffer.offer(TestFrame(1) { closed += 1 })
        buffer.offer(TestFrame(2) { closed += 2 })
        buffer.offer(TestFrame(3) { closed += 3 })

        assertEquals(listOf(2, 3), buffer.snapshot().map { it.id })
        assertEquals(listOf(1), closed)
        buffer.close()
        assertEquals(listOf(1, 2, 3), closed)
    }

    @Test
    fun closed_buffer_rejects_and_closes_new_frame() {
        var closed = false
        val buffer = MultiFrameBuffer<TestFrame>(1)
        buffer.close()
        assertFalse(buffer.offer(TestFrame(1) { closed = true }))
        assertTrue(closed)
    }

    @Test
    fun selector_prefers_sharp_low_motion_frames() {
        val infos = listOf(
            FrameSelectionInfo(1, 1, sharpnessScore = 0.4f, motionScore = 0.1f, exposureScore = 1f),
            FrameSelectionInfo(2, 2, sharpnessScore = 1f, motionScore = 0.1f, exposureScore = 1f),
            FrameSelectionInfo(3, 3, sharpnessScore = 0.8f, motionScore = 5f, exposureScore = 1f)
        )
        assertEquals(2L, MultiFrameSelector.select(infos, 1).single().sequence)
    }
}
