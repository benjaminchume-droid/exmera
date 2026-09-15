package studio.exmera.exm

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

object ExmContainer {
    private const val FORMAT_VERSION = ExmFormat.VERSION

    fun write(output: OutputStream, manifest: ExmFormat.Manifest, payload: ByteArray) {
        require(manifest.id.isNotBlank())
        require(payload.size.toLong() <= ExmFormat.MAX_PAYLOAD_BYTES)
        val stored = if (manifest.compression == ExmFormat.Compression.DEFLATE) compress(payload) else payload
        require(stored.size.toLong() == manifest.sizeBytes)
        require(manifest.sha256 == ExmReader.sha256(stored)) { "EXM payload SHA-256 mismatch" }
        DataOutputStream(output).use { out ->
            out.writeBytes(ExmFormat.MAGIC)
            out.writeInt(FORMAT_VERSION)
            writeString(out, manifest.id); writeString(out, manifest.version); writeString(out, manifest.name)
            out.writeInt(manifest.quantization.ordinal)
            out.writeInt(manifest.backends.fold(0) { a, b -> a or (1 shl b.ordinal) })
            out.writeInt(manifest.compression.ordinal)
            writeString(out, manifest.sha256)
            out.writeLong(stored.size.toLong())
            out.writeLong(payload.size.toLong())
            out.writeInt(manifest.minRamMb); out.writeInt(manifest.minAndroid); writeString(out, manifest.license)
            out.writeLong(stored.size.toLong()); out.write(stored)
        }
    }

    fun read(input: InputStream): Pair<ExmFormat.Manifest, ByteArray> {
        DataInputStream(input).use { inputStream ->
            val magic = ByteArray(4); inputStream.readFully(magic)
            require(String(magic, Charsets.US_ASCII) == ExmFormat.MAGIC) { "Invalid EXM magic" }
            require(inputStream.readInt() == FORMAT_VERSION) { "Unsupported EXM version" }
            val id = readString(inputStream); val version = readString(inputStream); val name = readString(inputStream)
            val q = ExmFormat.Quantization.entries.getOrNull(inputStream.readInt()) ?: error("Invalid quantization")
            val mask = inputStream.readInt()
            val backends = ExmFormat.Backend.entries.filter { mask and (1 shl it.ordinal) != 0 }.toSet()
            val compression = ExmFormat.Compression.entries.getOrNull(inputStream.readInt()) ?: error("Invalid compression")
            val sha = readString(inputStream); val storedSize = inputStream.readLong(); val originalSize = inputStream.readLong()
            val minRam = inputStream.readInt(); val minAndroid = inputStream.readInt(); val license = readString(inputStream)
            val payloadSize = inputStream.readLong()
            require(payloadSize == storedSize && payloadSize in 0..ExmFormat.MAX_PAYLOAD_BYTES) { "Invalid EXM payload size" }
            val stored = ByteArray(payloadSize.toInt()); inputStream.readFully(stored)
            require(ExmReader.sha256(stored) == sha) { "EXM payload integrity check failed" }
            val payload = if (compression == ExmFormat.Compression.DEFLATE) decompress(stored, originalSize) else stored
            require(payload.size.toLong() == originalSize) { "EXM original size mismatch" }
            return ExmFormat.Manifest(id, version, name, q, backends, sha, stored.size.toLong(), minRam, minAndroid, license, compression, originalSize) to payload
        }
    }

    private fun compress(payload: ByteArray): ByteArray = ByteArrayOutputStream().use { b ->
        DeflaterOutputStream(b).use { it.write(payload) }; b.toByteArray()
    }

    private fun decompress(payload: ByteArray, expected: Long): ByteArray {
        require(expected in 0..ExmFormat.MAX_PAYLOAD_BYTES)
        return InflaterInputStream(ByteArrayInputStream(payload)).use { input ->
            val out = ByteArrayOutputStream(expected.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE); var total = 0L
            while (true) { val n = input.read(buffer); if (n < 0) break; total += n; require(total <= ExmFormat.MAX_PAYLOAD_BYTES); out.write(buffer, 0, n) }
            out.toByteArray()
        }
    }

    private fun writeString(out: DataOutputStream, value: String) { val bytes = value.toByteArray(Charsets.UTF_8); require(bytes.size <= ExmFormat.MAX_STRING_BYTES); out.writeInt(bytes.size); out.write(bytes) }
    private fun readString(input: DataInputStream): String { val size = input.readInt(); require(size in 0..ExmFormat.MAX_STRING_BYTES); val bytes = ByteArray(size); input.readFully(bytes); return String(bytes, Charsets.UTF_8) }
}
