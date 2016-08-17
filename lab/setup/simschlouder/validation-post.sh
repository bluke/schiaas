#!/bin/bash

TYPE="metrics"
PATTERN=""

while getopts "njkh" option
do
	case $option in
	n)
		TYPE="nodes"
		shift	
		(( OPTIND -- ))
		;;
	j)
		TYPE="jobs"
		shift	
		(( OPTIND -- ))
		;;
	k)
		shift
		PATTERN="$1"
		shift
		(( OPTIND -- ))
		;;
	h)
		echo "Do the post-processing of the validation. By defaut, print xp metrics."
		echo "Usage : $0 [-nj] [-k pattern] [simschlouder_json_file...]"
		echo "-n print jobs values"
		echo "-j print jobs metrics"
		echo "-k pattern print only values wich key matches the pattern"
		exit 1
		;;
	esac
done

#cd `dirname \`readlink -f $0\``

if [ ! $ST_DIR ] ; then 
	ST_DIR="../../../../schlouder-traces"
fi

if [ ! -f $ST_DIR/schlouder-traces-utils.py ] ; then
	echo "schlouder-traces-utilswas not found at $ST_DIR."
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
for sim_json in $sim_jsons
do
	tasks_file=`basename \`cat $sim_json | grep tasks_file | cut -d'"' -f4\``
	trace_json="$TRACES_DIR/${tasks_file%%tasks}json"
	#echo "${ST_DIR}/trace-utils.py -M $trace_json -d $sim_json"

	if [ "$trace_json" != "$old_trace_json" ]; then
		${ST_DIR}/schlouder-traces-utils.py $header $trace_json -v $TYPE "$PATTERN" 
		if [ $? -ne 0 ]; then
			>&2 echo "Error during the execution of:"
			>&2 echo "${ST_DIR}/schlouder-traces-utils.py $header $trace_json -v $TYPE \"$PATTERN\""
			exit 3
		fi
		header=""
		old_trace_json=$trace_json
	fi

	${ST_DIR}/schlouder-traces-utils.py $header $sim_json -v $TYPE "$PATTERN" 
	if [ $? -ne 0 ]; then
		>&2 echo "Error during the execution of:"
		>&2 echo "${ST_DIR}/schlouder-traces-utils.py $header $sim_json -v $TYPE \"$PATTERN\""
		exit 3
	fi
done

