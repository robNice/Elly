package de.robnice.elgy.platform

import android.os.Build
import de.robnice.elgy.BuildConfig

object DevicePlatform {

    val isEmulator: Boolean by lazy {
        val fingerprint = Build.FINGERPRINT.orEmpty()
        val model = Build.MODEL.orEmpty()
        val product = Build.PRODUCT.orEmpty()
        val hardware = Build.HARDWARE.orEmpty()
        val board = Build.BOARD.orEmpty()

        fingerprint.startsWith("generic") ||
                fingerprint.startsWith("unknown") ||
                Build.BRAND.orEmpty().startsWith("generic") ||
                Build.DEVICE.orEmpty().startsWith("generic") ||
                model.contains("google_sdk", ignoreCase = true) ||
                model.contains("Emulator", ignoreCase = true) ||
                model.contains("Android SDK built for", ignoreCase = true) ||
                Build.MANUFACTURER.orEmpty().contains("Genymotion", ignoreCase = true) ||
                product.contains("sdk", ignoreCase = true) ||
                product.contains("emulator", ignoreCase = true) ||
                product.contains("simulator", ignoreCase = true) ||
                hardware.contains("goldfish", ignoreCase = true) ||
                hardware.contains("ranchu", ignoreCase = true) ||
                board.contains("goldfish", ignoreCase = true) ||
                board.contains("ranchu", ignoreCase = true)
    }

    val isDebugEmulator: Boolean
        get() = BuildConfig.DEBUG && isEmulator
}
