package studio.exmera.engine.vision

import studio.exmera.engine.imaging.RgbImage
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class VisionRect(val left: Float,val top: Float,val right: Float,val bottom: Float){init{require(left in 0f..1f&&top in 0f..1f&&right in 0f..1f&&bottom in 0f..1f&&right>left&&bottom>top)};val centerX get()=(left+right)/2f;val centerY get()=(top+bottom)/2f;val area get()=(right-left)*(bottom-top)}
enum class VisionObjectType{FACE,PERSON,OBJECT,UNKNOWN}
data class VisionDetection(val id:Long,val type:VisionObjectType,val bounds:VisionRect,val confidence:Float){init{require(confidence in 0f..1f)}}
data class VisionFrame(val image:RgbImage,val timestampNs:Long)
data class VisionQuality(val sharpness:Float,val exposure:Float,val contrast:Float,val noise:Float,val overall:Float)
data class DepthMap(val width:Int,val height:Int,val values:FloatArray){init{require(width>0&&height>0&&values.size==width*height);require(values.all{it in 0f..1f})}}
data class SegmentationMask(val width:Int,val height:Int,val alpha:FloatArray){init{require(width>0&&height>0&&alpha.size==width*height);require(alpha.all{it in 0f..1f})}}
interface VisionBackend{val name:String;fun detect(frame:VisionFrame):List<VisionDetection>}

/** Deterministic fallback detector; it reports UNKNOWN rather than inventing classes. */
class CpuSaliencyBackend(private val threshold:Float=.16f):VisionBackend{
 override val name="cpu-saliency"
 override fun detect(frame:VisionFrame):List<VisionDetection>{val w=frame.image.width;val h=frame.image.height;val l=FloatArray(w*h);for(p in l.indices){val i=p*3;l[p]=.2126f*frame.image.pixels[i]+.7152f*frame.image.pixels[i+1]+.0722f*frame.image.pixels[i+2]};val seen=BooleanArray(l.size);val out=ArrayList<VisionDetection>();var id=1L
  for(y in 1 until h-1)for(x in 1 until w-1){val p=y*w+x;if(seen[p]||gradient(l,w,h,x,y)<threshold)continue;val q=IntArray(max(32,w*h/24));var head=0;var tail=0;q[tail++]=p;seen[p]=true;var minX=x;var maxX=x;var minY=y;var maxY=y;var count=0
   while(head<tail){val v=q[head++];val vx=v%w;val vy=v/w;count++;minX=min(minX,vx);maxX=max(maxX,vx);minY=min(minY,vy);maxY=max(maxY,vy);for(dy in -1..1)for(dx in -1..1){val nx=vx+dx;val ny=vy+dy;if(nx !in 1 until w-1||ny !in 1 until h-1)continue;val n=ny*w+nx;if(!seen[n]&&gradient(l,w,h,nx,ny)>=threshold){seen[n]=true;if(tail<q.size)q[tail++]=n}}}
   val area=count.toFloat()/(w*h);val box=((maxX-minX+1).toFloat()*(maxY-minY+1))/(w*h);if(area>=.002f&&box<=.65f)out+=VisionDetection(id++,VisionObjectType.UNKNOWN,VisionRect(minX/w.toFloat(),minY/h.toFloat(),(maxX+1)/w.toFloat(),(maxY+1)/h.toFloat()),(area/box).coerceIn(0f,1f))}
  return out.sortedByDescending{it.confidence*it.bounds.area}.take(32)}
 private fun gradient(l:FloatArray,w:Int,h:Int,x:Int,y:Int):Float{val gx=l[y*w+min(w-1,x+1)]-l[y*w+max(0,x-1)];val gy=l[min(h-1,y+1)*w+x]-l[max(0,y-1)*w+x];return sqrt(gx*gx+gy*gy)}
}

class VisionTracker(private val maxMissingFrames:Int=8,private val iouThreshold:Float=.12f){private data class Track(var d:VisionDetection,var missing:Int=0);private val tracks=ArrayList<Track>();private var nextId=1L
 @Synchronized fun update(detections:List<VisionDetection>):List<VisionDetection>{val used=BooleanArray(detections.size);for(t in tracks){var best=-1;var score=iouThreshold;for(i in detections.indices)if(!used[i]){val s=iou(t.d.bounds,detections[i].bounds);if(s>score){score=s;best=i}};if(best>=0){t.d=detections[best].copy(id=t.d.id);t.missing=0;used[best]=true}else t.missing++};tracks.removeAll{it.missing>maxMissingFrames};for(i in detections.indices)if(!used[i])tracks+=Track(detections[i].copy(id=nextId++));return tracks.map{it.d}}
 private fun iou(a:VisionRect,b:VisionRect):Float{val l=max(a.left,b.left);val t=max(a.top,b.top);val r=min(a.right,b.right);val d=min(a.bottom,b.bottom);if(r<=l||d<=t)return 0f;val x=(r-l)*(d-t);return x/(a.area+b.area-x)}}

