package studio.exmera.engine.testing

import kotlin.system.measureNanoTime

data class StressResult(val iterations:Int,val failures:Int,val elapsedNs:Long,val averageNs:Long)
class StressRunner{fun run(iterations:Int,action:()->Unit):StressResult{require(iterations>0);var failures=0;val elapsed=measureNanoTime{repeat(iterations){try{action()}catch(_:Throwable){failures++}}};return StressResult(iterations,failures,elapsed,elapsed/iterations)}}
object DeterminismCheck{fun <T> run(times:Int,operation:()->T):Boolean{require(times>=2);val first=operation();repeat(times-1){if(operation()!=first)return false};return true}}
