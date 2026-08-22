package com.ankitrawatgit.iqoo9rootghostlock

import android.content.Context
import java.io.File

data class VerifiedPayloads(
    val profile: TargetProfile,
    val releaseTag: String,
    val exploit: File,
    val kernelSu: File,
)

data class ResolvedTarget(
    val releaseTag: String,
    val profile: TargetProfile,
)

class PayloadRepository(context: Context) {
    private val remote = GithubReleasePayloadSource(context)

    fun resolveTarget(snapshot: DeviceSnapshot): ResolvedTarget = remote.resolve(snapshot)

    fun download(
        target: ResolvedTarget,
        onProgress: (String) -> Unit,
    ): VerifiedPayloads = remote.download(target, onProgress)

}
