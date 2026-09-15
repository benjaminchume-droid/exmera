package studio.exmera.engine.production

enum class ReleaseStage{INTERNAL,BETA,RELEASE}
data class ReleaseGate(val name:String,val passed:Boolean,val blocking:Boolean=true,val detail:String="")
data class ReleaseReport(val stage:ReleaseStage,val gates:List<ReleaseGate>){val passed get()=gates.none{it.blocking&&!it.passed}}
class ReleaseGateEvaluator{fun evaluate(stage:ReleaseStage,gates:List<ReleaseGate>):ReleaseReport=ReleaseReport(stage,gates)}
object HardeningDefaults{const val MAX_EXPORT_PIXELS:Long=40_000_000;const val MAX_PROJECT_CLIPS=1000;const val MAX_EFFECTS_PER_FRAME=64;const val MAX_MODEL_FILE_BYTES:Long=2L*1024*1024*1024}
