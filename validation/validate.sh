#!/bin/bash

TASK_FILE=${PWD}/simschlouder/trace.tasks

if [ $# -gt 0 ]
then 
	SOURCES="$*"
else
	SOURCES=sources/*
fi

for source in $SOURCES
do
	mkdir -p btuviewer/data
	rm btuviewer/data/*

	echo "processing $source"
	./schlouder2simtasks.py $source $TASK_FILE 
	
	if [ `echo $source | grep "Asap" -c` -eq 1 ] 
	then 
		STRATEGY="ASAP"
	else 
		STRATEGY="AFAP"
	fi
	

	echo "simschlouder"	
	cd simschlouder 
	rm *.json
	java -classpath "/usr/local/java/simgrid.jar:../../bin/simschlouder.jar" simschlouder.SimSchlouder simschlouder.xml $TASK_FILE $STRATEGY
	cp simschlouder.json simschlouder-realduration.json

	cp $TASK_FILE /tmp/`basename $TASK_FILE`; cat /tmp/`basename $TASK_FILE` | cut -f1,2,5 -d" " > $TASK_FILE
        java -classpath "/usr/local/java/simgrid.jar:../../bin/simschlouder.jar" simschlouder.SimSchlouder simschlouder.xml $TASK_FILE $STRATEGY
	mv simschlouder.json simschlouder-predictiononly.json
	
	echo "gluplot diamters"
	cd ../concurrent-jobs
	rm *.dat *.eps
	../json2diameter.py ../$source schlouder
	../json2diameter.py ../simschlouder/simschlouder-realduration.json simschlouder-realduration
	../json2diameter.py ../simschlouder/simschlouder-predictiononly.json simschlouder-predictiononly

	gnuplot template.plot
	epspdf diameter.eps ../btuviewer/data/diameter.pdf

	echo "latex btu"
	cd ../btuviewer
	echo $source > data/source.filename
	../schlouder-btu.py ../$source data/schlouder
	../schlouder-btu.py ../simschlouder/simschlouder-realduration.json data/simschlouder-realduration
	../schlouder-btu.py ../simschlouder/simschlouder-predictiononly.json data/simschlouder-predictiononly

	rm *.pdf
	pdflatex template-btu-schlouder.tex 
	cp template-btu-schlouder.pdf ../results/`basename $source`.pdf

	cd ..
done
