package com.ankitrawatgit.iqoo9rootghostlock

/** Metadata retained for the one payload compiled into this APK. */
data class RemoteArtifact(
    val url: String,
    val size: Long,
)

data class KernelSuArtifact(
    val artifact: RemoteArtifact,
    val kmi: String,
    val managerPackage: String,
    val managerVersionCode: Int?,
    val managerVersionName: String?,
    val managerUrl: String?,
    val managerCustom: Boolean,
    val managerNote: String?,
)

data class TargetProfile(
    val profileId: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val kernelRelease: String,
    val kernelBuildVersion: String,
    val buildDisplay: String,
    val buildFingerprint: String,
    val sdk: Int,
    val abi: String,
    val pageSize: Long,
    val exploit: RemoteArtifact,
    val kernelSu: KernelSuArtifact,
)
