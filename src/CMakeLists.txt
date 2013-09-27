file(GLOB_RECURSE simiaas_src  org/*.java)

file(GLOB_RECURSE simschlouder_src  simschlouder/*.java)

file(GLOB_RECURSE cloudmasterslave_src cloudmasterslave/*.java)



add_jar(simiaas ${simiaas_src})

add_jar(simschlouder ${simiaas_src} ${simschlouder_src})

add_jar(cloudmasterslave ${simiaas_src} ${cloudmasterslave_src})


set (CMAKE_CURRENT_BINARY_DIR ${PROJECT_BINARY_DIR}/doc/html)


create_javadoc(simiaas
	FILES ${simiaas_src}
	CLASSPATH ${CMAKE_JAVA_INCLUDE_PATH}
	INSTALLPATH "../doc/html/"
	WINDOWTITLE "SimIaaS"
	DOCTITLE "<h1>SimIaaS</h1>"
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
