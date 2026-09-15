package studio.exmera.engine.export

import studio.exmera.engine.security.MetadataSanitizer
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class ExportFormat{RAW_RGB8,EXMERA_FRAME}
data class ExportRequest(val format:ExportFormat,val metadata:Map<String,String>=emptyMap(),val sanitizeMetadata:Boolean=true)
class ExportEngine { fun exportRgb(width:Int,height:Int,pixels:FloatArray,request:ExportRequest,out:OutputStream){require(width>0&&height>0&&pixels.size==width*height*3);val meta=if(request.sanitizeMetadata)MetadataSanitizer.sanitize(request.metadata) else request.metadata;require(meta.size<=255);when(request.format){ExportFormat.RAW_RGB8->{out.write(byteArrayOf('E'.code.toByte(),'X'.code.toByte(),'R'.code.toByte(),'G'.code.toByte(),'B'.code.toByte(),1));out.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putInt(width).putInt(height).array());out.write(meta.size);for((k,v)in meta){val kb=k.toByteArray();val vb=v.toByteArray();require(kb.size<256&&vb.size<65536);out.write(kb.size);out.write(kb);out.write(ByteBuffer.allocate(2).putShort(vb.size.toShort()).array());out.write(vb)};for(p in pixels)out.write((p.coerceIn(0f,1f)*255f+.5f).toInt())};ExportFormat.EXMERA_FRAME->{out.write(byteArrayOf('E'.code.toByte(),'X'.code.toByte(),'F'.code.toByte(),'1'.code.toByte()));for(p in pixels)out.write((p.coerceIn(0f,1f)*255f+.5f).toInt())}}}}
