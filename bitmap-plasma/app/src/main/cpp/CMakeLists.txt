cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")

add_library(imageutils SHARED
            jniImageutils.c)

# Include libraries needed for imageutils lib
target_link_libraries(imageutils
                      android
                      jnigraphics
                      log
                      m)
