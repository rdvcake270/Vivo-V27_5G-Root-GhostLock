# Convenience targets for the iQOO Z9 5G-only project.

ANDROID_NDK_HOME ?= $(if $(ANDROID_NDK_ROOT),$(ANDROID_NDK_ROOT),$(if $(ANDROID_HOME),$(ANDROID_HOME)/ndk/28.2.13676358,))

.PHONY: all payload apk apk-release verify clean

all: apk-release

payload:
	@test -n "$(ANDROID_NDK_HOME)" || (echo "Set ANDROID_NDK_HOME to your Android NDK 28.2.13676358 directory" >&2; exit 1)
	$(MAKE) -C payload ANDROID_NDK_HOME="$(ANDROID_NDK_HOME)" payload

apk: apk-release

apk-release: payload
	./gradlew :app:assembleRelease --no-daemon

verify:
	@test -n "$(ANDROID_NDK_HOME)" || (echo "Set ANDROID_NDK_HOME to your Android NDK 28.2.13676358 directory" >&2; exit 1)
	$(MAKE) -C payload ANDROID_NDK_HOME="$(ANDROID_NDK_HOME)" verify

clean:
	./gradlew clean --no-daemon
	$(MAKE) -C payload clean
