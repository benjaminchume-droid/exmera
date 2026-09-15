package studio.exmera.engine.performance

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

data class PerformanceSample(val stage:String,val elapsedNs:Long,val bytesAllocated:Long=0)
data class PerformanceStats(val stage:String,val samples:Int,val averageNs:Long,val p95Ns:Long,val peakBytes:Long)
class PerformanceProfiler{private val data=ConcurrentHashMap<String,MutableList<PerformanceSample>>();fun record(sample:PerformanceSample){data.computeIfAbsent(sample.stage){mutableListOf()}.add(sample)};fun stats(stage:String):PerformanceStats{val s=data[stage].orEmpty().sortedBy{it.elapsedNs};if(s.isEmpty())return PerformanceStats(stage,0,0,0,0);return PerformanceStats(stage,s.size,s.sumOf{it.elapsedNs}/s.size,s[((s.size-1)*.95).toInt()],s.maxOf{it.bytesAllocated})}}
data class ThermalBudget(val maxFrameWorkMs:Float,val maxConcurrentJobs:Int,val reduceQuality:Boolean)
class AdaptivePerformanceController{fun budget(thermalLevel:Int,memoryPressure:Boolean):ThermalBudget{val t=thermalLevel.coerceIn(0,5);return ThermalBudget(max(4f,33f-t*5f),if(memoryPressure)1 else 2,t>=3||memoryPressure)}}
