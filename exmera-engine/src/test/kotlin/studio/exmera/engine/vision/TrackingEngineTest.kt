package studio.exmera.engine.vision

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackingEngineTest {
    private fun detection(x: Float, y: Float, type: VisionObjectType = VisionObjectType.PERSON, confidence: Float = .9f) =
        VisionDetection(0L, type, VisionRect(x, y, x + .2f, y + .2f), confidence)

    @Test fun `track id survives movement`() {
        val engine = TrackingEngine()
        val first = engine.update(listOf(detection(.1f, .1f)))
        val id = first.single().id
        engine.update(listOf(detection(.12f, .11f)))
        val third = engine.update(listOf(detection(.15f, .13f))).single()
        assertEquals(id, third.id)
        assertEquals(TrackState.TRACKED, third.state)
        assertTrue(third.velocityX > 0f)
    }

    @Test fun `short occlusion preserves track and decays confidence`() {
        val engine = TrackingEngine(TrackingConfig(maxMissingFrames = 3, reidentificationFrames = 8))
        val id = engine.update(listOf(detection(.2f, .2f))).single().id
        val missing = engine.update(emptyList()).single()
        assertEquals(id, missing.id)
        assertEquals(TrackState.OCCLUDED, missing.state)
        assertTrue(missing.confidence < .9f)
        val recovered = engine.update(listOf(detection(.21f, .2f))).single()
        assertEquals(id, recovered.id)
        assertEquals(TrackState.TRACKED, recovered.state)
    }

    @Test fun `lost track is removed after reidentification window`() {
        val engine = TrackingEngine(TrackingConfig(maxMissingFrames = 1, reidentificationFrames = 2))
        engine.update(listOf(detection(.1f, .1f)))
        engine.update(emptyList())
        val lost = engine.update(emptyList())
        assertTrue(lost.isNotEmpty())
        assertEquals(TrackState.LOST, lost.single().state)
        assertTrue(engine.update(emptyList()).isEmpty())
    }

    @Test fun `class aware association avoids incompatible classes`() {
        val engine = TrackingEngine()
        val person = engine.update(listOf(detection(.1f, .1f, VisionObjectType.PERSON))).single()
        val objectTrack = engine.update(listOf(detection(.1f, .1f, VisionObjectType.OBJECT))).single()
        assertTrue(objectTrack.id != person.id)
    }

    @Test fun `coordinate transform maps points and bounds`() {
        val transform = TrackingTransform(scaleX = .5f, scaleY = .5f, offsetX = .1f, offsetY = .2f)
        val point = TrackingCoordinateMapper.map(TrackingPoint(.4f, .6f), transform)
        assertEquals(.3f, point.x)
        assertEquals(.5f, point.y)
        val rect = TrackingCoordinateMapper.map(VisionRect(.2f, .2f, .6f, .6f), transform)
        assertEquals(.2f, rect.left)
        assertEquals(.3f, rect.top)
        assertEquals(.4f, rect.right)
        assertEquals(.5f, rect.bottom)
    }
}
