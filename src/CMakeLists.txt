file(GLOB_RECURSE schiaas_src  org/*.java)

file(GLOB_RECURSE cloudmasterslave_src cloudmasterslave/*.java)


add_jar(schiaas ${schiaas_src})

add_jar(cloudmasterslave ${schiaas_src} ${cloudmasterslave_src})


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