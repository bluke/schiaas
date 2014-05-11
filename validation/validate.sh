#!/bin/bash

SS_CLASSPATH="/usr/local/java/simgrid.jar:$PWD/../bin/schiaas.jar:$PWD/../bin/simschlouder/simschlouder.jar"

if [ $# -gt 0 ]
then 
	SOURCES="$*"
else
	SOURCES=sources/*
fi

mkdir data

for source in $SOURCES
do
	mkdir btuviewer/data
	rm -rf btuviewer/data/*

	mkdir concurrent-jobs/data
	rm concurrent-jobs/data/* 


	# finding the strategy
	if [ `cat $source | grep "Asap" -c` -ne 0 ] 
	then 
		STRATEGY="ASAP"
	else 
		STRATEGY="AFAP"
	fi
	echo "Found strategy: $STRATEGY"	

	sourceFilename=$(basename "$source")


	for lob in none wto psm rio
	do
		taskFile=${PWD}/data/${sourceFilename%.*}-${lob}.tasks
		jsonFile=${PWD}/data/${sourceFilename%.*}-${lob}.json

		./json2tasks.py $lob $source > $taskFile

		echo "Simulating $lob"
		cd simschlouder
		java -classpath $SS_CLASSPATH simschlouder.SimSchlouder simschlouder.xml $taskFile $STRATEGY 2> simschlouder.out
		mv simschlouder.json $jsonFile
		cd ..

		echo "Calculating diameters"
		./json2diameter.py $jsonFile concurrent-jobs/data/simschlouder-${lob}

		echo "Drawing Tickz"
		./json2tikz.py $jsonFile btuviewer/data/simschlouder-${lob}
	done

	echo "Plotting diameters"
	cd concurrent-jobs
	../json2diameter.py ../$source data/schlouder
	gnuplot template.plot
	epspdf diameter.eps ../btuviewer/data/diameter.pdf

	echo "Processing tex"
	cd ../btuviewer
	echo $source > data/source.filename
	../json2tikz.py ../$source data/schlouder

	rm *.pdf
	pdflatex template-btu-schlouder.tex
	cp template-btu-schlouder.pdf ../results/${sourceFilename}.pdf

	cd ..
done
