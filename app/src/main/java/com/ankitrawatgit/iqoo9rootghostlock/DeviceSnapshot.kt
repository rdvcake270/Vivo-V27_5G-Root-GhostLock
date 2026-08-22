package com.ankitrawatgit.iqoo9rootghostlock

import android.os.Build
import android.system.Os
import android.system.OsConstants

data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val device: String,
    val kernelRelease: String,
    val kernelBuildVersion: String,
    val buildId: String,
    val fingerprint: String,
    /**
     * The build's incremental version, which is where a vendor OS puts its own
     * version number: on this project's Xiaomi targets it reads
     * `OS3.0.304.0.WPSJPXM` while `buildId` carries the AOSP build id. Shown
     * only, never used as an exploit selector in this device-only build.
     */
    val osVersion: String,
    val androidRelease: String,
    val sdk: Int,
    val abi: String,
    val pageSize: Long,
) {
    val targetLabel: String
        get() = "$kernelRelease / $buildId"

    /**
     * [osVersion] when it is worth putting on screen beside [buildId], else null.
     *
     * Vendor builds may expose a human-readable incremental version here while
     * the AOSP build id remains in [buildId]. A version-shaped value is shown;
     * hashes and unset properties are omitted.
     */
    val displayOsVersion: String?
        get() = osVersion.takeIf { it != buildId && VERSION_SHAPE.containsMatchIn(it) }

    companion object {
        private val VERSION_SHAPE = Regex("""\d+\.\d+""")

        fun current(): DeviceSnapshot {
            val uname = Os.uname()
            return DeviceSnapshot(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                device = Build.DEVICE,
                kernelRelease = uname.release,
                kernelBuildVersion = uname.version,
                buildId = Build.DISPLAY,
                fingerprint = Build.FINGERPRINT,
                osVersion = Build.VERSION.INCREMENTAL.orEmpty(),
                androidRelease = Build.VERSION.RELEASE,
                sdk = Build.VERSION.SDK_INT,
                abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
                pageSize = Os.sysconf(OsConstants._SC_PAGESIZE),
            )
        }
    }
}
