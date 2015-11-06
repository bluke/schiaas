## This script executes simulations according to one config file.
## It takes the configuration file as argument.
## It needs the java classpath to be set.
##
## The configuration file format is as follows.
## Lines stating by:
## ~ indicates the R template to call at he end of the simulations
## % indicates the arguments to pass to the trace_util.py script
## ^ indicates the common first arguments to pass to the simulator
## $ indicates the common last arguments to pass to the simulator
## Other lines indicates the specific arguments inbetween for each simulation
##
## @author julien.gossa@unistra.fr

if [ $# -ne 1 ] ; then
	echo "Takes one argument. Needs to run in the lab directory"
	echo "Usage : $0 config_file" >&2 
	exit 1
fi

LAB_DIR=`pwd`
BIN_DIR=$LAB_DIR/bin
SETUP_DIR=$LAB_DIR/setup
SIMULATIONS_DIR=$LAB_DIR/simulations
DATA_DIR=$LAB_DIR/data
R_SCRIPT=$SCHED_DIR/template.R

SCHIAAS_BIN_DIR=`readlink -f ../bin`

function setupify {
	for a in $*
	do
		if [ -e $SETUP_DIR/$a ]; then
			a="`readlink -f $SETUP_DIR/$a`"
		elif [ -e $a ]; then
			a="`readlink -f $a`"
		fi
		
		echo -n "${a//SCHIAAS_BIN_DIR/$SCHIAAS_BIN_DIR} "
	done
}

mkdir -p $DATA_DIR

while read line
do
	if [ -z "$line" ]; then
		continue
	elif [ "${line:0:1}" == "~" ]; then
		R_SCRIPT="`setupify ${line:1}`"
	elif [ "${line:0:1}" == "%" ]; then
		TU_ARGS="${TU_ARGS} ${line:1}"
	elif [ "${line:0:1}" == "^" ]; then
		JAVA_START_ARGS="${JAVA_START_ARGS} ${line:1}"
	elif [ "${line:0:1}" == "$" ]; then
		JAVA_END_ARGS="${JAVA_END_ARGS} ${line:1}"
	else
		XP_ID=${line%%:*}; XP_ID=${XP_ID/ /_}
		JAVA_XP_ARGS=${line#*:}

		echo "Simulating $XP_ID"

		XP_SIMULATION_DIR=$SIMULATIONS_DIR/$XP_ID
		rm -rf $XP_SIMULATION_DIR 2> /dev/null
		mkdir -p $XP_SIMULATION_DIR

		JAVA_ARGS=`setupify $JAVA_START_ARGS $JAVA_XP_ARGS $JAVA_END_ARGS`

		cd $XP_SIMULATION_DIR

		java $JAVA_ARGS 2> simgrid.out 1>&2

		TU_ARGS=`setupify $TU_ARGS`
		$BIN_DIR/trace-util.py schiaas.trace -f -p $XP_ID -d $DATA_DIR -r $TU_ARGS 
	fi
done < $1

if [ -v R_SCRIPT ] ; then 
	cd $DATA_DIR
	R --no-save < $R_SCRIPT
fi
