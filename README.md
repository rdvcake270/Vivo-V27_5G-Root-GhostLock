# iQOO Z9 5G / vivo T3 5G Jailbreak Root (GhostLock)

[Installed KernelSU](installed_ksu.jpg)

An iQOO Z9 5G and vivo T3 5G jailbreak/root Android application and
payloads. Both devices use the MediaTek Dimensity 7200 (MT6886) platform.

There are two supported run methods: the Android APK with Shizuku, or the
native helper run directly from an `adb shell`. Both methods use the same
device-specific payload and KernelSU daemon.

## CVE and scope

This port uses the CVE-2026-43499 Ghostlock kernel exploit chain to obtain temporary
bootstrap root, then late-loads the matching KernelSU daemon. It is a
device-specific jailbreak/root research build for the iQOO Z9 5G (model I2302)
and vivo T3 5G (model V2334), using kernel
`5.15.178-android13-8-g0ebe6a5da65d`. It is not a general Android rooting tool
and must only be used on hardware you own or are authorized to test.

For instructions on adapting this project to another device, see the
[Port to another device](#porting-guide) guide.

## Support the project

This project took a lot of effort and money to complete. If it helped you,
you can support the work with a coffee:

[![Buy Me a Coffee](https://www.buymeacoffee.com/assets/img/custom_images/yellow_img.png)](https://coff.ee/ankitrawatbmac)

Thanks to Codex for helping with the development.

## Project shape

```text
app/       Android/Compose application source and UI resources
payload/   iQOO exploit source, target profile, build script, and release inputs
```

The runtime flow is:

1. The app verifies the phone model is `I2302` or `V2334`, the running kernel is
   `5.15.178-android13-8-g0ebe6a5da65d`, and the device is arm64.
2. During installation it downloads the exact exploit and KernelSU
   artifacts from the latest GitHub payload release(Storing files locally in apk is causing a crash because of the system's security features.).
3. Shizuku is required: it starts the native helper compiled from
   `payload/src/su_daemon.c` as the shell service (UID 2000), which is the
   execution context needed by this port. The app invokes the helper's
   `--run-payload` supervisor, the same child/session handoff used by the
   validated adb runbook, and follows its durable exploit log.
4. After the exploit obtains temporary root, the helper stages the downloaded
   `ksud`, performs the guarded KernelSU late-load, and verifies the control
   channel.

> **Important recovery note:** If the phone becomes stuck or does not boot,
> hold **Volume Down + Power** together until it force-restarts.

Download the latest APK from the [GitHub Releases](https://github.com/ankitrawatgit/iQOO-Z9_5G-Root-GhostLock/releases) page.

Download the official [KernelSU Manager APK](https://github.com/tiann/KernelSU/releases)
from the KernelSU releases page.

## Running from the APK with Shizuku (Shizuku required for apk method)

An ordinary Android app runs in the `untrusted_app` SELinux domain. On this
device that domain cannot read tracefs, so launching the helper directly from
the APK makes the payload fall back to the physical oracle and usually fail at
the pipe gate. Shizuku is therefore required for a working APK installation.
The app starts the helper's `--run-payload` supervisor through the Shizuku
shell service (UID 2000), which gives the tracefs route the same execution
context and process handoff as the proven `adb shell` run.

The Settings page still exposes a **without Shizuku** switch for diagnostics
and future development. It is explicitly marked unsupported; turning it off
causes installation to stop before the exploit starts.

### Required setup

1. Install Shizuku from its [official releases](https://github.com/RikkaApps/Shizuku/releases).
2. On the phone, enable Developer options and Wireless debugging.
3. Open Shizuku, choose **Start via Wireless debugging**, pair it with the
   pairing code shown by Android, and tap **Start**. Android requires this
   startup again after a reboot when using the wireless-debugging method; see
   the [official Shizuku setup guide](https://shizuku.rikka.app/guide/setup/).
4. Open **iQOO Z9 5G Jailbreak Root (GhostLock)**. In **Settings → Execution
   privilege**, leave **Use Shizuku (required)** enabled and approve the app's
   permission request in Shizuku.


### Each install attempt

Reboot the phone first; this port allows one exploit attempt per boot. Start
Shizuku, confirm the jailbreak/root app still has permission and that **Use
Shizuku (required)** is enabled, then tap **Install KernelSU**.
The live log should contain `Shizuku permission granted` before the payload
starts. Keep the phone awake and connected to power while the exploit runs.

If Shizuku is stopped, permission is revoked, or the device is rebooted while
the run is in progress, stop and reboot before trying again. Do not repeatedly
retry the exploit on the same boot. Shizuku mode only changes how the helper is
launched; it does not make this device-specific payload portable to another
model or kernel.

## Running directly from an adb shell (without the APK)

The APK is optional. An `adb shell` already runs as Android's shell UID, so it
provides the tracefs access that the APK obtains through Shizuku. This is the
original diagnostic/runbook path and does not require Shizuku.

Use a fresh boot for every attempt. The stack writer is one-shot per boot; do
not rerun the exploit after `stack writer ran; refusing retry on this boot`.

From the repository root, stage the matching iQOO binaries:

```sh
adb reboot

# Wait for Android to finish booting, then push the payload and helper.
adb push payload/build/cve-2026-43499-app.so \
  /data/local/tmp/iqoo-app.so
adb push payload/build/cve-2026-43499-root \
  /data/local/tmp/cve-2026-43499-root
adb push payload/artifacts/ksud-iqoo-z9-5g \
  /data/local/tmp/ksud-iqoo-z9-5g

adb shell chmod 755 \
  /data/local/tmp/cve-2026-43499-root \
  /data/local/tmp/ksud-iqoo-z9-5g
adb shell rm -f /data/local/tmp/iqoo-app-run.log
```

Start the helper's supervisor. Keep this terminal attached and wait for it to
finish; a normal run can take several minutes:

```sh
adb shell 'SLIDE_SOURCE=tracefs EXPLOIT_ATTEMPTS=1 \
  P0_ATTEMPT_TIMEOUT_SEC=115 EXPLOIT_ATTEMPT_TIMEOUT_SEC=600 \
  /data/local/tmp/cve-2026-43499-root --run-payload \
  /data/local/tmp/iqoo-app.so /data/local/tmp/cve-2026-43499-root \
  /data/local/tmp/iqoo-app-run.log'
```

The exploit stage is complete only when the log contains both
`exploit completed` and `root=1`. If bootstrap root succeeds, late-load the
matching KernelSU daemon with the helper's exact one-argument operation:

```sh
adb shell '/data/local/tmp/cve-2026-43499-root --late-load'
```

Do not append KMI or manager arguments to `--late-load`; this target's helper
already has the iQOO KMI, loader path, and KernelSU options compiled in. A
successful late-load prints a KernelSU control verification message.

## Build payload

Requirements: Android SDK 37, NDK `28.2.13676358`, and CMake 3.22.1.

Set the NDK path once, then build the standalone payload artifacts:

```sh
export ANDROID_NDK_HOME="/path/to/android-sdk/ndk/28.2.13676358"
make -C payload all
```

## Build release APK

```sh
make apk-release
```

The release target rebuilds the local standalone payload artifacts when needed;
the APK still downloads its runtime payloads from the latest GitHub release.


The signed APK is written to
`app/build/outputs/apk/release/app-release.apk`.

Kernel source, module objects, and init companions remain outside this clean
checkout.

## Compatibility warning

This is a highly device-specific port. The exploit offsets, PAC/KASLR logic,
stack geometry, kernel ABI, module metadata, and KernelSU daemon all match the
tested iQOO Z9 5G (I2302) and vivo T3 5G (V2334) devices with the kernel above.
The binaries should not be expected to work on another model, firmware, kernel
release, or materially different build; they may abort, freeze, or panic an
incompatible device. A different device needs its own target profile, source
audit, and hardware validation.

KernelSU is late-loaded once per boot and is not a persistent boot-image
modification. Follow the runbook in the original port documentation when
testing the device, and always keep recovery access available.

Use only on hardware that you own or are explicitly authorized to test.

## Porting guide

This source currently supports the iQOO Z9 5G (I2302) and vivo T3 5G (V2334)
with the listed kernel family. For another device in the same kernel family,
use it as a starting point and replace the device-specific values after
validating them on that device. For a different kernel family such as 5.10 or
6.x, first find a matching public source, exploit port, or reference on GitHub
and adapt the profile for that kernel.

You will need the device's exact `boot.img` and matching kernel source tree.
AI coding agents can help inspect those files, prepare the build, and update
the port. Codex with the Luna model is my recommendation. Build and test with
the direct adb method first, not the APK. Run the agent's build, execute it on
your own device, then return the complete output and any panic logs to the
agent. Repeat that build/test/log cycle until the port succeeds.
