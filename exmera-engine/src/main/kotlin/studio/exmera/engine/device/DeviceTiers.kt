package studio.exmera.engine.device

enum class DeviceTier{EFFICIENT,BALANCED,PRO,MAXIMUM,EVERYTHING}
data class DeviceScore(val ramMb:Long,val cpuCores:Int,val gpu:Boolean,val npu:Boolean,val cameraPixels:Long,val sustainedScore:Float)
object DeviceTierClassifier{fun classify(s:DeviceScore):DeviceTier{val score=(s.ramMb/2048f)+(s.cpuCores/4f)+(if(s.gpu)1f else 0f)+(if(s.npu)1.5f else 0f)+(s.cameraPixels/12_000_000f).coerceAtMost(2f)+s.sustainedScore;return when{score>=10f->DeviceTier.EVERYTHING;score>=7f->DeviceTier.MAXIMUM;score>=5f->DeviceTier.PRO;score>=3f->DeviceTier.BALANCED;else->DeviceTier.EFFICIENT}}}
data class CapabilityBudget(val previewDimension:Int,val inferenceThreads:Int,val temporalFrames:Int,val enableLearnedModels:Boolean,val maxConcurrentJobs:Int)
object TierBudget{fun forTier(t:DeviceTier)=when(t){DeviceTier.EFFICIENT->CapabilityBudget(720,2,3,false,1);DeviceTier.BALANCED->CapabilityBudget(1080,3,5,true,1);DeviceTier.PRO->CapabilityBudget(1440,4,7,true,2);DeviceTier.MAXIMUM->CapabilityBudget(2160,6,9,true,3);DeviceTier.EVERYTHING->CapabilityBudget(3840,8,12,true,4)}}
