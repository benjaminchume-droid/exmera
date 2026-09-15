package studio.exmera.engine.video

import java.util.concurrent.atomic.AtomicBoolean

/** Phase 16: capture-side timeline ownership and dropped-frame accounting. */
data class CaptureProfile(val width:Int, val height:Int, val targetFps:Int, val codec:String="H264", val bitrate:Int=8_000_000) {
    init { require(width>0&&height>0); require(targetFps in 1..240); require(bitrate>0) }
}
data class CapturedVideoFrame(val frame:VideoFrame, val droppedBefore:Long=0)
data class VideoTimelineStats(val accepted:Long,val dropped:Long,val durationNs:Long,val averageIntervalNs:Long)
class VideoCaptureSession(private val profile:CaptureProfile) : AutoCloseable {
    private val running=AtomicBoolean(false); private var firstTs=-1L; private var lastTs=-1L; private var accepted=0L; private var dropped=0L; private var lastSequence=-1L
    fun start(){check(!running.getAndSet(true)){"capture already running"};firstTs=-1L;lastTs=-1L;accepted=0;dropped=0;lastSequence=-1}
    fun accept(frame:VideoFrame):Boolean { if(!running.get()) return false; if(firstTs<0) firstTs=frame.timestampNs; if(lastSequence>=0&&frame.sequence>lastSequence+1)dropped+=frame.sequence-lastSequence-1; if(frame.timestampNs<=lastTs){dropped++;return false};lastTs=frame.timestampNs;lastSequence=frame.sequence;accepted++;return true }
    fun stop(){running.set(false)}
    fun stats():VideoTimelineStats { val duration=if(firstTs<0)0 else maxOf(0,lastTs-firstTs); return VideoTimelineStats(accepted,dropped,duration,if(accepted>1)duration/(accepted-1) else 0) }
    override fun close(){stop()}
}
