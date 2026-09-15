package studio.exmera.exm

import java.io.InputStream
import java.security.MessageDigest

class ExmReader {
    fun inspect(input: InputStream): ExmFormat.Manifest {
        val bytes = input.readBytes()
        require(bytes.size >= 8) { "Invalid EXM: file is too small" }
        require(String(bytes, 0, 4, Charsets.US_ASCII) == ExmFormat.MAGIC) { "Invalid EXM magic" }
        throw UnsupportedOperationException("EXM manifest decoder is next in the compiler/runtime milestone")
    }

    companion object {
        fun sha256(data: ByteArray): String = MessageDigest.getInstance("SHA-256")
            .digest(data).joinToString("") { "%02x".format(it) }
    }
}
