package studio.exmera.engine.production

import java.security.MessageDigest
import kotlin.math.max

data class ProcessingBudget(val maxWidth:Int,val maxHeight:Int,val maxInFlightFrames:Int,val maxWorkingMemoryMb:Int,val allowHeavyModels:Boolean){init{require(maxWidth>0&&maxHeight>0);require(maxInFlightFrames in 1..32);require(maxWorkingMemoryMb>=128)}}
enum class ThermalLevel{NOMINAL,LIGHT,MODERATE,SEVERE,CRITICAL}
enum class ProcessingMode{MAXIMUM,BALANCED,EFFICIENT}
object ProcessingBudgetPolicy{
 fun choose(ramMb:Long,thermal:ThermalLevel,requested:ProcessingMode=ProcessingMode.BALANCED):ProcessingBudget{
  val base=when{ramMb>=12288->ProcessingBudget(4096,4096,12,1024,true);ramMb>=8192->ProcessingBudget(3072,3072,10,768,true);ramMb>=6144->ProcessingBudget(2560,2560,8,512,requested!=ProcessingMode.EFFICIENT);else->ProcessingBudget(1920,1920,6,384,false)}
  return when(thermal){ThermalLevel.NOMINAL->base;ThermalLevel.LIGHT->base.copy(maxInFlightFrames=max(3,base.maxInFlightFrames-1));ThermalLevel.MODERATE->base.copy(maxInFlightFrames=max(3,base.maxInFlightFrames/2),allowHeavyModels=false);ThermalLevel.SEVERE->base.copy(maxWidth=minOf(base.maxWidth,1920),maxHeight=minOf(base.maxHeight,1920),maxInFlightFrames=2,maxWorkingMemoryMb=minOf(base.maxWorkingMemoryMb,384),allowHeavyModels=false);ThermalLevel.CRITICAL->ProcessingBudget(1280,1280,1,256,false)}
 }
}
class CancellationToken{@Volatile private var cancelled=false;fun cancel(){cancelled=true};fun isCancelled()=cancelled;fun throwIfCancelled(){if(cancelled)throw ProcessingCancelledException()}}
class ProcessingCancelledException:RuntimeException("Processing cancelled")
object IntegrityHash{fun sha256(bytes:ByteArray):String=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)};fun verify(bytes:ByteArray,expected:String)=expected.length==64&&sha256(bytes).equals(expected,ignoreCase=true)}
data class StorageQuota(val limitBytes:Long,val usedBytes:Long){init{require(limitBytes>=0&&usedBytes>=0)};val remainingBytes get()=(limitBytes-usedBytes).coerceAtLeast(0);fun canAllocate(bytes:Long)=bytes>=0&&bytes<=remainingBytes}
data class ReleaseGate(val name:String,val passed:Boolean,val detail:String)
data class ReleaseReport(val version:String,val gates:List<ReleaseGate>){val passed get()=gates.all{it.passed};fun requirePassed(){check(passed){"Release gates failed: ${gates.filterNot{it.passed}.joinToString{it.name}}"}}}
object ReleaseGateEvaluator{fun evaluate(version:String,gates:List<ReleaseGate>)=ReleaseReport(version,gates)}
