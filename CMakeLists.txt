cmake_minimum_required (VERSION 2.6)
find_package(Java)
include(UseJava)

find_package(Doxygen)

project (SCHIaaS Java)

# Looking for simgrid.jar
find_file( SIMGRID_JAR "simgrid.jar" 
	"/usr/local/java/" 
	$ENV{SIMGRID_JAR_PATH})

#message(STATUS ${SIMGRID_JAR} )

if(${SIMGRID_JAR} MATCHES "SIMGRID_JAR-NOTFOUND")
	message(FATAL_ERROR "simgrid.jar not found. Please set the env variable SIMGRID_JAR_PATH.")
endif(${SIMGRID_JAR} MATCHES "SIMGRID_JAR-NOTFOUND")

set(PROJECT_ROOT_DIR ${PROJECT_BINARY_DIR})
set(PROJECT_SOURCE_DIR ${PROJECT_ROOT_DIR}/src)
set(PROJECT_EXAMPLES_DIR ${PROJECT_ROOT_DIR}/examples)
set(PROJECT_BINARY_DIR ${PROJECT_ROOT_DIR}/bin)
set(PROJECT_DOC_DIR ${PROJECT_ROOT_DIR}/doc)


# add a target to generate API documentation with Doxygen
set(MAKE_DOXYGEN comment("Doxygen not found"))
find_package(Doxygen)
if(DOXYGEN_FOUND)

configure_file(${PROJECT_DOC_DIR}/Doxyfile.in ${PROJECT_DOC_DIR}/Doxyfile @ONLY)

set(MAKE_DOXYGEN
${DOXYGEN_EXECUTABLE} ${PROJECT_DOC_DIR}/Doxyfile
WORKING_DIRECTORY ${PROJECT_DOC_DIR}
COMMENT "Generating API documentation with Doxygen" VERBATIM
)
endif(DOXYGEN_FOUND)

set(MSG_JAVADOC_URL "http://simgrid.gforge.inria.fr/simgrid/3.11/doc/javadoc/index.html")

set(DOC_INSTALL_PATH "${PROJECT_ROOT_DIR}/doc/html/javadoc")

add_custom_target(documentation
COMMAND ${MAKE_DOXYGEN}
COMMAND ${Java_JAVADOC_EXECUTABLE} 
-sourcepath ${PROJECT_SOURCE_DIR}
-d ${DOC_INSTALL_PATH}/schiaas
-subpackages org
-link http://simgrid.gforge.inria.fr/simgrid/3.11/doc/javadoc/
-doctitle "<h1>SchIaaS</h1>"
-windowtitle "SchIaaS"
-use
-quiet
COMMAND ${Java_JAVADOC_EXECUTABLE} 
-sourcepath ${PROJECT_EXAMPLES_DIR}
-d ${DOC_INSTALL_PATH}/simschlouder
-subpackages simschlouder
-link ../schiaas 
-doctitle "<h1>SimSchlouder</h1>"
-windowtitle "SimSchlouder"
-use
-quiet
COMMENT "${MSG_JAVADOC} Generating API documentation" VERBATIM
)

add_subdirectory(src)
add_subdirectory(examples)

#testing
find_program( TESH_BIN 
	tesh)

if(NOT ${TESH_BIN} MATCHES TESH_BIN-NOTFOUND)
	enable_testing()
	add_test(tesh-cloudmasterslave ${TESH_BIN} --cd ${PROJECT_BINARY_DIR}/cloudmasterslave ${PROJECT_BINARY_DIR}/cloudmasterslave/test.tesh)
	add_test(tesh-simschlouder-ASAP ${TESH_BIN} --cd ${PROJECT_BINARY_DIR}/simschlouder ${PROJECT_BINARY_DIR}/simschlouder/ASAP.tesh)
	add_test(tesh-simschlouder-AFAP ${TESH_BIN} --cd ${PROJECT_BINARY_DIR}/simschlouder ${PROJECT_BINARY_DIR}/simschlouder/AFAP.tesh)
endif(NOT ${TESH_BIN} MATCHES TESH_BIN-NOTFOUND)
