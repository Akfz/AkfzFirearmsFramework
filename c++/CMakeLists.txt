cmake_minimum_required(VERSION 3.15)
project(aff_physics VERSION 1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
        add_compile_options(-O3 -ffast-math -march=native)
    elseif(CMAKE_CXX_COMPILER_ID MATCHES "MSVC")
        add_compile_options(/O2 /fp:fast /arch:AVX2)
    endif()
endif()

find_package(JNI REQUIRED)

add_library(aff_physics SHARED
    src/NativeBallistics.cpp
    src/aff_types.h
)

target_include_directories(aff_physics PRIVATE
    ${JNI_INCLUDE_DIRS}
    src/headers
)

if(WIN32)
    set_target_properties(aff_physics PROPERTIES PREFIX "")
endif()

set(JAVA_NATIVE_DIR "${CMAKE_SOURCE_DIR}/../common/src/main/resources/native")

add_custom_command(TARGET aff_physics POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E make_directory "${JAVA_NATIVE_DIR}"
    COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:aff_physics> "${JAVA_NATIVE_DIR}/"
    COMMENT "Copying native library to Java resources: ${JAVA_NATIVE_DIR}"
)
