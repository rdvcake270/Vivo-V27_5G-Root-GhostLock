package com.ankitrawatgit.iqoo9rootghostlock

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class InstallRunResult {
    Running,
    Succeeded,
    Failed,

    /**
     * The run was refused before it started, because this boot cannot win it --
     * currently only when the kernel-base leak channel is already mounted over.
     *
     * Distinct from [Failed] because nothing was attempted and nothing is wrong:
     * calling it a failure sends the reader looking for a cause in a log that
     * only says a reboot is needed.
     */
    Skipped,
}

data class InstallHistoryEntry(
    val id: String,
    val startedAtMillis: Long,
    val completedAtMillis: Long?,
    val result: InstallRunResult,
    val log: String,
    // The payload release the run read its artifacts from. Null for a run that
    // failed before resolving one, and for every entry written before this was
    // recorded -- JSONObject.isNull answers true for a key that is not there.
    val payloadTag: String?,
)

class InstallHistoryStore(context: Context) {
    private val directory = File(context.filesDir, "install-history").apply { mkdirs() }

    fun load(): List<InstallHistoryEntry> = directory
        .listFiles { file -> file.extension == "json" }
        .orEmpty()
        .mapNotNull(::decodeOrQuarantine)
        .sortedByDescending(InstallHistoryEntry::startedAtMillis)

    fun closeInterruptedRuns(): List<InstallHistoryEntry> = load().map { entry ->
        if (entry.result == InstallRunResult.Running) {
            entry.copy(
                completedAtMillis = System.currentTimeMillis(),
                result = InstallRunResult.Failed,
            ).also(::save)
        } else {
            entry
        }
    }

    fun create(): InstallHistoryEntry = InstallHistoryEntry(
        id = UUID.randomUUID().toString(),
        startedAtMillis = System.currentTimeMillis(),
        completedAtMillis = null,
        result = InstallRunResult.Running,
        log = "",
        payloadTag = null,
    ).also(::save)

    fun delete(id: String) {
        // AtomicFile rather than File: save() writes through AtomicFile, which
        // parks a backup beside the entry. Deleting the entry alone leaves that
        // backup behind, and nothing ever comes back for it.
        AtomicFile(File(directory, "$id.json")).delete()
    }

    fun clearAll() {
        // Every file in the directory, not the result of load(): the quarantined
        // copies and the AtomicFile backups are precisely what load() filters
        // out, so clearing what it returns would leave the junk behind.
        directory.listFiles().orEmpty().forEach(File::delete)
    }

    /**
     * Keeps the [keep] newest runs and drops the rest, along with every
     * quarantined copy. [keepId] survives the cut whatever its age -- it is the
     * run being written right now, and its file is about to be rewritten anyway.
     */
    fun prune(keep: Int, keepId: String? = null): List<InstallHistoryEntry> {
        val entries = load()
        // After load(), not before: load() is what quarantines a file it cannot
        // decode, so sweeping first would leave this run's casualties for the next.
        directory.listFiles { file -> file.extension == "corrupt" }
            .orEmpty()
            .forEach(File::delete)
        val (kept, dropped) = entries.withIndex().partition { (index, entry) ->
            index < keep || entry.id == keepId
        }
        dropped.forEach { delete(it.value.id) }
        return kept.map { it.value }
    }

    fun save(entry: InstallHistoryEntry) {
        val target = File(directory, "${entry.id}.json")
        val atomicFile = AtomicFile(target)
        val output = atomicFile.startWrite()
        try {
            output.write(encode(entry).toString().toByteArray(Charsets.UTF_8))
            output.flush()
            output.fd.sync()
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    private fun encode(entry: InstallHistoryEntry) = JSONObject()
        .put("id", entry.id)
        .put("startedAtMillis", entry.startedAtMillis)
        .put("completedAtMillis", entry.completedAtMillis ?: JSONObject.NULL)
        .put("result", entry.result.name)
        .put("log", entry.log)
        .put("payloadTag", entry.payloadTag ?: JSONObject.NULL)

    private fun decodeOrQuarantine(file: File): InstallHistoryEntry? = try {
        decode(AtomicFile(file).openRead().use { it.readBytes() })
    } catch (_: Throwable) {
        val quarantined = File(directory, "${file.name}.corrupt")
        quarantined.delete()
        file.renameTo(quarantined)
        null
    }

    private fun decode(bytes: ByteArray): InstallHistoryEntry {
        val value = JSONObject(bytes.toString(Charsets.UTF_8))
        return InstallHistoryEntry(
            id = value.getString("id"),
            startedAtMillis = value.getLong("startedAtMillis"),
            completedAtMillis = if (value.isNull("completedAtMillis")) {
                null
            } else {
                value.getLong("completedAtMillis")
            },
            result = InstallRunResult.valueOf(value.getString("result")),
            log = value.getString("log"),
            payloadTag = if (value.isNull("payloadTag")) null else value.getString("payloadTag"),
        )
    }
}
