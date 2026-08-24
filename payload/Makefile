# iQOO Z9 5G payload build
#
# The app payload is rebuilt from payload/src. The APK downloads the two
# verified, exact-kernel artifacts from the GitHub release at install time.

ANDROID_NDK_HOME ?= $(if $(ANDROID_NDK_ROOT),$(ANDROID_NDK_ROOT),$(if $(ANDROID_HOME),$(ANDROID_HOME)/ndk/28.2.13676358,))
API ?= 35
STACK_WRITER ?= 2
TARGET ?= iqoo-z9-5g
TARGET_DIR ?= targets/$(TARGET)
TARGET_HEADER ?= $(TARGET_DIR)/target.h

ifeq ($(strip $(ANDROID_NDK_HOME)),)
$(error Set ANDROID_NDK_HOME to your Android NDK 28.2.13676358 directory)
endif

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
HOST_TAG := $(firstword $(notdir $(wildcard $(ANDROID_NDK_HOME)/toolchains/llvm/prebuilt/darwin-*)))
else
HOST_TAG := $(firstword $(notdir $(wildcard $(ANDROID_NDK_HOME)/toolchains/llvm/prebuilt/linux-*)))
endif
NDK_BIN := $(ANDROID_NDK_HOME)/toolchains/llvm/prebuilt/$(HOST_TAG)/bin
CC := $(NDK_BIN)/aarch64-linux-android$(API)-clang

SRC_DIR := $(CURDIR)/src
BUILD_DIR := $(CURDIR)/build
ARTIFACTS_DIR := $(CURDIR)/artifacts
TARGET_HEADER_DEFINE := -DTARGET_HEADER='"$(TARGET_HEADER)"'

COMMON_CFLAGS := -O2 -g0 -Wall -Wextra \
  -Wno-unused-parameter -Wno-sign-compare \
  -I"$(SRC_DIR)" $(TARGET_HEADER_DEFINE)

PRELOAD_SRCS := \
  $(SRC_DIR)/main.c $(SRC_DIR)/util.c $(SRC_DIR)/slide.c \
  $(SRC_DIR)/fops.c $(SRC_DIR)/pipe.c $(SRC_DIR)/root.c $(SRC_DIR)/preload.c

APP_SRCS := \
  $(SRC_DIR)/main.c $(SRC_DIR)/util.c $(SRC_DIR)/slide_app.c \
  $(SRC_DIR)/fops.c $(SRC_DIR)/pipe.c $(SRC_DIR)/root.c $(SRC_DIR)/preload.c

PRELOAD := $(BUILD_DIR)/cve-2026-43499
APP_PAYLOAD := $(BUILD_DIR)/cve-2026-43499-app.so
ROOT_HELPER := $(BUILD_DIR)/cve-2026-43499-root
TARGET_FILES := $(SRC_DIR)/$(TARGET_HEADER) $(SRC_DIR)/$(TARGET_DIR)/p0_fingerprint.h

MKDIR_P = mkdir -p "$(BUILD_DIR)"
CLEAN_DIR = rm -rf "$(BUILD_DIR)"
VERIFY_ARTIFACT = test -s "$(ARTIFACTS_DIR)/ksud-iqoo-z9-5g" || (echo "missing verified ksud artifact" >&2; exit 1); shasum -a 256 "$(APP_PAYLOAD)" "$(ARTIFACTS_DIR)/ksud-iqoo-z9-5g"

.PHONY: all payload verify clean info

all: payload

payload: $(PRELOAD) $(APP_PAYLOAD) $(ROOT_HELPER)

$(BUILD_DIR):
	$(MKDIR_P)

$(PRELOAD): $(PRELOAD_SRCS) $(TARGET_FILES) | $(BUILD_DIR)
	$(CC) -fPIC $(COMMON_CFLAGS) $(PRELOAD_SRCS) -shared -pthread -o "$@"

$(APP_PAYLOAD): $(APP_SRCS) $(TARGET_FILES) | $(BUILD_DIR)
	$(CC) -DAPP_PAYLOAD=1 -DSLIDE_STACK_WRITER=$(STACK_WRITER) -fPIC $(COMMON_CFLAGS) $(APP_SRCS) -shared -pthread -o "$@"

$(ROOT_HELPER): $(SRC_DIR)/su_daemon.c | $(BUILD_DIR)
	$(CC) -DKSU_LOADER_PATH='"/data/local/tmp/ksud-iqoo-z9-5g"' -DKSU_LOADER_ALLOW_SHELL=1 -DKSU_LOADER_FOREGROUND=1 -fPIE -pie -O2 -g0 -Wall -Wextra "$<" -ldl -o "$@"

verify: payload
	$(VERIFY_ARTIFACT)

info:
	@echo "ANDROID_NDK_HOME=$(ANDROID_NDK_HOME)"
	@echo "CC=$(CC)"
	@echo "TARGET=$(TARGET)"
	@echo "TARGET_DIR=$(TARGET_DIR)"
	@echo "TARGET_HEADER=$(TARGET_HEADER)"
	@echo "STACK_WRITER=$(STACK_WRITER)"
	@echo "ARTIFACTS_DIR=$(ARTIFACTS_DIR)"

clean:
	$(CLEAN_DIR)
