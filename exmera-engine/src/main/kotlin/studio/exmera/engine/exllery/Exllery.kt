package studio.exmera.engine.exllery

import studio.exmera.engine.imaging.RgbImage
import studio.exmera.engine.vision.VisionDetection
import studio.exmera.engine.vision.VisionEngine
import studio.exmera.engine.vision.VisionFrame

data class ExlleryAsset(val id:String,val title:String,val mimeType:String,val sizeBytes:Long,val createdAtMs:Long,val tags:Set<String>=emptySet())
data class ExllerySearchResult(val asset:ExlleryAsset,val score:Float)
class ExlleryIndex(private val vision:VisionEngine=VisionEngine()){
 private val assets=LinkedHashMap<String,ExlleryAsset>();private val embeddings=HashMap<String,FloatArray>()
 fun upsert(asset:ExlleryAsset,image:RgbImage?=null){assets[asset.id]=asset;if(image!=null)embeddings[asset.id]=embedding(image)}
 fun remove(id:String){assets.remove(id);embeddings.remove(id)}
 fun search(query:RgbImage,limit:Int=20):List<ExllerySearchResult>{val q=embedding(query);return embeddings.mapNotNull{(id,e)->assets[id]?.let{ExllerySearchResult(it,cosine(q,e))}}.sortedByDescending{it.score}.take(limit.coerceAtLeast(1))}
 fun inspect(image:RgbImage):List<VisionDetection>=vision.detect(VisionFrame(image,0L))
 private fun embedding(img:RgbImage):FloatArray{val bins=FloatArray(12);for(i in img.pixels.indices step 3){bins[(i/3)%12]+=img.pixels[i]+img.pixels[i+1]+img.pixels[i+2]};val norm=kotlin.math.sqrt(bins.sumOf{(it*it).toDouble()}).toFloat().coerceAtLeast(1e-6f);return bins.map{it/norm}.toFloatArray()}
 private fun cosine(a:FloatArray,b:FloatArray):Float=a.indices.sumOf{(a[it]*b[it]).toDouble()}.toFloat()
}