object VisionQualityAnalyzer{private fun luma(p:FloatArray,i:Int)=.2126f*p[i]+.7152f*p[i+1]+.0722f*p[i+2]
 fun analyze(image:RgbImage):VisionQuality{val w=image.width;val h=image.height;var mean=0f;var sharp=0f;var clipped=0;for(p in 0 until w*h){val i=p*3;val v=luma(image.pixels,i);mean+=v;if(v<=.01f||v>=.99f)clipped++;if(p%w<w-1&&p/w<h-1)sharp+=abs(v-luma(image.pixels,(p+1)*3))+abs(v-luma(image.pixels,(p+w)*3))};mean/=(w*h).coerceAtLeast(1);var variance=0f;for(p in 0 until w*h){val d=luma(image.pixels,p*3)-mean;variance+=d*d};val contrast=sqrt(variance/(w*h)).coerceIn(0f,1f);val sharpness=(sharp/(w*h*2f)).coerceIn(0f,1f);val exposure=(1f-2f*abs(mean-.5f)).coerceIn(0f,1f)*(1f-clipped.toFloat()/(w*h));val noise=estimateNoise(image);val overall=(sharpness*.45f+exposure*.3f+contrast*.2f+(1f-noise)*.05f).coerceIn(0f,1f);return VisionQuality(sharpness,exposure,contrast,noise,overall)}
 private fun estimateNoise(image:RgbImage):Float{val w=image.width;val h=image.height;var sum=0f;var n=0;for(y in 1 until h-1)for(x in 1 until w-1){val p=(y*w+x)*3;val a=((y-1)*w+x)*3;val b=((y+1)*w+x)*3;val c=(y*w+x-1)*3;val d=(y*w+x+1)*3;sum+=abs(luma(image.pixels,p)-(luma(image.pixels,a)+luma(image.pixels,b)+luma(image.pixels,c)+luma(image.pixels,d))/4f);n++};return if(n==0)0f else(sum/n*2.5f).coerceIn(0f,1f)}}

/** Relative depth fallback; values are relative and are not physical distances. */
object RelativeDepthEstimator{fun estimate(image:RgbImage):DepthMap{val w=image.width;val h=image.height;val v=FloatArray(w*h);for(y in 0 until h)for(x in 0 until w){val i=(y*w+x)*3;val l=.2126f*image.pixels[i]+.7152f*image.pixels[i+1]+.0722f*image.pixels[i+2];val edge=if(x>0&&x<w-1)abs(l-(.2126f*image.pixels[i-3]+.7152f*image.pixels[i-2]+.0722f*image.pixels[i-1])) else 0f;v[y*w+x]=(1f-l*.72f+edge*.2f).coerceIn(0f,1f)};return DepthMap(w,h,v)}}

object SaliencySegmenter{fun segment(image:RgbImage):SegmentationMask{val w=image.width;val h=image.height;val a=FloatArray(w*h);for(y in 0 until h)for(x in 0 until w){val i=(y*w+x)*3;val r=image.pixels[i];val g=image.pixels[i+1];val b=image.pixels[i+2];val sat=max(r,max(g,b))-min(r,min(g,b));val dx=x/w.toFloat()-.5f;val dy=y/h.toFloat()-.5f;val center=(1f-sqrt(dx*dx+dy*dy)*1.4f).coerceIn(0f,1f);a[y*w+x]=(center*.65f+sat*.35f).coerceIn(0f,1f)};return SegmentationMask(w,h,a)}}

class VisionEngine(private val backends:List<VisionBackend> = listOf(CpuSaliencyBackend())){private val tracker=VisionTracker();fun detect(frame:VisionFrame)=backends.firstOrNull()?.detect(frame)?.let(tracker::update)?:emptyList();fun quality(image:RgbImage)=VisionQualityAnalyzer.analyze(image);fun depth(image:RgbImage)=RelativeDepthEstimator.estimate(image);fun segmentation(image:RgbImage)=SaliencySegmenter.segment(image)}
