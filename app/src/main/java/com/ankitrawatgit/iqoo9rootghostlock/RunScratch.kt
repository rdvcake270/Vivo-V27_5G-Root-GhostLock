package com.ankitrawatgit.iqoo9rootghostlock

import java.io.File
import java.util.UUID

/**
 * Names for the files a run writes, so that a run never has to replace a file an
 * earlier run left behind.
 *
 * It cannot count on being able to. On warhol a run leaves some of its own files with
 * no SELinux label -- the kernel reports `u:object_r:unlabeled:s0`, which is what it
 * says when a file's `security.selinux` xattr is missing or names a type the loaded
 * policy does not have. Policy grants `untrusted_app` nothing on that type, so the app
 * is denied `getattr`, `unlink` and `rename` on its own file:
 *
 *     avc: denied { unlink } for name="cve-2026-43499-app.so"
 *       scontext=u:r:untrusted_app:s0:c85,c257,c512,c768
 *       tcontext=u:object_r:unlabeled:s0 tclass=file permissive=0
 *
 * A fixed destination name is unrecoverable once that happens: the file cannot be
 * deleted, cannot be renamed over, and the download has nowhere to land. It reads only
 * as "cannot finalize", because `File.renameTo` answers a boolean. The label is in an
 * xattr, so it survives a reboot, and the app -- which is the party without root -- has
 * no way to repair it.
 *
 * Writing to a name nothing has used sidesteps the whole question: there is no inode to
 * replace. [sweep] is what stops that accumulating, and it ignores every failure on
 * purpose, since a file that cannot be deleted is the exact case this exists for.
 *
 * Repairing the labels, and stopping a run from stripping them, are both jobs for the
 * side that has root; neither is this.
 */
object RunScratch {

    /** A token no earlier run used, short enough to keep the file names readable. */
    fun token(): String = UUID.randomUUID().toString().substring(0, 8)

    /**
     * Delete everything in [directory] that is not tagged [keep], best effort. A null
     * [keep] spares nothing.
     *
     * Called before a run writes anything, so "not tagged [keep]" means "left by an
     * earlier run". Files that refuse to be deleted are the damaged ones, and skipping
     * them is the point -- they are why nothing here is allowed to throw.
     */
    fun sweep(directory: File, keep: String?) {
        val stale = directory.listFiles() ?: return
        for (file in stale) {
            if (keep != null && file.name.contains(keep)) continue
            runCatching { file.delete() }
        }
    }
}
