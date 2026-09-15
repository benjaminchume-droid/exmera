package studio.exmera.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.exmera.app.camera.ExmeraCameraController
import studio.exmera.app.camera.MultiFrameCaptureEngine
import studio.exmera.app.camera.RecordingState

@Composable
fun ExmeraApp(profile:String){
 var mode by remember{mutableStateOf("Camera")};var fusedBitmap by remember{mutableStateOf<android.graphics.Bitmap?>(null)};var controller by remember{mutableStateOf<ExmeraCameraController?>(null)}
 val captureEngine=remember{MultiFrameCaptureEngine(capacity=8)};val captureState by captureEngine.state.collectAsState();val fallback=remember{mutableStateOf(RecordingState(false))};val recordingState by (controller?.recording?:fallback).collectAsState();val scope=rememberCoroutineScope();DisposableEffect(captureEngine){onDispose{captureEngine.close()}}
 MaterialTheme(colorScheme=darkColorScheme()){Surface(Modifier.fillMaxSize(),color=Color.Black){Column(Modifier.fillMaxSize().padding(20.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text("EXMERA",fontSize=25.sp,fontWeight=FontWeight.Bold);Text("Professional power. Simple controls.",color=Color.LightGray,fontSize=12.sp)};AssistChip(onClick={},label={Text(profile)})}
  Spacer(Modifier.height(18.dp));Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF151515),RoundedCornerShape(28.dp)),contentAlignment=Alignment.Center){if(fusedBitmap!=null)Image(fusedBitmap!!.asImageBitmap(),"Pixel Creator reconstructed result",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)else CameraPreview(Modifier.fillMaxSize(),captureEngine=captureEngine,onController={controller=it});if(recordingState.recording)Surface(Modifier.align(Alignment.TopCenter).padding(top=16.dp),shape=RoundedCornerShape(20.dp)){Text("● Recording",Modifier.padding(horizontal=16.dp,vertical=8.dp),fontSize=12.sp)}}
  Spacer(Modifier.height(12.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){TextButton(onClick={fusedBitmap=null;mode="Camera"}){Text("Camera")};Button(enabled=controller!=null,onClick={if(recordingState.recording)controller?.stopRecording()else controller?.startRecording()},shape=RoundedCornerShape(24.dp),modifier=Modifier.size(72.dp)){Text(if(recordingState.recording)"■" else "REC",fontSize=11.sp)};Button(enabled=captureState!=MultiFrameCaptureEngine.State.PROCESSING&&!recordingState.recording,onClick={if(captureState==MultiFrameCaptureEngine.State.COMPLETE){scope.launch{val result=withContext(Dispatchers.Default){captureEngine.processToBitmap()};fusedBitmap=result;mode=if(result!=null)"Pixel Creator result"else"Camera"}}}else if(captureState!=MultiFrameCaptureEngine.State.CAPTURING&&captureState!=MultiFrameCaptureEngine.State.CLOSED){fusedBitmap=null;captureEngine.start(5);mode="Computational Capture"}},shape=RoundedCornerShape(24.dp),modifier=Modifier.size(72.dp)){Text(if(captureState==MultiFrameCaptureEngine.State.COMPLETE)"✓" else "●")}}
  Text(if(recordingState.error!=null)recordingState.error!! else when(captureState){MultiFrameCaptureEngine.State.CAPTURING->"Gathering original frames";MultiFrameCaptureEngine.State.COMPLETE->"5 frames captured • tap ✓ to reconstruct";MultiFrameCaptureEngine.State.PROCESSING->"Fusion • super-resolution • Pixel Creator";MultiFrameCaptureEngine.State.CLOSED->"Camera engine closed";else->if(recordingState.uri!=null)"Video saved" else mode},Modifier.align(Alignment.CenterHorizontally),color=Color.Gray,fontSize=11.sp)
 }}}
}
