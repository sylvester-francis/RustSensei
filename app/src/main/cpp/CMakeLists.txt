cmake_minimum_required(VERSION 3.22.1)
project(rustsensei-llm)

# 16KB page size compatibility (required for Android 15+)
# Set GLOBALLY before any targets so it applies to all shared libs including llama.cpp
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

include(FetchContent)

# Disable OpenMP — libomp.so from NDK is not 16KB aligned
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

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

target_link_libraries(rustsensei-llm
    llama
    android
    log
)
