package studio.exmera.app.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class CameraCapabilities(val cameraId:String,val facing:Int,val sensorWidth:Int,val sensorHeight:Int,val maxDigitalZoom:Float,val supportsRaw:Boolean,val supportsYuv:Boolean,val supportsJpeg:Boolean,val supportsManualExposure:Boolean,val supportsManualFocus:Boolean,val supportsOis:Boolean,val supportsHdr:Boolean,val availableFps:List<Int>,val activeArrayWidth:Int,val activeArrayHeight:Int)
data class CameraFrameMetadata(val timestampNs:Long,val width:Int,val height:Int,val format:Int,val rotationDegrees:Int,val exposureTimeNs:Long?,val sensitivityIso:Int?,val focalLengthMm:Float?,val aperture:Float?)
data class CameraFrame(val image:ImageProxy,val metadata:CameraFrameMetadata)
data class RecordingState(val recording:Boolean,val uri:String?=null,val durationNs:Long=0L,val error:String?=null)

class CameraCapabilityRepository(private val context:Context){
 fun discover(facing:Int=CameraCharacteristics.LENS_FACING_BACK):CameraCapabilities?{
  val manager=context.getSystemService(CameraManager::class.java)?:return null
  val id=manager.cameraIdList.firstOrNull{manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING)==facing}?:return null
  val c=manager.getCameraCharacteristics(id);val map=c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?:return null
  val active=c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);val pixel=c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
  val fps=c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.map{it.upper}?.distinct()?.sorted()?:emptyList();val formats=map.outputFormats.toSet();val caps=c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toSet().orEmpty()
  return CameraCapabilities(id,facing,pixel?.width?:0,pixel?.height?:0,c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)?:1f,ImageFormat.RAW_SENSOR in formats&&CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW in caps,ImageFormat.YUV_420_888 in formats,ImageFormat.JPEG in formats,c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)?.contains(CameraCharacteristics.CONTROL_AE_MODE_OFF)==true,c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.contains(CameraCharacteristics.CONTROL_AF_MODE_OFF)==true,c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON)==true,c.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)?.contains(CameraCharacteristics.CONTROL_SCENE_MODE_HDR)==true,fps,active?.width?:0,active?.height?:0)
 }
}

class ExmeraCameraController(private val context:Context,private val lifecycleOwner:LifecycleOwner){
 private val _capabilities=MutableStateFlow<CameraCapabilities?>(null);val capabilities:StateFlow<CameraCapabilities?> = _capabilities.asStateFlow();private val _isReady=MutableStateFlow(false);val isReady:StateFlow<Boolean> = _isReady.asStateFlow();private val _recording=MutableStateFlow(RecordingState(false));val recording:StateFlow<RecordingState> = _recording.asStateFlow()
 private var provider:ProcessCameraProvider?=null;private var camera:Camera?=null;private var analysis:ImageAnalysis?=null;private var capture:ImageCapture?=null;private var videoCapture:VideoCapture<Recorder>?=null;private var activeRecording:Recording?=null;private val executor:ExecutorService=Executors.newFixedThreadPool(2)
 fun bind(previewView:PreviewView,onFrame:((CameraFrame)->Unit)?=null){val future=ProcessCameraProvider.getInstance(context);future.addListener({val p=future.get();provider=p;_capabilities.value=CameraCapabilityRepository(context).discover();val preview=Preview.Builder().build().also{it.surfaceProvider=previewView.surfaceProvider};val a=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888).build();a.setAnalyzer(executor){proxy->val frame=CameraFrame(proxy,CameraFrameMetadata(proxy.imageInfo.timestamp,proxy.width,proxy.height,proxy.format,proxy.imageInfo.rotationDegrees,null,null,null,null));if(onFrame!=null)onFrame(frame)else proxy.close()};analysis=a;capture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();val quality=QualitySelector.fromOrderedList(listOf(Quality.FHD,Quality.HD,Quality.SD),QualitySelector.FallbackStrategy.lowerQualityOrHigherThan(Quality.HD));val recorder=Recorder.Builder().setQualitySelector(quality).build();videoCapture=VideoCapture.withOutput(recorder);p.unbindAll();camera=p.bindToLifecycle(lifecycleOwner,CameraSelector.DEFAULT_BACK_CAMERA,preview,a,capture,videoCapture);_isReady.value=true},ContextCompat.getMainExecutor(context))}
 fun startRecording(){val vc=videoCapture?:return;if(activeRecording!=null)return;val values=ContentValues().apply{put(MediaStore.Video.Media.DISPLAY_NAME,"EXMERA_${System.currentTimeMillis()}.mp4");put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");if(Build.VERSION.SDK_INT>=29)put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/Exmera")};val output=MediaStoreOutputOptions.Builder(context.contentResolver,MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).build();activeRecording=vc.output.prepareRecording(context,output).start(ContextCompat.getMainExecutor(context)){event->when(event){is VideoRecordEvent.Start->_recording.value=RecordingState(true);is VideoRecordEvent.Finalize->{activeRecording=null;if(event.hasError())_recording.value=RecordingState(false,error="Recording failed: ${event.error}")else _recording.value=RecordingState(false,event.outputResults.outputUri.toString(),event.recordingStats.recordedDurationNanos)}}}}
 fun stopRecording(){activeRecording?.stop()};fun setZoomRatio(ratio:Float){camera?.cameraControl?.setZoomRatio(ratio)};fun setLinearZoom(value:Float){camera?.cameraControl?.setLinearZoom(value.coerceIn(0f,1f))}
 fun shutdown(){activeRecording?.stop();activeRecording=null;provider?.unbindAll();analysis?.clearAnalyzer();analysis=null;capture=null;videoCapture=null;camera=null;_isReady.value=false;executor.shutdownNow()}
}
