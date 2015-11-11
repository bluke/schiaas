## This script executes simulations according to one config file.
## It takes the configuration file as argument.
## It needs the java classpath to be set.
##
## The configuration file format is as follows.
## Lines stating by:
## #SETUP_DIR indicates the directory containing setup files (should be called first)
## #R_SCRIPT indicates the R script to call at he end of the simulations
## #TU_ARGS indicates the arguments to pass to the trace_util.py script
## #JAVA_FIRST_ARGS indicates the common first arguments to pass to the simulator
## #JAVA_END_ARGS indicates the common last arguments to pass to the simulator
## Other lines indicates the specific arguments inbetween for each simulation 
## as <simulation_id>: <simulation arguments>
##
## Two options are available:
## -k keeps the data from previous executions
## -p x run x simulation in parallel
##
## @author julien.gossa@unistra.fr


PARALLEL_SIMS=1

while getopts "kp" option
do
	case $option in
	k)
		KEEP=true
		shift
		(( OPTIND -- ))
		echo "- Keep previous produced data"
		;;
	p)
		shift
		PARALLEL_SIMS=$1
		JAVA_PS_COUNT=`ps --no-header -C java | wc -l`
		shift
		(( OPTIND -- ))
		echo "- Parallel execution: $PARALLEL_SIMS"
		;;
	esac
done

if [ $# -ne 1 ] ; then
	echo "Takes one argument. Needs to run in the lab directory"
	echo "Usage : $0 [-k] [-p parallel_simulations] config_file" >&2 
	exit 1
fi

LAB_DIR=`pwd`
BIN_DIR=$LAB_DIR/bin
SETUP_DIR=$LAB_DIR/setup
SIMULATIONS_DIR=$LAB_DIR/simulations
DATA_DIR=$LAB_DIR/data

SCHIAAS_BIN_DIR=`readlink -f ../bin`

function setupify {
	res=""
	for a in $*
	do
		a="${a##*([[:space:]])}"
		a="${a%%*([[:space:]])}"
		a=${a//SCHIAAS_BIN_DIR/$SCHIAAS_BIN_DIR}

		if [ -e "$SETUP_DIR/$a" ]; then
			a="`readlink -f $SETUP_DIR/$a`"
		elif [ -e "$a" ]; then
			a="`readlink -f \"$a\"`"
		fi

		res="$res $a"
	done
	echo -n $res
}

[[ ! -v KEEP ]] && rm -rf $DATA_DIR
mkdir -p $DATA_DIR

while read line
do
	if [ -z "$line" ]; then
		continue
	elif [ "${line:0:1}" == "#" ]
	then
		if [ "${line%% *}" == "#SETUP_DIR" ]; then
			SETUP_DIR="`setupify \"${line#* }\"`"
			echo "SETUP_DIR='${SETUP_DIR}'"
			if [ ! -d $SETUP_DIR ] ; then 
				echo "Error: SETUP_DIR $SETUP_DIR was not found"
				exit 1
			fi
		elif [ "${line%% *}" == "#R_SCRIPT" ]; then
			R_SCRIPT="`setupify \"${line#* }\"`"
			echo "R_SCRIPT='$R_SCRIPT'"
			if [ ! -f $R_SCRIPT ] ; then 
				echo "Error: R_SCRIPT $R_SCRIPT was not found"
			fi
		elif [ "${line%% *}" == "#TU_ARGS" ]; then
			TU_ARGS="${TU_ARGS} `setupify ${line#* }`"
			echo "TU_ARGS=$TU_ARGS"
		elif [ "${line%% *}" == "#JAVA_START_ARGS" ]; then
			JAVA_START_ARGS="${JAVA_START_ARGS} `setupify ${line#* }`"
			echo "JAVA_START_ARGS=$JAVA_START_ARGS"
		elif [ "${line%% *}" == "#JAVA_END_ARGS" ]; then
			JAVA_END_ARGS="${JAVA_END_ARGS} `setupify ${line#* }`"
			echo "JAVA_END_ARGS=$JAVA_END_ARGS"
		fi
	else
		while [ `ps -Af | grep -c "$JAVA_START_ARGS"` -ge $(( PARALLEL_SIMS +1)) ] ; do
			sleep 1
		done

		XP_ID=${line%%:*}; XP_ID=${XP_ID/ /_}
		XP_ID_LIST="$XP_ID_LIST $XP_ID"
		JAVA_XP_ARGS="`setupify ${line#*:}`"

		echo "Simulating $XP_ID"

		XP_SIMULATION_DIR=$SIMULATIONS_DIR/$XP_ID
		rm -rf $XP_SIMULATION_DIR 2> /dev/null
		mkdir -p $XP_SIMULATION_DIR

		cd $XP_SIMULATION_DIR
		(
			java $JAVA_START_ARGS $JAVA_XP_ARGS $JAVA_END_ARGS 2> simgrid.out 1>&2
			$BIN_DIR/trace-util.py schiaas.trace -f -p $XP_ID -d $DATA_DIR -r $TU_ARGS 
		) &
		SIM_PIDS="$SIM_PIDS $!"

	fi
done < $1

wait $SIM_PIDS

echo "Gathering results"
cd $DATA_DIR
for XP_ID in $XP_ID_LIST
do
	echo -e "\n##################################### $XP_ID" >> reads.R
	cat ${XP_ID}.reads.R >> reads.R
	echo "##################################### $XP_ID" >> plots.R
	cat ${XP_ID}.plots.R >> plots.R
done

if [ -v R_SCRIPT ] ; then 
	echo "Plotting results"
	R --no-save < $R_SCRIPT > R.out
fi
