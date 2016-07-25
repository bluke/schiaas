#!/bin/sh

if [ $# -lt 1 ] ; then
	echo "Do the post-processing of the validation."
	echo "Usage : $0  simschlouder_json_file [simschlouder_json_file...]"
	exit 1
fi

#cd `dirname \`readlink -f $0\``

if [ ! $ST_DIR ] ; then 
	ST_DIR="../../../../schlouder-traces"
fi

if [ ! -f $ST_DIR/trace-utils.py ] ; then
	echo "trace-utils.py was not found at $ST_DIR."
	echo "Make sure to export ST_DIR='path/to/schlouder-traces'"
	exit 2
fi

SIM_DIR="../../simulations"
TRACES_DIR="$ST_DIR/traces"

sim_json=$1
tasks_file=`basename \`cat $sim_json | grep tasks_file | cut -d'"' -f4\``
trace_json="$TRACES_DIR/${tasks_file%%tasks}json"
#echo "${ST_DIR}/trace-utils.py -M $trace_json -d $sim_json"
${ST_DIR}/schlouder-traces-utils.py -MH $trace_json -d $sim_json

for sim_json in $*
do
	tasks_file=`basename \`cat $sim_json | grep tasks_file | cut -d'"' -f4\``
	trace_json="$TRACES_DIR/${tasks_file%%tasks}json"
	#echo "${ST_DIR}/trace-utils.py -M $trace_json -d $sim_json"
	${ST_DIR}/schlouder-traces-utils.py -M $trace_json -d $sim_json
	if [ $? -ne 0 ]; then
		echo "Error during the traces comparison"
		exit 3
	fi
done

