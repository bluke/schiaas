# SCHIaaS
file(GLOB_RECURSE schiaas_src  org/*.java)

message(STATUS ${PROJECT_BINARY_DIR})

add_jar(schiaas ${schiaas_src} OUTPUT_DIR ${PROJECT_BINARY_DIR}/)

add_custom_command( TARGET schiaas POST_BUILD COMMAND jar ARGS uf ${PROJECT_BINARY_DIR}/schiaas.jar -C ${PROJECT_SOURCE_DIR} org/simgrid/schiaas/cloud.xsd)


# SimSchlouder
file(GLOB_RECURSE simschlouder_src  simschlouder/*.java)
set(CMAKE_JAVA_JAR_ENTRY_POINT simschlouder.SimSchlouder)
add_jar(simschlouder ${simschlouder_src} OUTPUT_DIR ${PROJECT_BINARY_DIR}/simschlouder)

add_custom_command( TARGET simschlouder POST_BUILD COMMAND cp ${PROJECT_SOURCE_DIR}/simschlouder/companionFiles/* ${PROJECT_BINARY_DIR}/simschlouder)


# CloudMasterSlave
file(GLOB_RECURSE cloudmasterslave_src cloudmasterslave/*.java)
set(CMAKE_JAVA_JAR_ENTRY_POINT cloudmasterslave.Masterslave)
add_jar(cloudmasterslave ${cloudmasterslave_src} OUTPUT_DIR ${PROJECT_BINARY_DIR}/cloudmasterslave)

add_custom_command( TARGET cloudmasterslave POST_BUILD COMMAND cp ${PROJECT_SOURCE_DIR}/cloudmasterslave/companionFiles/* ${PROJECT_BINARY_DIR}/cloudmasterslave)


# AllMasterSlave

file(GLOB_RECURSE allmasterslave_src allmasterslave/*.java)
set(CMAKE_JAVA_JAR_ENTRY_POINT allmasterslave.Masterslave)
add_jar(allmasterslave ${allmasterslave_src} OUTPUT_DIR ${PROJECT_BINARY_DIR}/allmasterslave)

add_custom_command( TARGET allmasterslave POST_BUILD COMMAND cp ${PROJECT_SOURCE_DIR}/allmasterslave/companionFiles/* ${PROJECT_BINARY_DIR}/allmasterslave)


set (CMAKE_CURRENT_BINARY_DIR ${PROJECT_BINARY_DIR}/doc/html)
