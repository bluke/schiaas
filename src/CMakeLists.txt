# SCHIaaS
file(GLOB_RECURSE schiaas_src  org/*.java)

set(CMAKE_JAVA_INCLUDE_PATH ${SIMGRID_JAR})

add_jar(schiaas ${schiaas_src} OUTPUT_DIR ${PROJECT_BINARY_DIR}/)

add_custom_command( TARGET schiaas POST_BUILD COMMAND jar ARGS uf ${PROJECT_BINARY_DIR}/schiaas.jar -C ${PROJECT_SOURCE_DIR} org/simgrid/schiaas/cloud.xsd)
