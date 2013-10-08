cmake_minimum_required (VERSION 2.6)
find_package(Java)
include(UseJava)

find_package(Doxygen)

project (SimSchlouder Java)

set(CMAKE_JAVA_TARGET_OUTPUT_DIR ${PROJECT_BINARY_DIR}/bin)

# Looking for simgrid.jar
find_file( SIMGRID_JAR "simgrid.jar" 
	"/usr/local/java/" 
	$ENV{SIMGRID_JAR_PATH})

message(STATUS ${SIMGRID_JAR} )

if(${SIMGRID_JAR} MATCHES "SIMGRID_JAR-NOTFOUND")
	message(FATAL_ERROR "simgrid.jar not found. Please set the env variable SIMGRID_JAR_PATH.")
endif(${SIMGRID_JAR} MATCHES "SIMGRID_JAR-NOTFOUND")

set(CMAKE_JAVA_INCLUDE_PATH ${SIMGRID_JAR})

set(CMAKE_JAVA_JAR_ENTRY_POINT simschlouder.SimSchlouder)


# add a target to generate API documentation with Doxygen
find_package(Doxygen)
if(DOXYGEN_FOUND)

configure_file(${PROJECT_SOURCE_DIR}/doc/Doxyfile.in ${PROJECT_SOURCE_DIR}/doc/Doxyfile @ONLY)

add_custom_target(doc ALL
${DOXYGEN_EXECUTABLE} ${PROJECT_SOURCE_DIR}/doc/Doxyfile
WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}/doc/
COMMENT "Generating API documentation with Doxygen" VERBATIM
)

endif(DOXYGEN_FOUND)


add_subdirectory(src)
