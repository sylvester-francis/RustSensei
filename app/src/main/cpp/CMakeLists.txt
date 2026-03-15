cmake_minimum_required(VERSION 3.22.1)
project(rustsensei-llm)

include(FetchContent)

# Fetch llama.cpp source
FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
    GIT_TAG b4568
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

# 16KB page size compatibility (required for Android 15+)
target_link_options(rustsensei-llm PRIVATE "-Wl,-z,max-page-size=16384")

# Also apply to llama.cpp shared libs
if(TARGET llama)
    target_link_options(llama PRIVATE "-Wl,-z,max-page-size=16384")
endif()
if(TARGET ggml)
    target_link_options(ggml PRIVATE "-Wl,-z,max-page-size=16384")
endif()
if(TARGET ggml-base)
    target_link_options(ggml-base PRIVATE "-Wl,-z,max-page-size=16384")
endif()
if(TARGET ggml-cpu)
    target_link_options(ggml-cpu PRIVATE "-Wl,-z,max-page-size=16384")
endif()
