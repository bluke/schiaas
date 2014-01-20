file(GLOB_RECURSE schiaas_src  org/*.java)

file(GLOB_RECURSE simschlouder_src  simschlouder/*.java)

file(GLOB_RECURSE cloudmasterslave_src cloudmasterslave/*.java)

file(GLOB_RECURSE allmasterslave_src allmasterslave/*.java)


add_jar(schiaas ${schiaas_src})

add_custom_command( TARGET schiaas POST_BUILD COMMAND jar ARGS uf ${PROJECT_BINARY_DIR}/bin/schiaas.jar -C ${PROJECT_BINARY_DIR}/src org/simgrid/schiaas/cloud.xsd)

add_jar(simschlouder ${schiaas_src} ${simschlouder_src})

add_jar(cloudmasterslave ${schiaas_src} ${cloudmasterslave_src})

add_jar(allmasterslave ${schiaas_src} ${allmasterslave_src})

set (CMAKE_CURRENT_BINARY_DIR ${PROJECT_BINARY_DIR}/doc/html)
