cmake_minimum_required(VERSION 3.22.1)
project(ghostlock_native LANGUAGES C)

add_library(ghostlock_native SHARED native_probe.c)
target_compile_options(ghostlock_native PRIVATE -O2 -Wall -Wextra -Werror)
target_link_libraries(ghostlock_native PRIVATE log)

# The Vivo V27 project keeps the exact helper source beside the payload source.
# There is no payload submodule or network dependency in this device-only app.
set(PAYLOAD_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../payload")
set(SU_DAEMON_SOURCE "${PAYLOAD_DIR}/src/su_daemon.c")
if(NOT EXISTS "${SU_DAEMON_SOURCE}")
    message(FATAL_ERROR "Missing bundled payload helper: ${SU_DAEMON_SOURCE}")
endif()

# Named libcve43499root.so, and an executable rather than a library.
#
# Both halves of that are load-bearing. It has to be an executable because
# InstallViewModel runs it with ProcessBuilder, which needs the PT_INTERP and
# entry point that -shared would not give it. It has to be called lib*.so
# because that is what the platform extracts out of an APK into
# nativeLibraryDir as a real file with the execute bit set -- which is also why
# jniLibs.useLegacyPackaging must stay true in build.gradle.kts.
add_executable(cve43499root
    "${SU_DAEMON_SOURCE}")
set_target_properties(cve43499root PROPERTIES PREFIX "lib" SUFFIX ".so")
target_include_directories(cve43499root PRIVATE "${PAYLOAD_DIR}/src")
target_compile_options(cve43499root PRIVATE -O2 -Wall -Wextra -Werror)
target_link_options(cve43499root PRIVATE -pie)
target_link_libraries(cve43499root PRIVATE dl)
target_compile_definitions(cve43499root PRIVATE
    KSU_LOADER_PATH=\"/data/local/tmp/ksud-vivo-v27-5g\"
    KSU_LOADER_ALLOW_SHELL=1
    KSU_LOADER_FOREGROUND=1)
