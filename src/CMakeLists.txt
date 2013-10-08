file(GLOB_RECURSE schiaas_src  org/*.java)

file(GLOB_RECURSE simschlouder_src  simschlouder/*.java)

file(GLOB_RECURSE cloudmasterslave_src cloudmasterslave/*.java)

file(GLOB_RECURSE allmasterslave_src allmasterslave/*.java)


add_jar(schiaas ${schiaas_src})

add_jar(simschlouder ${schiaas_src} ${simschlouder_src})

add_jar(cloudmasterslave ${schiaas_src} ${cloudmasterslave_src})

add_jar(allmasterslave ${schiaas_src} ${allmasterslave_src})


set (CMAKE_CURRENT_BINARY_DIR ${PROJECT_BINARY_DIR}/doc/html)


create_javadoc(schiaas
	FILES ${schiaas_src}
	CLASSPATH ${CMAKE_JAVA_INCLUDE_PATH}
	INSTALLPATH "../doc/html/"
	WINDOWTITLE "SCHIaaS"
	DOCTITLE "<h1>SCHIaaS</h1>"
	AUTHOR TRUE	
	USE TRUE
	VERSION TRUE
)

create_javadoc(simschlouder
	FILES ${simschlouder_src}
	CLASSPATH ${CMAKE_JAVA_INCLUDE_PATH}
	INSTALLPATH "../doc/html/"
	WINDOWTITLE "SimSchlouder"
	DOCTITLE "<h1>SimSchlouder</h1>"
	AUTHOR TRUE
	USE TRUE
	VERSION TRUE
)
