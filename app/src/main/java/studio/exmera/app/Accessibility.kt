package studio.exmera.app

/** Phase 29: shared accessibility semantics for camera/editor controls. */
data class AccessibleAction(val label:String,val hint:String="",val enabled:Boolean=true)
object ExmeraAccessibility{fun cameraAction(label:String,enabled:Boolean=true)=AccessibleAction(label,"Double tap to activate",enabled);fun progressLabel(stage:String,percent:Int)=AccessibleAction("$stage, $percent percent complete")}
