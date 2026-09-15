package studio.exmera.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Build

class DeviceProfile private constructor(
    val ramMb: Long,
    val hasNeuralAccelerator: Boolean,
    val cpuCores: Int,
    val tier: Tier
) {
    enum class Tier { LITE, STANDARD, PRO, ULTRA }

    companion object {
        fun detect(context: Context): DeviceProfile {
            val memory = context.getSystemService(ActivityManager::class.java).memoryInfo
            val ram = memory.totalMem / (1024 * 1024)
            val cores = Runtime.getRuntime().availableProcessors()
            val accelerator = Build.VERSION.SDK_INT >= 29
            val tier = when {
                ram < 4096 -> Tier.LITE
                ram < 6144 -> Tier.STANDARD
                ram < 12288 -> Tier.PRO
                else -> Tier.ULTRA
            }
            return DeviceProfile(ram, accelerator, cores, tier)
        }
    }
}
