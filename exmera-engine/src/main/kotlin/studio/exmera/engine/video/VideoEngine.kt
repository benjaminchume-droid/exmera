package studio.exmera.engine.video

import studio.exmera.engine.imaging.RgbImage
import kotlin.math.abs
import kotlin.math.max

data class VideoFrame(val image: RgbImage, val timestampNs: Long, val sequence: Long, val exposure: Float = 1f) {
    init { require(timestampNs >= 0); require(sequence >= 0); require(exposure > 0f) }
}

data class OpticalFlowField(val width: Int, val height: Int, val vectors: FloatArray, val confidence: Float) {
    init { require(width > 0 && height > 0 && vectors.size == width * height * 2); require(confidence in 0f..1f) }
    fun dx(x: Int, y: Int) = vectors[(y * width + x) * 2]
    fun dy(x: Int, y: Int) = vectors[(y * width + x) * 2 + 1]
}

data class VideoProcessConfig(val temporalWindow: Int = 5, val denoiseStrength: Float = .35f, val stabilization: Float = .65f, val maxFrameGapNs: Long = 250_000_000L) {
    init { require(temporalWindow in 2..16); require(denoiseStrength in 0f..1f); require(stabilization in 0f..1f); require(maxFrameGapNs > 0) }
}
interface OpticalFlowBackend { fun estimate(previous: RgbImage, current: RgbImage): OpticalFlowField }

/** CPU translation-flow reference backend; dense learned EXM flow can replace this contract. */
class CpuOpticalFlowBackend(private val radius: Int = 6) : OpticalFlowBackend {
    override fun estimate(previous: RgbImage, current: RgbImage): OpticalFlowField {
        require(previous.width == current.width && previous.height == current.height)
        val w = current.width; val h = current.height
        fun luma(img: RgbImage, x: Int, y: Int): Float { val i = img.index(x, y); return .2126f*img.pixels[i]+.7152f*img.pixels[i+1]+.0722f*img.pixels[i+2] }
        var bestDx=0; var bestDy=0; var best=Float.MAX_VALUE
        val r=radius.coerceIn(1,minOf(32,max(1,minOf(w,h)/4))); val ys=max(1,h/24); val xs=max(1,w/24)
        for(dy in -r..r) for(dx in -r..r){ var error=0f; var n=0; for(y in r until max(r+1,h-r) step ys) for(x in r until max(r+1,w-r) step xs){ val xx=(x+dx).coerceIn(0,w-1); val yy=(y+dy).coerceIn(0,h-1); error+=abs(luma(previous,x,y)-luma(current,xx,yy)); n++ }; val score=error/max(1,n); if(score<best){best=score;bestDx=dx;bestDy=dy} }
        val confidence=(1f-best*3f).coerceIn(0f,1f)
        return OpticalFlowField(w,h,FloatArray(w*h*2){if(it%2==0)bestDx.toFloat() else bestDy.toFloat()},confidence)
    }
}

class TemporalFrameBuffer<T:AutoCloseable>(private val capacity:Int):AutoCloseable{
    init{require(capacity in 2..32)}
    private val frames=ArrayDeque<T>(capacity); private var closed=false
    @Synchronized fun offer(value:T):Boolean{if(closed){value.close();return false};if(frames.size==capacity)frames.removeFirst().close();frames.addLast(value);return true}
    @Synchronized fun snapshot():List<T>=frames.toList()
    @Synchronized fun clear(){while(frames.isNotEmpty())frames.removeFirst().close()}
    @Synchronized override fun close(){if(!closed){closed=true;clear()}}
}

/** Conservative temporal denoise: recent frames only, dimensions validated, strong pixel disagreement rejected. */
object TemporalDenoiser{
    fun process(frames:List<VideoFrame>,strength:Float,maxGapNs:Long=250_000_000L):RgbImage{
        require(frames.isNotEmpty()); require(maxGapNs>0)
        val ordered=frames.sortedWith(compareBy<VideoFrame>{it.timestampNs}.thenBy{it.sequence}); val ref=ordered.last().image; val w=ref.width; val h=ref.height
        if(ordered.size==1||strength<=0f)return RgbImage(w,h,ref.pixels.copyOf())
        val newest=ordered.last().timestampNs
        val valid=ordered.filter{it.image.width==w&&it.image.height==h&&newest-it.timestampNs<=maxGapNs}.ifEmpty{listOf(ordered.last())}
        val alpha=strength.coerceIn(0f,1f); val out=FloatArray(w*h*3)
        for(p in 0 until w*h)for(c in 0..2){val cur=ref.pixels[p*3+c];var sum=0f;var weight=0f;for(f in valid){val v=f.image.pixels[p*3+c];val weightPixel=1f/(1f+abs(v-cur)*8f);sum+=v*weightPixel;weight+=weightPixel};val average=if(weight>0f)sum/weight else cur;out[p*3+c]=(cur*(1f-alpha)+average*alpha).coerceIn(0f,1f)}
        return RgbImage(w,h,out)
    }
}

data class StabilizationTransform(val dx:Float,val dy:Float,val confidence:Float)
object Stabilizer{fun estimate(flow:OpticalFlowField,strength:Float):StabilizationTransform{var dx=0f;var dy=0f;var n=0;for(p in 0 until flow.width*flow.height){dx+=flow.vectors[p*2];dy+=flow.vectors[p*2+1];n++};val scale=strength.coerceIn(0f,1f)/max(1,n);return StabilizationTransform(-dx*scale,-dy*scale,flow.confidence)}}

class VideoEngine(private val flowBackend:OpticalFlowBackend=CpuOpticalFlowBackend(),private val config:VideoProcessConfig=VideoProcessConfig()){
    fun opticalFlow(previous:RgbImage,current:RgbImage)=flowBackend.estimate(previous,current)
    fun process(frames:List<VideoFrame>):RgbImage{require(frames.isNotEmpty());val ordered=frames.sortedBy{it.timestampNs}.takeLast(config.temporalWindow);return TemporalDenoiser.process(ordered,config.denoiseStrength,config.maxFrameGapNs)}
    fun stabilization(previous:RgbImage,current:RgbImage)=Stabilizer.estimate(flowBackend.estimate(previous,current),config.stabilization)
}
