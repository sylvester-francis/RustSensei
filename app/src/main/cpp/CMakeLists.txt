cmake_minimum_required(VERSION 3.22.1)
project(rustsensei-llm)

# 16KB page size compatibility (required for Android 15+)
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# PERF CRITICAL: ARM optimization flags for modern SoCs
# armv8.2-a enables dotprod (4x faster quantized matmul) and fp16 (2x faster KV cache)
# All arm64-v8a devices since ~2018 support this (Snapdragon 845+, Tensor G1+, Exynos 9810+)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -march=armv8.2-a+dotprod+fp16")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -march=armv8.2-a+dotprod+fp16")

include(FetchContent)

# Disable OpenMP — libomp.so from NDK is not 16KB aligned
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

# Enable ARM-specific optimizations in ggml
set(GGML_CPU ON CACHE BOOL "" FORCE)

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

# Apply same optimization flags to the JNI bridge
target_compile_options(rustsensei-llm PRIVATE -O3)

target_link_libraries(rustsensei-llm
    llama
    android
    log
)
