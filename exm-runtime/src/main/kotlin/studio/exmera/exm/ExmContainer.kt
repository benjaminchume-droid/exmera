package studio.exmera.exm

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Deterministic EXM v1 container. Manifest is small; payload is opaque model data. */
object ExmContainer {
    private const val FORMAT_VERSION = 1

    fun write(output: OutputStream, manifest: ExmFormat.Manifest, payload: ByteArray) {
        require(manifest.id.isNotBlank())
        require(manifest.sha256 == ExmReader.sha256(payload)) { "Manifest SHA-256 does not match payload" }
        DataOutputStream(output).use { out ->
            out.writeBytes(ExmFormat.MAGIC)
            out.writeInt(FORMAT_VERSION)
            writeString(out, manifest.id)
            writeString(out, manifest.version)
            writeString(out, manifest.name)
            out.writeInt(manifest.quantization.ordinal)
            out.writeInt(manifest.backends.fold(0) { acc, b -> acc or (1 shl b.ordinal) })
            writeString(out, manifest.sha256)
            out.writeLong(payload.size.toLong())
            out.writeInt(manifest.minRamMb)
            out.writeInt(manifest.minAndroid)
            writeString(out, manifest.license)
            out.writeLong(payload.size.toLong())
            out.write(payload)
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
            val sha = readString(inputStream)
            val declaredSize = inputStream.readLong()
            val minRam = inputStream.readInt(); val minAndroid = inputStream.readInt(); val license = readString(inputStream)
            val payloadSize = inputStream.readLong()
            require(payloadSize == declaredSize && payloadSize >= 0 && payloadSize <= Int.MAX_VALUE) { "Invalid EXM payload size" }
            val payload = ByteArray(payloadSize.toInt()); inputStream.readFully(payload)
            require(ExmReader.sha256(payload) == sha) { "EXM payload integrity check failed" }
            return ExmFormat.Manifest(id, version, name, q, backends, sha, payload.size.toLong(), minRam, minAndroid, license) to payload
        }
    }

    private fun writeString(out: DataOutputStream, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8); require(bytes.size <= 1_048_576)
        out.writeInt(bytes.size); out.write(bytes)
    }
    private fun readString(input: DataInputStream): String {
        val size = input.readInt(); require(size in 0..1_048_576) { "Invalid EXM string length" }
        val bytes = ByteArray(size); input.readFully(bytes); return String(bytes, Charsets.UTF_8)
    }
}
