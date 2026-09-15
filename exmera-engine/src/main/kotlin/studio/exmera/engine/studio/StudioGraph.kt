package studio.exmera.engine.studio

import studio.exmera.engine.imaging.RgbImage

/** Phase 17: nondestructive editor graph. Nodes are ordered, addressable and resettable. */
data class StudioClip(val id:String,val startUs:Long,val durationUs:Long,val source:String){init{require(id.isNotBlank());require(startUs>=0&&durationUs>0)}}
data class StudioProject(val id:String,val width:Int,val height:Int,val frameRate:Int,val clips:List<StudioClip>){init{require(width>0&&height>0&&frameRate>0)}}
interface StudioNode { val id:String; fun apply(image:RgbImage,timeUs:Long):RgbImage }
class StudioGraph { private val nodes=LinkedHashMap<String,StudioNode>(); fun add(node:StudioNode){require(!nodes.containsKey(node.id)){"Duplicate node ${node.id}"};nodes[node.id]=node}; fun remove(id:String)=nodes.remove(id); fun process(image:RgbImage,timeUs:Long):RgbImage=nodes.values.fold(image){v,n->n.apply(v,timeUs)}; fun ids()=nodes.keys.toList(); fun clear()=nodes.clear() }
class StudioSession(val project:StudioProject){val graph=StudioGraph(); fun render(image:RgbImage,timeUs:Long)=graph.process(image,timeUs)}
