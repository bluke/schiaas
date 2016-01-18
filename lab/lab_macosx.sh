## This script executes simulations according to one config file.
## It takes at least one configuration file as argument.
## It needs the java classpath to be set.
##
## The configuration file format is as follows:
## SETUP_DIR indicates the directory containing setup files (should be called first)
## INCLUDE indicates a file to read as addtional configuration file
## PRE_COMMAND_SETUP indicates a command to run before the simulation in the setup directory
## POST_COMMAND_DATA indicates a command to run in the data directory
## TU_ARG indicates the arguments to pass to the trace_util.py script
## SIM_ARG x[:id] arg indicates the argument of the simulation
##  - x: the id of the argument (must be grouped);
##  - id: the id of the simulations using this argument;
##  - arg: the argument.
## 
## see lab/setup/cmp-scheduler/cmp-scheduler.cfg for example
##
## Two options are available:
## -k : keeps the data from previous executions
## -p x : run x simulations and x trace-util.py in parallel,
##
## @author julien.gossa@unistra.fr


SED=gsed

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

if [ $# -lt 1 ] ; then
	echo "Takes at least one argument. Needs to run in the lab directory"
	echo "Usage : $0 [-k] [-p parallel_simulations] config_file [config_files...]" >&2 
	exit 1
fi

[[ "$CLASSPATH" ]] && echo "CLASSPATH=$CLASSPATH" || echo "WARNING: CLASSPATH not set"

LAB_DIR=`pwd`
BIN_DIR=$LAB_DIR/bin
SETUP_DIR=$LAB_DIR/setup
SIMULATIONS_DIR=$LAB_DIR/simulations
DATA_DIR=$LAB_DIR/data
POST_COMMAND_DATA=""
PRE_COMMAND_SETUP=""

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
SIM_ARG_TMP_FILE=`mktemp`

#Reading the configuration files
while [ -n "$1" ]
do 
	while read line || [ -n "$line" ]
	do
		if [ -z "$line" -o "${line:0:1}" == "#" ]; then
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

		elif [ "$COMMAND" == "POST_COMMAND_DATA" ]; then
			POST_COMMAND_DATA="$POST_COMMAND_DATA $ARGS ;"

		elif [ "$COMMAND" == "PRE_COMMAND_SETUP" ]; then
			PRE_COMMAND_SETUP="$PRE_COMMAND_SETUP $ARGS ;"

		elif [ "$COMMAND" == "INCLUDE" ]; then
			set $* $ARGS
			echo $ARGS

		elif [ "$COMMAND" == "TU_ARG" ]; then
			TU_COMMAND=${ARGS%% *}
			TU_COMMAND_ARGS=${ARGS#* }
			
			tv=TU_ARGS_${TU_COMMAND//-/_}
			if [ -z "${!tv}" ]; then
				eval TU_ARGS_${TU_COMMAND//-/_}="$TU_COMMAND"
				tv=TU_ARGS_${TU_COMMAND//-/_}
			fi
			eval TU_ARGS_${TU_COMMAND//-/_}="\"${!tv} $TU_COMMAND_ARGS\""
		
		elif [ "$COMMAND" == "SIM_ARG" ]; then
			SIM_ARG_ID=${ARGS%% *}
			SIM_ARG_ARG=${ARGS#* }
			s=(${SIM_ARG_ID/:/ })
			SIM_ARG_ID_NUM=${s[0]}
			SIM_ARG_ID_ID=${s[1]}
			if [ "$SIM_ARG_ID_NUM" != "$OLD_SIM_ARG_ID_NUM" ] ; then
				mv $SIM_ARG_FILE $SIM_ARG_TMP_FILE
				OLD_SIM_ARG_ID_NUM="$SIM_ARG_ID_NUM"
			fi
			cat $SIM_ARG_TMP_FILE \
				| ${SED} "s@\$@${SIM_ARG_ARG} @"\
				| ${SED} "s@ @_${SIM_ARG_ID_ID} @"\
				| tr -s "_"\
				>> $SIM_ARG_FILE
		fi

	done < $1
	shift
done

for tua in ${!TU_ARGS_*} ; do TU_ARGS="$TU_ARGS ${!tua} "; done

echo "SETUP_DIR='${SETUP_DIR}'"
echo "TU_ARGS='$TU_ARGS'"
echo "PRE_COMMAND_SETUP='$PRE_COMMAND_SETUP'"
echo "POST_COMMAND_DATA='$POST_COMMAND_DATA'"


# Pre command
( cd $SETUP_DIR ; eval $PRE_COMMAND_SETUP )


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

# Post command
if [ -n "$POST_COMMAND_DATA" ] ; then 
	echo "Executing post commands" $POST_COMMAND_DATA
	( cd $DATA_DIR ; eval $POST_COMMAND_DATA )
fi
