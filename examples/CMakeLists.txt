
set(CMAKE_JAVA_INCLUDE_PATH ${SIMGRID_JAR}:${PROJECT_BINARY_DIR}/schiaas.jar)

# SimSchlouder
file(GLOB_RECURSE simschlouder_src  ${PROJECT_EXAMPLES_DIR}/simschlouder/*.java)
set(CMAKE_JAVA_JAR_ENTRY_POINT simschlouder.SimSchlouder)
add_jar(simschlouder ${simschlouder_src} OUTPUT_DIR ${PROJECT_BINARY_DIR}/simschlouder)
add_custom_command( TARGET simschlouder POST_BUILD COMMAND jar ARGS uf ${PROJECT_BINARY_DIR}/simschlouder/simschlouder.jar -C ${PROJECT_EXAMPLES_DIR} /simschlouder/simschlouder.xsd)
add_custom_command( TARGET simschlouder POST_BUILD COMMAND cp -r ${PROJECT_EXAMPLES_DIR}/simschlouder/companionFiles/* ${PROJECT_BINARY_DIR}/simschlouder)

# CloudMasterSlave
file(GLOB_RECURSE cloudmasterslave_src ${PROJECT_EXAMPLES_DIR}/cloudmasterslave/*.java)
set(CMAKE_JAVA_JAR_ENTRY_POINT cloudmasterslave.Masterslave)
add_jar(cloudmasterslave ${cloudmasterslave_src} OUTPUT_DIR ${PROJECT_BINARY_DIR}/cloudmasterslave)

add_custom_command( TARGET cloudmasterslave POST_BUILD COMMAND cp -r ${PROJECT_EXAMPLES_DIR}/cloudmasterslave/companionFiles/* ${PROJECT_BINARY_DIR}/cloudmasterslave)

#loadinjector
file(GLOB_RECURSE loadinjector_src ${PROJECT_EXAMPLES_DIR}/loadinjector/*.java)
set(CMAKE_JAVA_JAR_ENTRY_POINT loadinjector.SimpleInjection)
add_jar(loadinjector ${loadinjector_src} OUTPUT_DIR ${PROJECT_BINARY_DIR}/loadinjector)

add_custom_command( TARGET loadinjector POST_BUILD COMMAND cp -r ${PROJECT_EXAMPLES_DIR}/loadinjector/companionFiles/* ${PROJECT_BINARY_DIR}/loadinjector)

