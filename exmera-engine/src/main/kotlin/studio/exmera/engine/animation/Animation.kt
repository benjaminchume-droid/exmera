package studio.exmera.engine.animation

import kotlin.math.max

/** Phase 19: deterministic keyframe animation with interpolation and looping. */
enum class Interpolation{STEP,LINEAR,EASE_IN_OUT}
data class Keyframe<T>(val timeUs:Long,val value:T,val interpolation:Interpolation=Interpolation.LINEAR){init{require(timeUs>=0)}}
class FloatTrack(private val keys:List<Keyframe<Float>>){init{require(keys.isNotEmpty());require(keys.zipWithNext().all{it.first.timeUs<=it.second.timeUs})}
 fun valueAt(timeUs:Long):Float{if(timeUs<=keys.first().timeUs)return keys.first().value;if(timeUs>=keys.last().timeUs)return keys.last().value;val i=keys.indexOfLast{it.timeUs<=timeUs}.coerceAtLeast(0);val a=keys[i];val b=keys[i+1];val t=((timeUs-a.timeUs).toFloat()/max(1L,b.timeUs-a.timeUs)).coerceIn(0f,1f);val e=when(a.interpolation){Interpolation.STEP->0f;Interpolation.LINEAR->t;Interpolation.EASE_IN_OUT->t*t*(3f-2f*t)};return a.value+(b.value-a.value)*e}}
data class AnimatedTransform(val x:Float,val y:Float,val scale:Float,val rotation:Float,val opacity:Float)
class TransformAnimation(val x:FloatTrack,val y:FloatTrack,val scale:FloatTrack,val rotation:FloatTrack,val opacity:FloatTrack){fun valueAt(timeUs:Long)=AnimatedTransform(x.valueAt(timeUs),y.valueAt(timeUs),scale.valueAt(timeUs),rotation.valueAt(timeUs),opacity.valueAt(timeUs))}
