package studio.exmera.exm

import java.io.InputStream
import java.security.MessageDigest

class ExmReader {
    fun inspect(input: InputStream): ExmFormat.Manifest = ExmContainer.read(input).first

    companion object {
        fun sha256(data: ByteArray): String = MessageDigest.getInstance("SHA-256")
            .digest(data).joinToString("") { "%02x".format(it) }
    }
}
