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

@Composable
fun ExmeraApp(profile: String) {
    var mode by remember { mutableStateOf("Camera") }
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("EXMERA", fontSize = 25.sp, fontWeight = FontWeight.Bold); Text("Professional power. Simple controls.", color = Color.LightGray, fontSize = 12.sp) }
                    AssistChip(onClick = {}, label = { Text(profile) })
                }
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CameraPreview(Modifier.fillMaxSize().background(Color(0xFF151515), RoundedCornerShape(28.dp)))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { mode = "Enhance" }) { Text("✨ Enhance") }
                    Button(onClick = { mode = "Camera" }, shape = RoundedCornerShape(24.dp), modifier = Modifier.size(72.dp)) { Text("●") }
                    TextButton(onClick = { mode = "Exllery" }) { Text("Exllery") }
                }
                Text(mode, Modifier.align(Alignment.CenterHorizontally), color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}
