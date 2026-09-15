package studio.exmera.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.exmera.app.camera.MultiFrameCaptureEngine

@Composable
fun ExmeraApp(profile: String) {
    var mode by remember { mutableStateOf("Camera") }
    val captureEngine = remember { MultiFrameCaptureEngine(capacity = 8) }
    val captureState by captureEngine.state.collectAsState()

    DisposableEffect(captureEngine) {
        onDispose { captureEngine.close() }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("EXMERA", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("Professional power. Simple controls.", color = Color.LightGray, fontSize = 12.sp)
                    }
                    AssistChip(onClick = {}, label = { Text(profile) })
                }
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CameraPreview(
                        Modifier.fillMaxSize().background(Color(0xFF151515), RoundedCornerShape(28.dp)),
                        captureEngine = captureEngine
                    )
                    if (captureState == MultiFrameCaptureEngine.State.CAPTURING) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 4.dp
                        ) {
                            Text("Capturing frames…", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { mode = "Enhance" }) { Text("✨ Enhance") }
                    Button(
                        onClick = {
                            if (captureState != MultiFrameCaptureEngine.State.CAPTURING) {
                                if (captureState == MultiFrameCaptureEngine.State.COMPLETE) captureEngine.reset()
                                captureEngine.start(5)
                                mode = "Computational Capture"
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.size(72.dp)
                    ) { Text("●") }
                    TextButton(onClick = { mode = "Exllery" }) { Text("Exllery") }
                }
                Text(
                    when (captureState) {
                        MultiFrameCaptureEngine.State.CAPTURING -> "Gathering original frames"
                        MultiFrameCaptureEngine.State.COMPLETE -> "5-frame capture ready for fusion"
                        MultiFrameCaptureEngine.State.CLOSED -> "Camera engine closed"
                        else -> mode
                    },
                    Modifier.align(Alignment.CenterHorizontally),
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
