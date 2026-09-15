package studio.exmera.engine

import android.content.Context

class ExmeraEngine(context: Context) {
    val device: DeviceProfile = DeviceProfile.detect(context)

    fun profileName(): String = when (device.tier) {
        DeviceProfile.Tier.LITE -> "Efficient"
        DeviceProfile.Tier.STANDARD -> "Balanced"
        DeviceProfile.Tier.PRO -> "Pro"
        DeviceProfile.Tier.ULTRA -> "Maximum"
    }
}
