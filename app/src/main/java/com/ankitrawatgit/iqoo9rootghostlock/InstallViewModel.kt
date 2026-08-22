package com.ankitrawatgit.iqoo9rootghostlock

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

enum class InstallPhase {
    Checking,
    Ready,
    Downloading,
    Exploiting,
    LoadingKernelSu,
    Installed,
    Failed,

    /**
     * Refused before it started, because this boot cannot win it. Terminal like
     * [Failed], but nothing was attempted, so it is not reported as a failure.
     */
    Skipped,
}

data class InstallUiState(
    val phase: InstallPhase = InstallPhase.Checking,
    val message: String = "",
    val probeOutput: String = "",
    val log: String = "",
    /** The payload release this run read its artifacts from. */
    val payloadTag: String? = null,
    /**
     * The KernelSU build this device's profile pairs with, so the overview can
     * name the manager version it needs rather than leaving the user to find
     * out from the manager that the two do not match. Null when no profile has
     * been resolved -- no network, or no entry for this device.
     */
    val kernelSu: KernelSuArtifact? = null,
) {
    val busy: Boolean
        get() = phase in setOf(
            InstallPhase.Checking,
            InstallPhase.Downloading,
            InstallPhase.Exploiting,
            InstallPhase.LoadingKernelSu,
        )

}

/**
 * Thrown to end a run that this boot cannot win, before anything is attempted.
 * Carried by its own type so the outcome is recorded as skipped rather than
 * failed -- see [InstallRunResult.Skipped].
 */
private class RunSkipped(message: String) : Exception(message)

private data class CommandResult(val code: Int, val output: String)

class InstallViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repository = PayloadRepository(application)
    private val historyStore = InstallHistoryStore(application)
    private val mutableState = MutableStateFlow(InstallUiState())
    private val mutableHistory = MutableStateFlow(historyStore.closeInterruptedRuns())
    private var discoveryJob: Job? = null
    private var installJob: Job? = null
    // Volatile because a delete arriving from the History page runs on a
    // different thread from the install job that keeps writing this entry.
    @Volatile
    private var activeHistoryEntry: InstallHistoryEntry? = null
    val state: StateFlow<InstallUiState> = mutableState.asStateFlow()
    val history: StateFlow<List<InstallHistoryEntry>> = mutableHistory.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (installJob?.isActive == true) return
        mutableHistory.value = historyStore.prune(HISTORY_LIMIT, activeHistoryEntry?.id)
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch(Dispatchers.IO) {
            val probe = NativeProbe.run()
            val next = when {
                detectInstalled() -> InstallUiState(
                    phase = InstallPhase.Installed,
                    message = app.getString(R.string.status_ksu_active),
                    probeOutput = probe,
                    log = probe,
                    kernelSu = runCatching {
                        repository.resolveTarget(DeviceSnapshot.current()).profile.kernelSu
                    }.getOrNull(),
                )
                else -> try {
                    val target = repository.resolveTarget(DeviceSnapshot.current())
                    InstallUiState(
                        phase = InstallPhase.Ready,
                        message = app.getString(R.string.status_not_installed),
                        probeOutput = probe,
                        log = "$probe\n${app.getString(R.string.log_profile, target.profile.profileId)}",
                        kernelSu = target.profile.kernelSu,
                    )
                } catch (error: Throwable) {
                    InstallUiState(
                        phase = InstallPhase.Failed,
                        message = app.getString(R.string.status_support_failed),
                        probeOutput = probe,
                        log = "$probe\n[-] ${error.message ?: error.javaClass.simpleName}",
                    )
                }
            }
            if (isActive) mutableState.value = next
        }
    }

    fun install() {
        if (installJob?.isActive == true || mutableState.value.phase == InstallPhase.Installed) return
        discoveryJob?.cancel()
        // install() is called from the installer activity's main-thread event
        // path. Request the foreground service here, before the long IO/native
        // work is launched; starting it from the IO coroutine can leave the
        // service waiting behind a busy app main thread and trigger Android's
        // ForegroundServiceDidNotStartInTimeException.
        val keepAliveStarted = runCatching {
            ExploitKeepAliveService.start(app)
            true
        }.getOrDefault(false)
        installJob = viewModelScope.launch(Dispatchers.IO) {
            mutableState.value = InstallUiState(
                phase = InstallPhase.Checking,
                probeOutput = mutableState.value.probeOutput,
                // Carried across the reset: the run is about to resolve the
                // same profile again, and the manager it needs does not stop
                // being true while the install is in flight.
                kernelSu = mutableState.value.kernelSu,
            )
            startHistory()
            try {
                if (!shizukuEnabled()) {
                    appendLog(app.getString(R.string.log_shizuku_required))
                    error(app.getString(R.string.error_shizuku_required))
                }
                appendLog(app.getString(R.string.log_shizuku_prepare))
                if (!ShizukuController.isRunning() && !ShizukuController.pingUntilRunning()) {
                    error(app.getString(R.string.error_shizuku_unavailable))
                }
                if (!ShizukuController.isGranted() && !ShizukuController.requestPermission()) {
                    error(app.getString(R.string.error_shizuku_permission))
                }
                appendLog(app.getString(R.string.log_shizuku_permission))
                requestIdleWhitelist()
                // Before anything is fetched: a run cannot leak the kernel base while
                // the file it reads the answer out of is mounted over, and a reboot is
                // the only way to clear that. See leakChannelPinned().
                if (leakChannelPinned()) throw RunSkipped(app.getString(R.string.error_leak_channel_pinned))
                setPhase(InstallPhase.Checking, app.getString(R.string.status_checking_github))
                val target = repository.resolveTarget(DeviceSnapshot.current())
                mutableState.value = mutableState.value.copy(
                    payloadTag = target.releaseTag,
                    kernelSu = target.profile.kernelSu,
                )
                appendLog(app.getString(R.string.log_profile, target.profile.profileId))
                appendLog(app.getString(R.string.log_payload_release, target.releaseTag))

                setPhase(InstallPhase.Downloading, app.getString(R.string.status_downloading_payload))
                val payloads = repository.download(target) { appendLog("[*] $it") }
                appendLog(app.getString(R.string.log_download_verified))

                setPhase(InstallPhase.Exploiting, app.getString(R.string.status_exploit_running))
                executeExploit(payloads.exploit)

                setPhase(InstallPhase.LoadingKernelSu, app.getString(R.string.status_ksu_loading))
                installKernelSu(payloads)

                setPhase(InstallPhase.Installed, app.getString(R.string.status_ksu_active))
                appendLog(app.getString(R.string.log_install_complete))
                finishHistory(InstallRunResult.Succeeded)
            } catch (skipped: RunSkipped) {
                // Not a failure: the run was refused before it touched anything, and
                // the reader should not go looking for a cause in the log.
                appendLog("[*] ${skipped.message}")
                setPhase(InstallPhase.Skipped, app.getString(R.string.status_install_skipped))
                finishHistory(InstallRunResult.Skipped)
            } catch (error: Throwable) {
                appendLog("[-] ${error.message ?: error.javaClass.simpleName}")
                setPhase(InstallPhase.Failed, app.getString(R.string.status_install_failed))
                finishHistory(InstallRunResult.Failed)
            } finally {
                if (keepAliveStarted) ExploitKeepAliveService.stop(app)
            }
        }
    }

    fun deleteHistoryEntry(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Dropping the reference first is what makes the delete stick: a run
            // still in flight rewrites its own file on every log line, so leaving
            // it attached would put the entry straight back.
            if (activeHistoryEntry?.id == id) activeHistoryEntry = null
            historyStore.delete(id)
            mutableHistory.value = historyStore.load()
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            activeHistoryEntry = null
            historyStore.clearAll()
            // The entry logs are copies of these files, which the payload writes and
            // which otherwise survive until a later run sweeps them. Clearing the
            // copies and leaving the originals is not a clear.
            RunScratch.sweep(exploitLogDirectory(), keep = null)
            mutableHistory.value = emptyList()
        }
    }

    private suspend fun executeExploit(payload: File) {
        // Fresh name per run, and the previous run's logs swept, for the same reason
        // the payload files are named that way -- see RunScratch. The logs have a
        // directory of their own so that sweeping it cannot reach anything else.
        val run = RunScratch.token()
        RunScratch.sweep(exploitLogDirectory(), run)
        val shizuku = shizukuEnabled()
        val logFile = if (shizuku) File(SHIZUKU_LOG_PATH) else exploitLogFile(run)
        if (shizuku) {
            ShizukuController.exec(arrayOf("rm", "-f", SHIZUKU_LOG_PATH)).waitFor()
        } else {
            logFile.delete()
        }

        val helper = if (shizuku) {
            shizukuStage(nativeHelperFile(), SHIZUKU_HELPER_PATH, "755")
        } else {
            helperFile()
        }
        require(shizuku || helper.canExecute()) { app.getString(R.string.error_helper_unavailable) }
        val logPrefix = mutableState.value.log
        val bootToken = currentBootToken()
        val process = if (shizuku) {
            // Use the helper's own runner, just like the proven adb invocation. The
            // runner forks a session-separated child, redirects the payload output to
            // the durable log, and sets CVE43499_ROOT_HELPER from argv[3]. Running the
            // payload through LD_PRELOAD in `sh -c true` changes the process and
            // scheduler context and makes the final workqueue publication race much
            // less reliable on this device.
            val stagedPayload = shizukuStage(payload, SHIZUKU_PAYLOAD_PATH, "755")
            ShizukuController.exec(
                arrayOf(
                    helper.absolutePath,
                    "--run-payload",
                    stagedPayload.absolutePath,
                    helper.absolutePath,
                    logFile.absolutePath,
                ),
                shizukuRunnerEnvironment(bootToken, helper.absolutePath),
            )
        } else {
            val processBuilder = ProcessBuilder(
                helper.absolutePath,
                "--run-payload",
                payload.absolutePath,
                helper.absolutePath,
                logFile.absolutePath,
            ).redirectErrorStream(true)
            processBuilder.environment().apply {
                put("EXPLOIT_ATTEMPTS", EXPLOIT_ATTEMPTS)
                put("P0_ATTEMPT_TIMEOUT_SEC", P0_ATTEMPT_TIMEOUT_SEC)
                put("EXPLOIT_ATTEMPT_TIMEOUT_SEC", EXPLOIT_ATTEMPT_TIMEOUT_SEC)
                cachedP0Offset(bootToken)?.let { put(P0_OFFSET_ENV, it) }
            }
            processBuilder.start()
        }

        val captured = StringBuilder()
        val readLog: () -> String = if (shizuku) {
            {
                // The durable log is shell-owned (0600), so the app UID cannot read
                // it directly. The runner relays that file over its stdout transport;
                // drain it continuously so the Shizuku pipe cannot fill.
                drainProcessOutput(process, captured)
            }
        } else {
            { logFile.readTextIfPresent() }
        }

        try {
            val startedAt = SystemClock.elapsedRealtime()
            var lastProgressAt = startedAt
            var lastRawLog = ""
            while (process.isAlive) {
                val rawLog = readLog()
                if (rawLog != lastRawLog) {
                    cacheP0Offset(bootToken, rawLog)
                    publishExploitLog(logPrefix, rawLog)
                    lastRawLog = rawLog
                    lastProgressAt = SystemClock.elapsedRealtime()
                }
                val now = SystemClock.elapsedRealtime()
                require(now - lastProgressAt < EXPLOIT_STALL_MILLIS) {
                    app.getString(R.string.error_exploit_stalled)
                }
                require(now - startedAt < EXPLOIT_TOTAL_MILLIS) {
                    app.getString(R.string.error_exploit_timeout)
                }
                delay(if (shizuku) SHIZUKU_LOG_POLL_INTERVAL else LOG_POLL_INTERVAL)
            }

            val exitCode = process.waitFor()
            val rawLog = readLog()
            cacheP0Offset(bootToken, rawLog)
            publishExploitLog(logPrefix, rawLog)
            val earlyOutput = readProcessOutput(process, shizuku).trim()
            require(exitCode == 0) {
                app.getString(
                    R.string.error_payload_exit,
                    exitCode,
                    earlyOutput.takeIf(String::isNotBlank)?.let { " ($it)" } ?: "",
                )
            }
            require(rawLog.contains("exploit completed") && rawLog.contains("done=1 root=1")) {
                app.getString(R.string.error_success_marker)
            }
        } finally {
            if (process.isAlive) {
                process.destroy()
                delay(500.milliseconds)
                if (process.isAlive) process.destroyForcibly()
            }
        }
        appendLog(app.getString(R.string.log_bootstrap_root))
    }

    private fun publishExploitLog(prefix: String, rawLog: String) {
        mutableState.value = mutableState.value.copy(
            log = listOf(prefix, stripAnsi(rawLog))
                .filter(String::isNotBlank)
                .joinToString("\n"),
        )
        updateHistoryEntry()
    }

    private fun installKernelSu(payloads: VerifiedPayloads) {
        if (shizukuEnabled()) {
            shizukuStage(payloads.kernelSu, SHIZUKU_KSUD_PATH, "755")
            shizukuStage(payloads.kernelSu, SHIZUKU_KSUD_STAGE_PATH, "755")
        } else {
            val source = shellQuote(payloads.kernelSu.absolutePath)
            val stageCommand =
                "/system/bin/cp $source /data/local/tmp/ksud-iqoo-z9-5g && " +
                    "/system/bin/cp $source /data/local/tmp/ksud && " +
                    "/system/bin/cp $source /data/local/tmp/.ksud-stage && " +
                    "/system/bin/chmod 755 /data/local/tmp/ksud-iqoo-z9-5g /data/local/tmp/ksud /data/local/tmp/.ksud-stage"
            val stage = runHelper("-c", stageCommand)
            require(stage.code == 0) { app.getString(R.string.error_ksu_stage, stage.output) }
        }
        appendLog(app.getString(R.string.log_ksu_staged))

        // The helper protocol recognizes the late-load operation only when the
        // request is exactly `--late-load`; the target-specific KMI, loader path,
        // and manager package are compiled into the paired helper/ksud artifacts.
        // Passing metadata as extra argv entries makes the request fall through to
        // the normal shell-command path (`sh: sh:--: unknown option`).
        val lateLoad = runHelper("--late-load")
        require(lateLoad.code == 0) {
            app.getString(R.string.error_ksu_verify, lateLoad.code, lateLoad.output)
        }
        if (lateLoad.output.isNotBlank()) appendLog(lateLoad.output)
        storeInstallReceipt()
        appendLog(app.getString(R.string.log_ksu_control_verified))
    }

    private fun detectInstalled(): Boolean {
        if (NativeProbe.isKernelSuActive()) return true
        val bootToken = currentBootToken() ?: return false
        val receipt = app.getSharedPreferences(INSTALL_RECEIPT, Application.MODE_PRIVATE)
        return receipt.getString(RECEIPT_BOOT_TOKEN, null) == bootToken &&
            receipt.getBoolean(RECEIPT_VERIFIED, false)
    }

    private fun storeInstallReceipt() {
        val bootToken = currentBootToken() ?: error(app.getString(R.string.error_boot_id))
        val stored = app.getSharedPreferences(INSTALL_RECEIPT, Application.MODE_PRIVATE)
            .edit()
            .putString(RECEIPT_BOOT_TOKEN, bootToken)
            .putBoolean(RECEIPT_VERIFIED, true)
            .commit()
        require(stored) { app.getString(R.string.error_receipt) }
    }

    private fun currentBootToken(): String? = runCatching {
        File("/proc/sys/kernel/random/boot_id")
            .readText(Charsets.US_ASCII)
            .trim()
            .takeIf(String::isNotBlank)
    }.getOrNull()

    private fun cachedP0Offset(bootToken: String?): String? {
        if (bootToken == null) return null
        val stored = app.getSharedPreferences(P0_CACHE, Application.MODE_PRIVATE)
        if (stored.getString(P0_CACHE_BOOT_TOKEN, null) != bootToken) return null
        return stored.getString(P0_CACHE_OFFSET, null)
    }

    private fun cacheP0Offset(bootToken: String?, log: String) {
        if (bootToken == null) return
        val match = P0_OFFSET_PATTERN.findAll(log).lastOrNull() ?: return
        val offset = match.groupValues[1].toLongOrNull(16) ?: return
        if (offset !in 0..P0_OFFSET_MAX || offset and P0_OFFSET_MASK != 0L) return
        val value = "0x${offset.toString(16)}"
        val stored = app.getSharedPreferences(P0_CACHE, Application.MODE_PRIVATE)
        if (stored.getString(P0_CACHE_BOOT_TOKEN, null) == bootToken &&
            stored.getString(P0_CACHE_OFFSET, null) == value
        ) return
        stored.edit()
            .putString(P0_CACHE_BOOT_TOKEN, bootToken)
            .putString(P0_CACHE_OFFSET, value)
            .apply()
    }

    /**
     * Whether a previous run has pinned `/proc/sys/kernel/random/boot_id`.
     *
     * The exploit leaks the kernel base by reading that file, and it has no other
     * channel at that point in the run -- establishing a kernel read is what the leak
     * is for. After a run takes root, `su_daemon`'s `pin_boot_id()` bind-mounts the
     * boot id the device actually started with over the same path, so that libcutils
     * can keep computing `/dev/ashmem<boot_id>` and applications keep launching.
     *
     * Both are needed and they want the same file. While the mount is there the leak
     * reads a real UUID, every attempt is rejected, and the run spends fifteen minutes
     * failing identically. A mount does not survive a reboot, and a pin only exists
     * because this boot has already been rooted once, so refusing here and saying so
     * is the whole of the fix.
     */
    private fun leakChannelPinned(): Boolean = runCatching {
        // Mount targets are the second field, so match the path with its separators.
        File("/proc/self/mounts").readText().contains(" /proc/sys/kernel/random/boot_id ")
    }.getOrDefault(false)

    private fun exploitLogDirectory() =
        File(app.filesDir, "exploit-logs").apply { mkdirs() }

    private fun exploitLogFile(run: String) = File(exploitLogDirectory(), "exploit-$run.log")

    private fun helperFile() = File(app.applicationInfo.nativeLibraryDir, "libcve43499root.so")

    private fun nativeHelperFile() = helperFile()

    private fun shizukuEnabled(): Boolean = AppPreferences.shizukuMode(app)

    /**
     * Ask Android's device-idle service to exempt this package while Shizuku is
     * authenticated.  This is deliberately best effort: it protects against
     * Doze/app-standby reclamation, but vendor power managers can still force-stop
     * an app and that must not turn a usable exploit run into an immediate failure.
     */
    private fun requestIdleWhitelist() {
        appendLog(app.getString(R.string.log_idle_whitelist_request))
        val process = try {
            ShizukuController.exec(
                arrayOf(
                    "cmd",
                    "deviceidle",
                    "whitelist",
                    "+${app.packageName}",
                ),
            )
        } catch (error: Throwable) {
            appendLog("[!] Android idle whitelist unavailable: ${error.message ?: error.javaClass.simpleName}")
            return
        }

        try {
            val output = readProcessOutput(process, shizuku = true).trim()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                appendLog(app.getString(R.string.log_idle_whitelist_success))
            } else {
                appendLog("[!] Android idle whitelist command failed (exit $exitCode)${output.takeIf(String::isNotBlank)?.let { ": $it"} ?: ""}")
            }
        } catch (error: Throwable) {
            appendLog("[!] Android idle whitelist check failed: ${error.message ?: error.javaClass.simpleName}")
        } finally {
            if (process.isAlive) process.destroy()
        }
    }

    private fun shizukuStage(source: File, target: String, mode: String): File {
        try {
            ShizukuController.writeFile(target, mode, source.inputStream())
        } catch (error: Throwable) {
            throw IllegalStateException(
                app.getString(R.string.error_shizuku_stage, target, error.message.orEmpty()),
                error,
            )
        }
        return File(target)
    }

    private fun shizukuRunnerEnvironment(
        bootToken: String?,
        helperPath: String,
    ): Array<String> = buildList {
        add("EXPLOIT_ATTEMPTS=$EXPLOIT_ATTEMPTS")
        add("P0_ATTEMPT_TIMEOUT_SEC=$P0_ATTEMPT_TIMEOUT_SEC")
        add("EXPLOIT_ATTEMPT_TIMEOUT_SEC=$EXPLOIT_ATTEMPT_TIMEOUT_SEC")
        add("CVE43499_ROOT_HELPER=$helperPath")
        // Match the old adb runbook. In particular, avoid the app's `auto` route
        // selecting a different leak source after the helper has started.
        add("SLIDE_SOURCE=tracefs")
        cachedP0Offset(bootToken)?.let { add("$P0_OFFSET_ENV=$it") }
    }.toTypedArray()

    private fun runHelper(vararg arguments: String): CommandResult {
        val shizuku = shizukuEnabled()
        val helper = if (shizuku) {
            // executeExploit() stages this helper before launching the payload. Do
            // not copy it again after the exploit has changed kernel/SELinux state:
            // the second shell-UID write is unnecessary and can be rejected even
            // though the staged executable is still valid.
            File(SHIZUKU_HELPER_PATH)
        } else {
            helperFile()
        }
        val process = if (shizuku) {
            ShizukuController.exec(arrayOf(helper.absolutePath) + arguments)
        } else {
            ProcessBuilder(listOf(helper.absolutePath) + arguments)
                .redirectErrorStream(true)
                .start()
        }
        val output = readProcessOutput(process, shizuku)
        return CommandResult(process.waitFor(), stripAnsi(output.trim()))
    }

    private fun readProcessOutput(process: Process, shizuku: Boolean): String {
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = if (shizuku) process.errorStream.bufferedReader().use { it.readText() } else ""
        return stdout + stderr
    }

    private fun shellQuote(value: String) = "'${value.replace("'", "'\\''")}'"

    private fun setPhase(phase: InstallPhase, message: String) {
        mutableState.value = mutableState.value.copy(phase = phase, message = message)
        appendLog("[*] $message")
    }

    private fun appendLog(line: String) {
        val cleanLine = stripAnsi(line).trim()
        if (cleanLine.isBlank()) return
        mutableState.value = mutableState.value.copy(
            log = (mutableState.value.log + "\n" + cleanLine).trim(),
        )
        updateHistoryEntry()
    }

    private fun startHistory() {
        val entry = historyStore.create()
        activeHistoryEntry = entry
        publishHistory(entry)
    }

    private fun updateHistoryEntry() {
        val entry = activeHistoryEntry ?: return
        val updated = entry.copy(
            log = mutableState.value.log,
            payloadTag = mutableState.value.payloadTag,
        )
        activeHistoryEntry = updated
        historyStore.save(updated)
        publishHistory(updated)
    }

    private fun finishHistory(result: InstallRunResult) {
        val entry = activeHistoryEntry ?: return
        val completed = entry.copy(
            completedAtMillis = System.currentTimeMillis(),
            result = result,
            log = mutableState.value.log,
        )
        activeHistoryEntry = null
        historyStore.save(completed)
        // The cap is applied here as well as on refresh(), so that a session
        // that installs repeatedly without a restart still stays bounded.
        mutableHistory.value = historyStore.prune(HISTORY_LIMIT)
    }

    private fun publishHistory(entry: InstallHistoryEntry) {
        mutableHistory.value = (mutableHistory.value.filterNot { it.id == entry.id } + entry)
            .sortedByDescending(InstallHistoryEntry::startedAtMillis)
    }

    private fun File.readTextIfPresent(): String = if (exists()) readText() else ""

    private fun drainProcessOutput(process: Process, buffer: StringBuilder): String {
        return try {
            drainStream(process.inputStream, buffer)
            drainStream(process.errorStream, buffer)
            buffer.toString()
        } catch (_: Throwable) {
            buffer.toString()
        }
    }

    private fun drainStream(stream: InputStream, buffer: StringBuilder) {
        val data = ByteArray(4096)
        while (stream.available() > 0) {
            val count = stream.read(data)
            if (count <= 0) break
            buffer.append(String(data, 0, count, Charsets.UTF_8))
        }
    }

    companion object {
        private const val HISTORY_LIMIT = 20
        private const val EXPLOIT_STALL_MILLIS = 90_000L
        private const val EXPLOIT_TOTAL_MILLIS = 900_000L
        private const val INSTALL_RECEIPT = "install_receipt"
        private const val RECEIPT_BOOT_TOKEN = "kernel_boot_id"
        private const val RECEIPT_VERIFIED = "verified"
        private const val P0_CACHE = "p0_cache"
        private const val P0_CACHE_BOOT_TOKEN = "kernel_boot_id"
        private const val P0_CACHE_OFFSET = "offset"
        private const val P0_OFFSET_ENV = "SLIDE_P0_OFFSET"
        private const val EXPLOIT_ATTEMPTS = "1"
        private const val P0_ATTEMPT_TIMEOUT_SEC = "115"
        private const val EXPLOIT_ATTEMPT_TIMEOUT_SEC = "600"
        // Samsung's placement offset fits in 0x1f0000; a real arm64 KASLR slide does
        // not -- this device reports around 0x106c600000.
        private const val P0_OFFSET_MAX = 0x4000000000L
        private const val P0_OFFSET_MASK = 0xffffL
        private const val SHIZUKU_LOG_PATH = "/data/local/tmp/ghostlock-exploit.log"
        private const val SHIZUKU_HELPER_PATH = "/data/local/tmp/ghostlock-helper"
        private const val SHIZUKU_PAYLOAD_PATH = "/data/local/tmp/ghostlock-payload"
        private const val SHIZUKU_KSUD_PATH = "/data/local/tmp/ksud-iqoo-z9-5g"
        private const val SHIZUKU_KSUD_STAGE_PATH = "/data/local/tmp/.ksud-stage"
        private val LOG_POLL_INTERVAL = 250.milliseconds
        private val SHIZUKU_LOG_POLL_INTERVAL = 1.seconds
        private val ANSI_ESCAPE = Regex("\u001B\\[[0-?]*[ -/]*[@-~]")
        private val P0_OFFSET_PATTERN = Regex(
            "slide-kaslr-ok[^\\n]*slide=([0-9a-fA-F]{16})",
        )

        private fun stripAnsi(value: String): String = ANSI_ESCAPE.replace(value, "").replace("\r", "")
    }
}
