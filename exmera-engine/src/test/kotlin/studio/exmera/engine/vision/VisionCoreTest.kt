package studio.exmera.engine.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.exmera.engine.imaging.RgbImage

class VisionCoreTest {
    private fun image(w:Int=32,h:Int=24):RgbImage{val p=FloatArray(w*h*3);for(y in 0 until h)for(x in 0 until w){val i=(y*w+x)*3;val v=if(x in 10..21&&y in 6..18).8f else .18f;p[i]=v;p[i+1]=v*.9f;p[i+2]=v*.8f};return RgbImage(w,h,p)}
    @Test fun qualityIsBounded(){val q=VisionQualityAnalyzer.analyze(image());assertTrue(q.sharpness in 0f..1f);assertTrue(q.exposure in 0f..1f);assertTrue(q.contrast in 0f..1f);assertTrue(q.noise in 0f..1f);assertTrue(q.overall in 0f..1f)}
    @Test fun depthAndMaskMatchImage(){val i=image(11,9);val d=RelativeDepthEstimator.estimate(i);val s=SaliencySegmenter.segment(i);assertEquals(99,d.values.size);assertEquals(99,s.alpha.size);assertTrue(d.values.all{it in 0f..1f});assertTrue(s.alpha.all{it in 0f..1f})}
    @Test fun trackerKeepsStableId(){val t=VisionTracker();val a=VisionDetection(0,VisionObjectType.UNKNOWN,VisionRect(.2f,.2f,.4f,.5f),.9f);val b=VisionDetection(0,VisionObjectType.UNKNOWN,VisionRect(.21f,.21f,.41f,.51f),.9f);val first=t.update(listOf(a));val second=t.update(listOf(b));assertEquals(first.single().id,second.single().id)}
    @Test fun backendProducesDetectionsForHighContrastRegion(){val r=CpuSaliencyBackend(.1f).detect(VisionFrame(image(),1L));assertTrue(r.isNotEmpty());assertTrue(r.all{it.confidence in 0f..1f})}
    @Test fun engineUsesTrackerAndAnalysis(){val e=VisionEngine();val frame=VisionFrame(image(),1L);assertTrue(e.detect(frame).all{it.bounds.left>=0f});assertTrue(e.quality(frame.image).overall in 0f..1f)}
}
