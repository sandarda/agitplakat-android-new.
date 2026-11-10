cmake_minimum_required(VERSION 3.22.1)
project(agitplakat_native LANGUAGES CXX)

add_library(agitplakat_native SHARED native-lib.cpp)
find_library(log-lib log)

set(MNN_ROOT "" CACHE PATH "Path to a matching Alibaba MNN source checkout")
if(MNN_ROOT AND EXISTS "${MNN_ROOT}/transformers/diffusion/engine/include/diffusion/diffusion.hpp"
             AND EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/arm64-v8a/libMNN.so")
    add_library(MNN SHARED IMPORTED)
    set_target_properties(MNN PROPERTIES
        IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/arm64-v8a/libMNN.so")
    target_include_directories(agitplakat_native PRIVATE
        "${MNN_ROOT}/include"
        "${MNN_ROOT}/transformers/diffusion/engine/include")
    target_compile_definitions(agitplakat_native PRIVATE AGITPLAKAT_WITH_MNN=1)
    target_link_libraries(agitplakat_native PRIVATE MNN android)
    message(STATUS "AgitPlakat: MNN Diffusion JNI enabled")
else()
    message(WARNING "AgitPlakat: MNN_ROOT/libMNN.so missing; building diagnostic-only JNI")
endif()

target_link_libraries(agitplakat_native PRIVATE ${log-lib})
target_compile_features(agitplakat_native PRIVATE cxx_std_17)
