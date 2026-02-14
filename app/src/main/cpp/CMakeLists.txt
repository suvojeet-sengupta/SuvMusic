cmake_minimum_required(VERSION 3.22.1)

project("suvmusic_native")

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(suvmusic_native SHARED
        spatial_audio.cpp
        limiter.cpp
        file_mapper.cpp)

find_library(log-lib log)

target_link_libraries(suvmusic_native
        ${log-lib}
        android)

# Enable 16 KB page size support for Android 15+ and Android 16
target_link_options(suvmusic_native PRIVATE "-Wl,-z,max-page-size=16384")
