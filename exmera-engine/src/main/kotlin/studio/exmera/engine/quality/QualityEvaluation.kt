package studio.exmera.engine.quality

import studio.exmera.engine.imaging.RgbImage
import studio.exmera.engine.vision.VisionQualityAnalyzer

data class QualityThresholds(val minimumOverall:Float=.35f,val minimumSharpness:Float=.15f)
data class QualityGateResult(val passed:Boolean,val score:Float,val reasons:List<String>)
object QualityEvaluation{fun evaluate(image:RgbImage,thresholds:QualityThresholds=QualityThresholds()):QualityGateResult{val q=VisionQualityAnalyzer.analyze(image);val r=buildList{if(q.overall<thresholds.minimumOverall)add("overall quality below threshold");if(q.sharpness<thresholds.minimumSharpness)add("sharpness below threshold")};return QualityGateResult(r.isEmpty(),q.overall,r)}}
