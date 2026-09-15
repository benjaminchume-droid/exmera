package studio.exmera.engine.production

import java.time.Instant

data class EngineHealth(val version:String,val startedAt:Instant,val ready:Boolean,val lastError:String?=null)
data class JobDescriptor(val id:String,val type:String,val priority:Int=0,val createdAt:Instant=Instant.now())
class JobQueue{private val jobs=java.util.PriorityQueue<JobDescriptor>(compareByDescending<JobDescriptor>{it.priority}.thenBy{it.createdAt});@Synchronized fun submit(job:JobDescriptor){jobs.add(job)};@Synchronized fun poll():JobDescriptor?=jobs.poll();@Synchronized fun size()=jobs.size}
class FailureBoundary{fun <T> execute(fallback:T,block:()->T):T=try{block()}catch(_:Throwable){fallback}}
