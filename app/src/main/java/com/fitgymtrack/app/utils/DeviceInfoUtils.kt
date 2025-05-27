// Crea questo file: app/src/main/java/com/fitgymtrack/app/utils/DeviceInfoUtils.kt
package com.fitgymtrack.app.utils

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.fitgymtrack.app.models.DeviceInfo

object DeviceInfoUtils {

    /**
     * Raccoglie tutte le informazioni del dispositivo
     */
    fun collectDeviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            androidVersion = Build.VERSION.RELEASE,
            deviceModel = Build.MODEL,
            deviceManufacturer = Build.MANUFACTURER,
            appVersion = getAppVersion(context),
            screenSize = getScreenSize(context),
            apiLevel = Build.VERSION.SDK_INT
        )
    }

    /**
     * Ottiene la versione dell'app
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Ottiene le dimensioni dello schermo
     */
    private fun getScreenSize(context: Context): String {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                "${bounds.width()}x${bounds.height()}"
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}