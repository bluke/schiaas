#!/bin/bash

SS_CLASSPATH="/usr/local/java/simgrid.jar:$PWD/../bin/schiaas.jar:$PWD/../bin/simschlouder/simschlouder.jar"
STAT_FILE="results/stats.dat"


FAST=false
LODS="wpo wto psm rio"
BTPLN=false

while getopts "fp" option
do
	case $option in
	f)
		echo "- Fast execution (stats only)"
		FAST=true
		LODS="wpo wto"
		shift
		(( OPTIND -- ))
		;;
	p)
		echo "- Plots only (no stats)"
		STAT_FILE=/dev/null
		shift
		(( OPTIND -- ))
		;;
	esac
done

if [ "$1" == "--fast" ]
then 
	echo "Fast validation (stats only)"
	FAST=true
	LODS="wpo wto"
	shift
else
	echo "Full validation"
fi


if [ $# -gt 0 ]
then 
	SOURCES="$*"
	NSOURCES="$#"
else
	SOURCES=sources/*
	NSOURCES=`ls -l sources/* | wc -l`
fi

mkdir -p results
mkdir -p data
mkdir -p btuviewer/data
mkdir -p concurrent-jobs/data

if [ ! -e "${STAT_FILE}" ] 
then
	echo "Creating stat file $STAT_FILE"
	echo -en "\"source\"\t\"VM\"\t\"BTU\"\t\"makespan\"\t\"wpo VM\"\t\"wpo BTU\"\t\"wpo makespan\"\t\"wto VM\"\t\"wto BTU\"\t\"wto makespan\"" > $STAT_FILE
	if $FAST ; then
		echo -en "\t\"psm VM\"\t\"psm BTU\"\t\"psm makespan\"\t\"rio VM\"\t\"rio BTU\"\t\"rio makespan\"" >> $STAT_FILE
	fi
	echo >> $STAT_FILE
fi

iSources=1
for source in $SOURCES
do
	echo "[${iSources}/${NSOURCES}] Processing $source"
	iSources=$(( iSources + 1 ))

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

	# finding the boot_time_predictions
	BTPfound=0
	for BTP in 41 48 53 86 139 198 212 220 242
	do  
		if [ `grep -c "\"boot_time_prediction\": ${BTP}," $source` -ne 0 ]
		then
			BTPfound=1
			break
		fi
	done
	if [ $BTPfound -eq 0 ]
	then
		echo "Boot Time Prediction not found"
		exit -1
	fi

	# finding the cloud
	if [[ $source == *inria* ]] 
	then 
		CLOUD_FILE="simschlouderBonFIRE-fr-inria-$BTP.xml"
	elif [[ $source == *epcc* ]] 
	then
		CLOUD_FILE="simschlouderBonFIRE-uk-epcc-$BTP.xml"
	elif [[ $source == *hlrs* ]] 
	then
		CLOUD_FILE="simschlouderBonFIRE-de-hlrs-$BTP.xml"
	elif [[ $source == *openstack* ]] 
	then
		CLOUD_FILE="simschlouderICPS-$BTP.xml"
	else
		echo "Cloud not found"
		exit -1
	fi


	echo "Using: $CLOUD_FILE"
	CLOUD_FILE=`realpath simschlouder/$CLOUD_FILE`

	echo "Processing source"
	./json2diameter.py $source concurrent-jobs/data/schlouder
	stat=`./json2tikz.py $source btuviewer/data/schlouder`

	# Fix the XP with 22 vms
	if [ "`grep host -c $source`" -eq "22" ]
	then
		cat $CLOUD_FILE | sed 's/max_instances_per_user="20"/max_instances_per_user="22"/' > /tmp/simschlouder.xml
		CLOUD_FILE="/tmp/simschlouder.xml"	
	fi


	echo -en "\t$stat" >> $STAT_FILE

	pids=""
	for lod in $LODS
	do
		rm -f /tmp/$lod
		./validateLod.sh $source $lod $CLOUD_FILE $STRATEGY > /tmp/$lod &
		pids="$pids $!"
	done

	wait $pids

	for lod in $LODS
	do
		echo -en "`cat /tmp/$lod | tr " " "\t"`" >> $STAT_FILE
	done

	echo >> $STAT_FILE

	if $FAST ; then
		continue
	fi

	echo "Plotting diameters"
	cd concurrent-jobs
	gnuplot template.plot
	epspdf diameter.eps ../btuviewer/data/diameter.pdf

	echo "Processing tex"
	cd ../btuviewer
	echo $source > data/source.filename

	rm -f *.pdf
	pdflatex template-btu-schlouder.tex > pdflatex.out
	cp template-btu-schlouder.pdf ../results/${sourceFilename}.pdf

	cd ..
done

if [ "$STAT_FILE" != "/dev/null" ]
then
	echo "Computing stats"
	cat $STAT_FILE | cut -f1-10 > stats/stats.dat
	cd stats
	R --no-save < stats.R > stats.out
	R --no-save < statsPSM.R > /dev/null
	cd ..
fi
