package studio.exmera.app

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import studio.exmera.app.camera.ExmeraCameraController

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onController: (ExmeraCameraController) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner ?: return
    val controller = remember(context, lifecycleOwner) {
        ExmeraCameraController(context, lifecycleOwner)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also { view ->
                controller.bind(view)
                onController(controller)
            }
        }
    )

    DisposableEffect(controller) {
        onDispose { controller.shutdown() }
    }
}
