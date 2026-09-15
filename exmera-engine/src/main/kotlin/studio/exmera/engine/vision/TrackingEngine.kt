package studio.exmera.engine.vision

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class TrackingCoordinateSpace { NORMALIZED, VIEW, IMAGE }
data class TrackingPoint(val x: Float, val y: Float)
data class TrackingTransform(val scaleX: Float=1f,val scaleY: Float=1f,val offsetX: Float=0f,val offsetY: Float=0f){
 fun map(p:TrackingPoint)=TrackingPoint(p.x*scaleX+offsetX,p.y*scaleY+offsetY)
 fun map(r:VisionRect):VisionRect{val a=map(TrackingPoint(r.left,r.top));val b=map(TrackingPoint(r.right,r.bottom));return VisionRect(min(a.x,b.x).coerceIn(0f,1f),min(a.y,b.y).coerceIn(0f,1f),max(a.x,b.x).coerceIn(0f,1f),max(a.y,b.y).coerceIn(0f,1f))}}

enum class TrackState{TENTATIVE,TRACKED,OCCLUDED,LOST,REMOVED}
data class TrackingConfig(val maxMissingFrames:Int=12,val minHitsToConfirm:Int=2,val iouThreshold:Float=.12f,val maxAssociationDistance:Float=.25f,val velocitySmoothing:Float=.65f,val accelerationSmoothing:Float=.45f,val confidenceDecay:Float=.90f,val reidentificationFrames:Int=30){init{require(maxMissingFrames>=1&&minHitsToConfirm>=1);require(iouThreshold in 0f..1f&&maxAssociationDistance>0f);require(velocitySmoothing in 0f..1f&&accelerationSmoothing in 0f..1f&&confidenceDecay in 0f..1f);require(reidentificationFrames>=maxMissingFrames)}}
data class TrackSnapshot(val id:Long,val type:VisionObjectType,val bounds:VisionRect,val predictedBounds:VisionRect,val velocityX:Float,val velocityY:Float,val accelerationX:Float,val accelerationY:Float,val confidence:Float,val ageFrames:Int,val hits:Int,val missingFrames:Int,val state:TrackState)

