
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

# AllMasterSlave
file(GLOB_RECURSE allmasterslave_src ${PROJECT_EXAMPLES_DIR}/allmasterslave/*.java)
set(CMAKE_JAVA_JAR_ENTRY_POINT allmasterslave.Masterslave)
add_jar(allmasterslave ${allmasterslave_src} OUTPUT_DIR ${PROJECT_BINARY_DIR}/allmasterslave)

add_custom_command( TARGET allmasterslave POST_BUILD COMMAND cp -r ${PROJECT_EXAMPLES_DIR}/allmasterslave/companionFiles/* ${PROJECT_BINARY_DIR}/allmasterslave)

#LiveSim
file(GLOB_RECURSE livesim_src ${PROJECT_EXAMPLES_DIR}/livesim/*.java)
set(CMAKE_JAVA_JAR_ENTRY_POINT livesim.LiveSim)
add_jar(livesim ${livesim_src} OUTPUT_DIR ${PROJECT_BINARY_DIR}/livesim)

add_custom_command( TARGET livesim POST_BUILD COMMAND cp -r ${PROJECT_EXAMPLES_DIR}/livesim/companionFiles/* ${PROJECT_BINARY_DIR}/livesim)

