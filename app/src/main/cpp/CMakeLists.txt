cmake_minimum_required(VERSION 3.22.1)

project("suvmusic_native")

add_library(suvmusic_native SHARED
        spatial_audio.cpp
        limiter.cpp)

find_library(log-lib log)

target_link_libraries(suvmusic_native
        ${log-lib}
        android)