class TrackingEngine(private val config:TrackingConfig=TrackingConfig()){
 private data class Track(val id:Long,var type:VisionObjectType,var bounds:VisionRect,var velocityX:Float=0f,var velocityY:Float=0f,var accelerationX:Float=0f,var accelerationY:Float=0f,var confidence:Float=0f,var ageFrames:Int=0,var hits:Int=0,var missingFrames:Int=0,var state:TrackState=TrackState.TENTATIVE)
 private val tracks=ArrayList<Track>();private var nextId=1L
 @Synchronized fun update(detections:List<VisionDetection>,deltaTimeSeconds:Float=1f/30f):List<TrackSnapshot>{require(deltaTimeSeconds>0f&&deltaTimeSeconds.isFinite());val dt=deltaTimeSeconds.coerceIn(1f/240f,1f);val predicted=tracks.map{predict(it,dt)};val assignments=associate(predicted,detections);val used=BooleanArray(detections.size);val assigned=HashSet<Int>();for((ti,di)in assignments){val t=tracks[ti];updateTrack(t,detections[di],dt);used[di]=true;assigned+=ti};for(i in tracks.indices)if(i !in assigned)markMissing(tracks[i],dt);for(i in detections.indices)if(!used[i])tracks+=newTrack(detections[i]);tracks.removeAll{it.state==TrackState.REMOVED};return tracks.map{snapshot(it,dt)}.sortedBy{it.id}}
 @Synchronized fun reset(){tracks.clear();nextId=1L}
 private fun predict(t:Track,dt:Float)=translate(t.bounds,t.velocityX*dt+.5f*t.accelerationX*dt*dt,t.velocityY*dt+.5f*t.accelerationY*dt*dt)
 private fun associate(predicted:List<VisionRect>,detections:List<VisionDetection>):List<Pair<Int,Int>>{if(predicted.isEmpty()||detections.isEmpty())return emptyList();data class C(val ti:Int,val di:Int,val score:Float);val cs=ArrayList<C>();for(ti in tracks.indices)for(di in detections.indices){val t=tracks[ti];val d=detections[di];if(t.type!=VisionObjectType.UNKNOWN&&d.type!=VisionObjectType.UNKNOWN&&t.type!=d.type)continue;val i=iou(predicted[ti],d.bounds);val dist=centerDistance(predicted[ti],d.bounds);if(i>=config.iouThreshold||dist<=config.maxAssociationDistance)cs+=C(ti,di,i*.72f+(1f-(dist/config.maxAssociationDistance).coerceIn(0f,1f))*.28f)}val ut=HashSet<Int>();val ud=HashSet<Int>();return cs.sortedByDescending{it.score}.mapNotNull{if(ut.add(it.ti)&&ud.add(it.di))it.ti to it.di else null}}
 private fun updateTrack(t:Track,d:VisionDetection,dt:Float){val vx=(d.bounds.centerX-t.bounds.centerX)/dt;val vy=(d.bounds.centerY-t.bounds.centerY)/dt;val nvx=lerp(t.velocityX,vx,config.velocitySmoothing);val nvy=lerp(t.velocityY,vy,config.velocitySmoothing);t.accelerationX=lerp(t.accelerationX,(nvx-t.velocityX)/dt,config.accelerationSmoothing);t.accelerationY=lerp(t.accelerationY,(nvy-t.velocityY)/dt,config.accelerationSmoothing);t.velocityX=nvx;t.velocityY=nvy;t.bounds=d.bounds;if(d.type!=VisionObjectType.UNKNOWN)t.type=d.type;t.confidence=max(t.confidence*.35f,d.confidence).coerceIn(0f,1f);t.ageFrames++;t.hits++;t.missingFrames=0;t.state=if(t.hits>=config.minHitsToConfirm)TrackState.TRACKED else TrackState.TENTATIVE}
 private fun markMissing(t:Track,dt:Float){t.missingFrames++;t.ageFrames++;t.bounds=predict(t,dt);t.confidence=(t.confidence*config.confidenceDecay).coerceIn(0f,1f);t.state=when{t.missingFrames>config.reidentificationFrames->TrackState.REMOVED;t.missingFrames>config.maxMissingFrames->TrackState.LOST;else->TrackState.OCCLUDED}}
 private fun newTrack(d:VisionDetection)=Track(nextId++,d.type,d.bounds,confidence=d.confidence,ageFrames=1,hits=1)
 private fun snapshot(t:Track,dt:Float)=TrackSnapshot(t.id,t.type,t.bounds,predict(t,dt),t.velocityX,t.velocityY,t.accelerationX,t.accelerationY,t.confidence,t.ageFrames,t.hits,t.missingFrames,t.state)
 private fun translate(r:VisionRect,dx:Float,dy:Float)=VisionRect((r.left+dx).coerceIn(0f,1f),(r.top+dy).coerceIn(0f,1f),(r.right+dx).coerceIn(0f,1f),(r.bottom+dy).coerceIn(0f,1f))
 private fun centerDistance(a:VisionRect,b:VisionRect)=sqrt((a.centerX-b.centerX)*(a.centerX-b.centerX)+(a.centerY-b.centerY)*(a.centerY-b.centerY))
 private fun iou(a:VisionRect,b:VisionRect):Float{val l=max(a.left,b.left);val t=max(a.top,b.top);val r=min(a.right,b.right);val bo=min(a.bottom,b.bottom);if(r<=l||bo<=t)return 0f;val x=(r-l)*(bo-t);return x/(a.area+b.area-x)}
 private fun lerp(a:Float,b:Float,x:Float)=a+(b-a)*x
}
object TrackingCoordinateMapper{fun map(r:VisionRect,t:TrackingTransform)=t.map(r);fun map(p:TrackingPoint,t:TrackingTransform)=t.map(p)}
