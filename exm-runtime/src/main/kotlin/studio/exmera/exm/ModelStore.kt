package studio.exmera.exm

import java.io.File
import java.io.FileInputStream

class ModelStore(private val root: File) {
    init { root.mkdirs() }

    fun fileFor(id: String, version: String): File {
        require(id.matches(Regex("[A-Za-z0-9._-]+"))) { "Unsafe model id" }
        require(version.matches(Regex("[A-Za-z0-9._-]+"))) { "Unsafe model version" }
        return File(File(root, id), "$version.exm")
    }

    fun isInstalled(descriptor: ModelDescriptor): Boolean {
        val file = fileFor(descriptor.id, descriptor.version)
        return file.isFile && file.length() > 0 && sha256(file) == descriptor.sha256
    }

    fun remove(descriptor: ModelDescriptor): Boolean = fileFor(descriptor.id, descriptor.version).delete()

    fun open(descriptor: ModelDescriptor): FileInputStream = FileInputStream(fileFor(descriptor.id, descriptor.version))

    private fun sha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
