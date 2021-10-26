#!/bin/bash


if [ ! $ST_DIR ] ; then 
	ST_DIR="../../../../schlouder-traces"
fi

if [ ! -f $ST_DIR/schlouder-traces-utils.py ] ; then
	echo "schlouder-traces-utils was not found at $ST_DIR."
	echo "Make sure to export ST_DIR='path/to/schlouder-traces'"
	exit 2
fi


SIM_DIR="../../simulations"
TRACES_DIR="$ST_DIR/traces"

if [ $# -le 0 ] ; then
	sim_jsons="$SIM_DIR/*/simschlouder.json"
else
	sim_jsons="$*"
fi

header="-H"

for id in `echo $sim_jsons | tr " " "\n" | cut -d'_' -f2 | cut -d'.' -f1-7 | uniq`
do
	# Real xp
	for trace_json in $TRACES_DIR/${id}*
	do
		#echo $trace_json
		${ST_DIR}/schlouder-traces-utils.py $header $trace_json -v metrics
		if [ $? -ne 0 ]; then
			>&2 echo "Error during the execution of:"
			>&2 echo "${ST_DIR}/schlouder-traces-utils.py $header $trace_json -v metrics"
			exit 3
		fi

		header=
	done

	# Simulations
	for sim_json in $SIM_DIR/*$id*/simschlouder.json
	do
		#echo $trace_json
		${ST_DIR}/schlouder-traces-utils.py $header $sim_json -v metrics
		if [ $? -ne 0 ]; then
			>&2 echo "Error during the execution of:"
			>&2 echo "${ST_DIR}/schlouder-traces-utils.py $header $sim_json -v metrics"
			exit 3
		fi
	done

done