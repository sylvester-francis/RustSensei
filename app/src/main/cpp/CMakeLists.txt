cmake_minimum_required(VERSION 3.22.1)
project(rustsensei-llm)

# 16KB page size compatibility (required for Android 15+)
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

include(FetchContent)

# Disable OpenMP — libomp.so from NDK is not 16KB aligned
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

# Enable ARM-specific optimizations in ggml
set(GGML_CPU ON CACHE BOOL "" FORCE)

# PERF CRITICAL: Tell ggml to use ARMv8.2-A features (dotprod, fp16)
# This enables SDOT instructions for 4x faster Q4_K_M matmul
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    set(GGML_CPU_ARM_ARCH "armv8.2-a+dotprod+fp16" CACHE STRING "" FORCE)
endif()

# Fetch llama.cpp source
FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
    GIT_TAG b5097
)

FetchContent_MakeAvailable(llama_cpp)

# Build the JNI bridge
add_library(rustsensei-llm SHARED
    llama-android.cpp
)

target_include_directories(rustsensei-llm PRIVATE
    ${llama_cpp_SOURCE_DIR}/include
)

# Apply optimization flags only to our target (not globally)
target_compile_options(rustsensei-llm PRIVATE -O3)

target_link_libraries(rustsensei-llm
    llama
    android
    log
)
