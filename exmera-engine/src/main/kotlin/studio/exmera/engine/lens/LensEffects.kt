package studio.exmera.engine.lens

import studio.exmera.engine.imaging.RgbImage
import kotlin.math.abs

data class EffectParameter(val id:String,val value:Float,val min:Float=0f,val max:Float=1f){init{require(value in min..max)}}
data class LensEffect(val id:String,val name:String,val version:String,val parameters:List<EffectParameter>,val apply:(RgbImage,List<EffectParameter>)->RgbImage)
object LensEffects {
 fun exposure(id:String="exposure")=LensEffect(id,"Exposure","1.0",listOf(EffectParameter("amount",.0f,-1f,1f))){img,p->val a=p.first().value;RgbImage(img.width,img.height,img.pixels.mapIndexed{_,v->(v+a).coerceIn(0f,1f)}.toFloatArray())}
 fun contrast(id:String="contrast")=LensEffect(id,"Contrast","1.0",listOf(EffectParameter("amount",0f,-1f,1f))){img,p->val a=p.first().value;RgbImage(img.width,img.height,img.pixels.map{((it-.5f)*(1f+a)+.5f).coerceIn(0f,1f)}.toFloatArray())}
 fun apply(image:RgbImage,effects:List<LensEffect>):RgbImage=effects.fold(image){img,e->e.apply(img,e.parameters)}
}
