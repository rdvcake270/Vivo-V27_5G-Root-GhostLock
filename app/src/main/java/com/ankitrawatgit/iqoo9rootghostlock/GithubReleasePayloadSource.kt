package com.ankitrawatgit.iqoo9rootghostlock

import android.content.Context
import android.system.Os
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/** Downloads the exact iQOO payload pair from the latest GitHub release. */
class GithubReleasePayloadSource(private val context: Context) {
    fun resolve(snapshot: DeviceSnapshot): ResolvedTarget {
        require(snapshot.model.equals(EXPECTED_MODEL, ignoreCase = true)) {
            "Unsupported model: ${snapshot.model}; expected $EXPECTED_MODEL"
        }
        require(snapshot.kernelRelease == KERNEL_RELEASE) {
            "Unsupported kernel: ${snapshot.kernelRelease}; expected $KERNEL_RELEASE"
        }
        require(snapshot.abi == "arm64-v8a") {
            "Unsupported ABI: ${snapshot.abi}; this payload is arm64-v8a only"
        }
        val release = loadLatestRelease()
        val profile = TargetProfile(
            profileId = PROFILE_ID,
            manufacturer = snapshot.manufacturer,
            model = snapshot.model,
            device = snapshot.device,
            kernelRelease = snapshot.kernelRelease,
            kernelBuildVersion = snapshot.kernelBuildVersion,
            buildDisplay = snapshot.buildId,
            buildFingerprint = snapshot.fingerprint,
            sdk = snapshot.sdk,
            abi = snapshot.abi,
            pageSize = snapshot.pageSize,
            exploit = release.exploit,
            kernelSu = KernelSuArtifact(
                artifact = release.kernelSu,
                kmi = KMI,
                managerPackage = KERNELSU_MANAGER_PACKAGE,
                managerVersionCode = KERNELSU_VERSION_CODE,
                managerVersionName = KERNELSU_VERSION_NAME,
                managerUrl = KERNELSU_MANAGER_URL,
                managerCustom = false,
                managerNote = "Use the matching KernelSU Manager for this payload.",
            ),
        )
        return ResolvedTarget(release.tag, profile)
    }

    fun download(
        target: ResolvedTarget,
        onProgress: (String) -> Unit,
    ): VerifiedPayloads {
        val directory = File(context.filesDir, "payloads/$PROFILE_ID").apply { mkdirs() }
        val exploit = downloadArtifact(
            target.profile.exploit,
            File(directory, "cve-2026-43499-app.so"),
            context.getString(R.string.artifact_exploit),
            onProgress,
        )
        val kernelSu = downloadArtifact(
            target.profile.kernelSu.artifact,
            File(directory, "ksud-iqoo-z9-5g"),
            context.getString(R.string.artifact_kernelsu),
            onProgress,
        )
        Os.chmod(exploit.absolutePath, 0b111101101)
        Os.chmod(kernelSu.absolutePath, 0b111101101)
        return VerifiedPayloads(target.profile, target.releaseTag, exploit, kernelSu)
    }

    private fun loadLatestRelease(): GithubRelease {
        val json = JSONObject(downloadBytes(RELEASE_API_URL, MAX_RELEASE_RESPONSE_BYTES).toString(Charsets.UTF_8))
        val tag = json.optString("tag_name").trim()
        require(tag.isNotEmpty()) {
            context.getString(R.string.repo_release_invalid)
        }
        val assets = json.optJSONArray("assets")
            ?: error(context.getString(R.string.repo_release_invalid))
        val assetList = buildList {
            for (index in 0 until assets.length()) {
                add(assets.getJSONObject(index))
            }
        }
        return GithubRelease(
            tag = tag,
            exploit = assetList.singleAsset(EXPLOIT_SUFFIX, tag),
            kernelSu = assetList.singleAsset(KSUD_SUFFIX, tag),
        )
    }

    private fun List<JSONObject>.singleAsset(suffix: String, tag: String): RemoteArtifact {
        val matches = filter { it.optString("name").endsWith(suffix) }
        require(matches.size == 1) {
            context.getString(R.string.repo_feed_missing, suffix)
        }
        val asset = matches.single()
        val name = asset.optString("name")
        val size = asset.optLong("size", -1L)
        val url = asset.optString("browser_download_url")
        require(
            name.isNotBlank() &&
                size > 0L &&
                url == "$RELEASE_DOWNLOAD_BASE/$tag/$name",
        ) {
            context.getString(R.string.repo_release_invalid)
        }
        return RemoteArtifact(url = url, size = size)
    }

    private fun downloadArtifact(
        artifact: RemoteArtifact,
        destination: File,
        label: String,
        onProgress: (String) -> Unit,
    ): File {
        onProgress(context.getString(R.string.repo_downloading, label))
        val temporary = File(destination.parentFile, ".${destination.name}.part")
        try {
            val connection = open(artifact.url)
            try {
                require(connection.contentLengthLong == -1L || connection.contentLengthLong == artifact.size) {
                    context.getString(R.string.repo_size_mismatch, label)
                }
                var total = 0L
                connection.inputStream.use { input ->
                    FileOutputStream(temporary).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= artifact.size) {
                                context.getString(R.string.repo_size_exceeded, label)
                            }
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                }
                require(total == artifact.size) {
                    context.getString(R.string.repo_incomplete, label)
                }
            } finally {
                connection.disconnect()
            }
            if (destination.exists()) {
                require(destination.delete()) {
                    context.getString(R.string.repo_finalize_failed, label)
                }
            }
            require(temporary.renameTo(destination)) {
                context.getString(R.string.repo_finalize_failed, label)
            }
            onProgress(context.getString(R.string.repo_verified, label))
            return destination
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    private fun downloadBytes(url: String, maximum: Int): ByteArray {
        val connection = open(url)
        return try {
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= maximum) {
                        context.getString(R.string.repo_response_too_large)
                    }
                    output.write(buffer, 0, count)
                }
            }
            output.toByteArray()
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "iQOOZ9GhostLock/${BuildConfig.VERSION_NAME}")
            connect()
            require(responseCode == HttpURLConnection.HTTP_OK) { "HTTP $responseCode" }
        }

    private data class GithubRelease(
        val tag: String,
        val exploit: RemoteArtifact,
        val kernelSu: RemoteArtifact,
    )

    companion object {
        const val PROFILE_ID = "iqoo-z9-5g"
        const val EXPECTED_MODEL = "I2302"
        const val KERNEL_RELEASE = "5.15.178-android13-8-g0ebe6a5da65d"
        private const val RELEASE_API_URL =
            "https://api.github.com/repos/ankitrawatgit/iQOO-Z9_5G-Root-GhostLock/releases/latest"
        private const val RELEASE_DOWNLOAD_BASE =
            "https://github.com/ankitrawatgit/iQOO-Z9_5G-Root-GhostLock/releases/download"
        private const val EXPLOIT_SUFFIX = "-app.so"
        private const val KSUD_SUFFIX = "-ksud"
        private const val MAX_RELEASE_RESPONSE_BYTES = 128 * 1024
        private const val KMI = "android13-5.15"
        private const val KERNELSU_MANAGER_PACKAGE = "me.weishu.kernelsu"
        private const val KERNELSU_VERSION_CODE = 32525
        private const val KERNELSU_VERSION_NAME = "3.2.5"
        private const val KERNELSU_MANAGER_URL = "https://github.com/tiann/KernelSU/releases"
    }
}
