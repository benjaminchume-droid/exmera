package studio.exmera.exm

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ExmCompressionTest {
    @Test
    fun compressedPayloadRoundTrips() {
        val original = ByteArray(32_000) { (it % 17).toByte() }
        val stored = ByteArrayOutputStream()
        val compressedSize = java.util.zip.DeflaterOutputStream(ByteArrayOutputStream()).let { original.size }
        val compressed = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.DeflaterOutputStream(out).use { it.write(original) }
        }.toByteArray()
        val manifest = ExmFormat.Manifest(
            id = "test-model",
            version = "1.0.0",
            name = "Test Model",
            quantization = ExmFormat.Quantization.FP16,
            backends = setOf(ExmFormat.Backend.CPU, ExmFormat.Backend.GPU),
            sha256 = ExmReader.sha256(compressed),
            sizeBytes = compressed.size.toLong(),
            license = "Apache-2.0",
            compression = ExmFormat.Compression.DEFLATE,
            originalSizeBytes = original.size.toLong()
        )
        ExmContainer.write(stored, manifest, original)
        val (readManifest, readPayload) = ExmContainer.read(ByteArrayInputStream(stored.toByteArray()))
        assertEquals(ExmFormat.Compression.DEFLATE, readManifest.compression)
        assertEquals(original.size.toLong(), readManifest.originalSizeBytes)
        assertContentEquals(original, readPayload)
    }
}
