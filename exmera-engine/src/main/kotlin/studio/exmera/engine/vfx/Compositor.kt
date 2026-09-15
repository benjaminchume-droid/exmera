package studio.exmera.engine.vfx

import studio.exmera.engine.imaging.RgbImage
import kotlin.math.max
import kotlin.math.min

/** Phase 18: CPU compositor with premultiplied-alpha layers and blend modes. */
enum class BlendMode{NORMAL,ADD,MULTIPLY,SCREEN,OVERLAY}
data class CompositeLayer(val image:RgbImage,val alpha:Float=1f,val mode:BlendMode=BlendMode.NORMAL,val opacity:Float=1f){init{require(alpha in 0f..1f&&opacity in 0f..1f)}}
object Compositor {
 fun composite(base:RgbImage,layers:List<CompositeLayer>):RgbImage { val out=base.pixels.copyOf(); for(layer in layers){require(layer.image.width==base.width&&layer.image.height==base.height);for(i in out.indices){val a=(layer.alpha*layer.opacity).coerceIn(0f,1f);val s=layer.image.pixels[i];val d=out[i];val b=when(layer.mode){BlendMode.NORMAL->s;BlendMode.ADD->min(1f,d+s);BlendMode.MULTIPLY->d*s;BlendMode.SCREEN->1f-(1f-d)*(1f-s);BlendMode.OVERLAY->if(d<.5f)2f*d*s else 1f-2f*(1f-d)*(1f-s)};out[i]=(d*(1f-a)+b*a).coerceIn(0f,1f)}};return RgbImage(base.width,base.height,out)}
 fun alphaComposite(foreground:RgbImage,background:RgbImage,alpha:Float)=composite(background,listOf(CompositeLayer(foreground,alpha)))
}
