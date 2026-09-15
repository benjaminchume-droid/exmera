package studio.exmera.app.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import studio.exmera.engine.imaging.GrayImage
import studio.exmera.engine.imaging.ImagingFrame
import studio.exmera.engine.imaging.RgbImage
import kotlin.math.roundToInt

/** Converts CameraX YUV_420_888 frames into a bounded RGB/luma working representation. */
object ImageProxyImagingAdapter {
    fun toImagingFrame(proxy: ImageProxy, maxDimension: Int = 1920): ImagingFrame {
        require(proxy.format == android.graphics.ImageFormat.YUV_420_888) { "Expected YUV_420_888" }
        val srcW = proxy.width; val srcH = proxy.height
        val scale = minOf(1f, maxDimension.toFloat() / maxOf(srcW, srcH))
        val w = maxOf(1, (srcW * scale).roundToInt()); val h = maxOf(1, (srcH * scale).roundToInt())
        val yPlane = proxy.planes[0]; val uPlane = proxy.planes[1]; val vPlane = proxy.planes[2]
        val rgb = FloatArray(w * h * 3); val luma = FloatArray(w * h)
        val cw = maxOf(1, (srcW + 1) / 2); val ch = maxOf(1, (srcH + 1) / 2)
        for (dy in 0 until h) {
            val sy = minOf(srcH - 1, (dy / scale).toInt())
            for (dx in 0 until w) {
                val sx = minOf(srcW - 1, (dx / scale).toInt())
                val y = sample(yPlane, sx, sy, srcW, srcH)
                val u = sample(uPlane, sx / 2, sy / 2, cw, ch)
                val v = sample(vPlane, sx / 2, sy / 2, cw, ch)
                val yf = y / 255f; val uf = (u - 128) / 255f; val vf = (v - 128) / 255f
                val r = (yf + 1.402f * vf).coerceIn(0f, 1f)
                val g = (yf - 0.344136f * uf - 0.714136f * vf).coerceIn(0f, 1f)
                val b = (yf + 1.772f * uf).coerceIn(0f, 1f)
                val p = dy * w + dx
                rgb[p * 3] = r; rgb[p * 3 + 1] = g; rgb[p * 3 + 2] = b
                luma[p] = 0.2126f * r + 0.7152f * g + 0.0722f * b
            }
        }
        return ImagingFrame(RgbImage(w, h, rgb), GrayImage(w, h, luma), proxy.imageInfo.timestamp)
    }

    fun toBitmap(image: RgbImage): Bitmap {
        val out = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(image.width * image.height)
        for (p in pixels.indices) {
            val i = p * 3
            val r = (image.pixels[i] * 255f).roundToInt().coerceIn(0, 255)
            val g = (image.pixels[i + 1] * 255f).roundToInt().coerceIn(0, 255)
            val b = (image.pixels[i + 2] * 255f).roundToInt().coerceIn(0, 255)
            pixels[p] = android.graphics.Color.rgb(r, g, b)
        }
        out.setPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
        return out
    }

    private fun sample(plane: ImageProxy.PlaneProxy, x: Int, y: Int, width: Int, height: Int): Int {
        val xx = x.coerceIn(0, width - 1); val yy = y.coerceIn(0, height - 1)
        val index = yy * plane.rowStride + xx * plane.pixelStride
        return plane.buffer.get(index).toInt() and 0xff
    }
}
