## This script executes simulations according to one config file.
## It takes the configuration file as argument.
## It needs the java classpath to be set.
##
## The configuration file format is as follows:
## SETUP_DIR indicates the directory containing setup files (should be called first)
## R_SCRIPT indicates the R script to call at he end of the simulations
## TU_ARG indicates the arguments to pass to the trace_util.py script
## SIM_ARG x arg [id] indicates the argument of the simulation
##  - *x*: the number of the argument;
##  - *arg*: the argument;
##  - *id*: the id of the simulations using this argument. 
## 
## see lab/setup/cmp-scheduler/cmp-scheduler.cfg for example
##
## Two options are available:
## -k : keeps the data from previous executions
## -p x : run x simulations and x trace-util.py in parallel,
##
## @author julien.gossa@unistra.fr

# provided to implement a 'readlink -f' on all systems
function abspath {
  echo "$(cd $(dirname $1); pwd)/$(basename $1)"
}



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
		JAVA_PS_COUNT=`ps aux | grep java | wc -l`
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

[[ "$CLASSPATH" ]] && echo "CLASSPATH=$CLASSPATH" || echo "WARNING: CLASSPATH not set"

LAB_DIR=`pwd`
BIN_DIR=$LAB_DIR/bin
SETUP_DIR=$LAB_DIR/setup
SIMULATIONS_DIR=$LAB_DIR/simulations
DATA_DIR=$LAB_DIR/data

SCHIAAS_DIR="../bin"
SCHIAAS_BIN_DIR=`abspath $SCHIAAS_DIR`

function setupify {
	res=""
	for a in $*
	do
		a="${a##*([[:space:]])}"
		a="${a%%*([[:space:]])}"
		a=${a//SCHIAAS_BIN_DIR/$SCHIAAS_BIN_DIR}

		if [ -e "$SETUP_DIR/$a" ]; then
			a="`abspath $SETUP_DIR/$a`"
		elif [ -e "$a" ]; then
			a="`abspath \"$a\"`"
		fi

		res="$res $a"
	done
	echo -n $res
}

[ ! "$KEEP" ] && rm -rf $DATA_DIR $SIMULATIONS_DIR
mkdir -p $DATA_DIR $SIMULATIONS_DIR

SIM_ARG_FILE=$SIMULATIONS_DIR/simulations.args
echo "xp " > $SIM_ARG_FILE
SIM_ARG_TMP_FILE=`tempfile`

declare -A TU_ARGS

#Reading the configuration file
while read line
do
	if [ -z "$line"  ]; then
		continue
	fi
	COMMAND="${line%% *}"
	ARGS="`setupify \"${line#* }\"`"

	if [ "$COMMAND" == "SETUP_DIR" ]; then
		SETUP_DIR="$ARGS"
		if [ ! -d $SETUP_DIR ] ; then 
			echo "Error: SETUP_DIR $SETUP_DIR was not found"
			exit 1
		fi

	elif [ "$COMMAND" == "R_SCRIPT" ]; then
		R_SCRIPT="$ARGS"
		if [ ! -f $R_SCRIPT ] ; then 
			echo "Error: R_SCRIPT $R_SCRIPT was not found"
		fi

	elif [ "$COMMAND" == "TU_ARG" ]; then
		TU_COMMAND=${ARGS%% *}
		TU_COMMAND_ARGS=${ARGS#* }
		
		if [ -z "${TU_ARGS[$TU_COMMAND]}" ]; then
			TU_ARGS[$TU_COMMAND]="$TU_COMMAND"
		fi
		TU_ARGS[$TU_COMMAND]="${TU_ARGS[$TU_COMMAND]} $TU_COMMAND_ARGS"
	
	elif [ "$COMMAND" == "SIM_ARG" ]; then
		SIM_ARG=($ARGS)
		if [ "${SIM_ARG[0]}" != "$SIM_ARG_NUM" ] ; then
			mv $SIM_ARG_FILE $SIM_ARG_TMP_FILE
			SIM_ARG_NUM="${SIM_ARG[0]}"
		fi
		cat $SIM_ARG_TMP_FILE \
			| sed "s/$/ ${SIM_ARG[1]//\//\\\/}/" \
			| sed "s/ /_${SIM_ARG[2]} /" \
			| tr -s "_"  \
			>> $SIM_ARG_FILE
	fi

done < $1

echo "SETUP_DIR='${SETUP_DIR}'"
echo "R_SCRIPT='$R_SCRIPT'"
echo "TU_ARGS=${TU_ARGS[@]}"

#Doing the simulations
JAVA_THREADS="`ps -Af | grep -c java`"
while read line
do
	XP_ID=${line%% *}
	JAVA_XP_ARGS=${line#* }

	XP_SIMULATION_DIR=$SIMULATIONS_DIR/$XP_ID
	mkdir -p $XP_SIMULATION_DIR

	while [ `ps -Af | grep -c java` -ge $(( PARALLEL_SIMS + JAVA_THREADS )) ] ; do
		sleep 1
	done
	cd $XP_SIMULATION_DIR
	(
		if [ ! -e $XP_SIMULATION_DIR/schiaas.trace ] ; then
			echo "Simulating $XP_ID"
		 	java $JAVA_XP_ARGS 2> simgrid.out 1>&2
		 	if [ $? -ne 0 ]; then echo "Critical error while executing $XP_ID" ; cat $XP_SIMULATION_DIR/simgrid.out ; exit $? ; fi
		fi

		while [ `ps -Af | grep -c "trace-util.py"` -ge $(( PARALLEL_SIMS +1)) ] ; do
			sleep 1
		done

		echo "Processing $XP_ID traces"
		$BIN_DIR/trace-util.py schiaas.trace -o $DATA_DIR -f $XP_ID ${TU_ARGS[@]}
	) &
	SIM_PIDS="$SIM_PIDS $!"
done < $SIM_ARG_FILE


wait $SIM_PIDS

if [ -n "$R_SCRIPT" ] ; then 
	echo "Plotting results"
	cd $DATA_DIR
	R -f $R_SCRIPT > R.out
	cd ..
fi
