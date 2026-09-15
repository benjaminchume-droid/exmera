package studio.exmera.engine.lens

import java.io.File
import java.security.MessageDigest

data class LensPackage(val id:String,val version:String,val sha256:String,val sizeBytes:Long,val minEngineVersion:String="1.0")
class LensStore(private val root:File){init{root.mkdirs()}
 fun file(pkg:LensPackage)=File(File(root,safe(pkg.id)),safe(pkg.version)+".exfx")
 fun install(pkg:LensPackage,payload:ByteArray):File{require(sha256(payload)==pkg.sha256);val f=file(pkg);f.parentFile?.mkdirs();f.writeBytes(payload);return f}
 fun isValid(pkg:LensPackage):Boolean{val f=file(pkg);return f.isFile&&f.length()==pkg.sizeBytes&&sha256(f.readBytes())==pkg.sha256}
 fun remove(pkg:LensPackage)=file(pkg).delete()
 private fun safe(s:String)=s.replace(Regex("[^A-Za-z0-9._-]"),"_")
 private fun sha256(data:ByteArray)=MessageDigest.getInstance("SHA-256").digest(data).joinToString(""){ "%02x".format(it) }
}
