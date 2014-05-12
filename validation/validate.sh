#!/bin/bash

SS_CLASSPATH="/usr/local/java/simgrid.jar:$PWD/../bin/schiaas.jar:$PWD/../bin/simschlouder/simschlouder.jar"
STAT_FILE="results/stats.dat"

if [ $# -gt 0 ]
then 
	SOURCES="$*"
else
	SOURCES=sources/*
fi

mkdir data
mkdir btuviewer/data
mkdir concurrent-jobs/data

if [ ! -e "${STAT_FILE}" ] 
then
	echo "Creating stat file $STAT_FILE"
	echo -e "\"source\"\t\"VM\"\t\"BTU\"\t\"makespan\"\t\"wpo VM\"\t\"wpo BTU\"\t\"wpo makespan\"\t\"wto VM\"\t\"wto BTU\"\t\"wto makespan\"\t\"psm VM\"\t\"psm BTU\"\t\"psm makespan\"\t\"rio VM\"\t\"rio BTU\"\t\"rio makespan\"" > $STAT_FILE
fi


for source in $SOURCES
do
	echo "Processing $source"

	source=`readlink -f $source`
	sourceFilename=$(basename "$source")

	echo -en "$sourceFilename" >> $STAT_FILE

	rm concurrent-jobs/data/* 
	rm -rf btuviewer/data/*


	# finding the strategy
	if [ `cat $source | grep "Asap" -c` -ne 0 ] 
	then 
		STRATEGY="ASAP"
	else 
		STRATEGY="AFAP"
	fi
	echo "Found strategy: $STRATEGY"	

	# finding the cloud
	if [[ $source == *inria* ]] 
	then 
		CLOUD_FILE="simschlouderBonFIRE-fr-inria.xml"
	elif [[ $source == *epcc* ]] 
	then
		CLOUD_FILE="simschlouderBonFIRE-uk-epcc.xml"
	elif [[ $source == *hlrs* ]] 
	then
		CLOUD_FILE="simschlouderBonFIRE-de-hlrs.xml"
	else
		CLOUD_FILE="simschlouderICPS.xml"
	fi
	echo "Using: $CLOUD_FILE"


	echo "Processing source"
	./json2diameter.py $source concurrent-jobs/data/schlouder
	stat=`./json2tikz.py $source btuviewer/data/schlouder`

	echo -en "\t$stat" >> $STAT_FILE

	for lob in wpo wto psm rio
	do
		taskFile=${PWD}/data/${sourceFilename%.*}-${lob}.tasks
		jsonFile=${PWD}/data/${sourceFilename%.*}-${lob}.json

		./json2tasks.py $lob $source > $taskFile
		if [ "$?" -ne 0 ] ; then
			echo "Fail"
			echo -en "\tNA\tNA\tNA" >> $STAT_FILE
			continue
		fi


		echo "Simulating $lob"
		cd simschlouder
		java -classpath $SS_CLASSPATH simschlouder.SimSchlouder $CLOUD_FILE $taskFile $STRATEGY 2> simschlouder.out
		mv simschlouder.json $jsonFile
		cd ..

		echo "Calculating diameters"
		./json2diameter.py $jsonFile concurrent-jobs/data/simschlouder-${lob}

		echo "Drawing Tickz"
		stat=`./json2tikz.py $jsonFile btuviewer/data/simschlouder-${lob}`
		echo -en "\t$stat" >> $STAT_FILE
	done

	echo >> $STAT_FILE

	echo "Plotting diameters"
	cd concurrent-jobs
	gnuplot template.plot
	epspdf diameter.eps ../btuviewer/data/diameter.pdf

	echo "Processing tex"
	cd ../btuviewer
	echo $source > data/source.filename

	rm *.pdf
	pdflatex template-btu-schlouder.tex
	cp template-btu-schlouder.pdf ../results/${sourceFilename}.pdf

	cd ..
done
