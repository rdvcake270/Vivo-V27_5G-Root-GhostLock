# vivo V27 5G Jailbreak Root payload

This directory contains the only native CVE-2026-43499 jailbreak/root payload
source used by the Vivo V27 APK. It targets the following device models and
one kernel:

```text
device: vivo V27 5G (MT6886), model V2231
kernel: 5.15.178-android13-8-00007-g362d545d31a5-ab14608873
writer: SIGRETURN (STACK_WRITER=2)
abi:    arm64-v8a
```

The I2302 and V2334 are the same target for this payload: the SoC, firmware
behavior, kernel release, ABI, exploit offsets, and KernelSU pairing are the
same. The build fingerprint is the expected identity difference, so both
models use the same payload and target profile.

The payload obtains temporary bootstrap root with the CVE-2026-43499 chain;
the app then late-loads the matching KernelSU daemon. This is not a portable
Android root implementation: the offsets, kernel ABI, and module metadata are
specific to this Vivo V27 5G build.

## Layout

- `src/` — exploit, root helper, and the Vivo V27 target profile.
- `artifacts/ksud-vivo-v27-5g` — already-built KernelSU v3.2.5 daemon. It
  embeds the matching `android13-5.15` module and is retained as a release/CI
  input for the standalone tooling.
- Kernel source, standalone module objects, and init companions are
  intentionally not vendored.
- `Makefile` — rebuilds the standalone exploit payload artifacts. The APK
  downloads the app payload and `ksud` from the GitHub release at install time.

The APK's native bootstrap helper is compiled directly from `src/su_daemon.c`
by the app's CMake build. This avoids a second helper source or a submodule.

## Build

From the project root, set the NDK path for your machine and run:

```sh
export ANDROID_NDK_HOME="/path/to/android-sdk/ndk/28.2.13676358"
make verify
```

`make` produces local outputs in `payload/build/`. The prebuilt `ksud` artifact
under `payload/artifacts/` is consumed as-is because rebuilding it
requires the exact recovered Vivo V27 kernel ABI, module metadata, symbol contract,
and the KernelSU source tree; those large inputs are deliberately not vendored
here.

Any change to the target profile, writer, or compiler flags requires fresh
hardware validation. This payload is not portable to another device or
kernel release.

## Run from adb only

This is the direct shell-UID run path; Shizuku and the APK are not required.
Use a fresh boot for every attempt because the stack writer is one-shot per
boot.

From the project root, after building the payload:

```sh
adb reboot

# Wait for Android to finish booting, then stage the matching Vivo V27 binaries.
adb push payload/build/cve-2026-43499-app.so \
  /data/local/tmp/vivo-v27-app.so
adb push payload/build/cve-2026-43499-root \
  /data/local/tmp/cve-2026-43499-root
adb push payload/artifacts/ksud-vivo-v27-5g \
  /data/local/tmp/ksud-vivo-v27-5g
adb shell chmod 755 \
  /data/local/tmp/cve-2026-43499-root \
  /data/local/tmp/ksud-vivo-v27-5g
adb shell rm -f /data/local/tmp/vivo-v27-app-run.log
```

Run the exploit and keep the terminal attached until it finishes:

```sh
adb shell 'SLIDE_SOURCE=tracefs EXPLOIT_ATTEMPTS=1 \
  P0_ATTEMPT_TIMEOUT_SEC=115 EXPLOIT_ATTEMPT_TIMEOUT_SEC=600 \
  /data/local/tmp/cve-2026-43499-root --run-payload \
  /data/local/tmp/vivo-v27-app.so /data/local/tmp/cve-2026-43499-root \
  /data/local/tmp/iqoo-app-run.log'
```

After the log reports `exploit completed` and `root=1`, late-load KernelSU:

```sh
adb shell '/data/local/tmp/cve-2026-43499-root --late-load'
```

Do not retry on the same boot after `stack writer ran; refusing retry on this
boot`. The payload and binaries are specific to the iQOO Z9 5G I2302 and vivo
T3 5G V2334 with the listed kernel.
