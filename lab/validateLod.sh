if [ $# -ne 4 ] ; then
	echo "Usage : $0 [source] [level of detail] $*" >&2 
	echo -en "\tNA\tNA\tNA"
	exit 1
fi

SS_CLASSPATH="/usr/local/java/simgrid.jar:$PWD/../bin/schiaas.jar:$PWD/../bin/simschlouder/simschlouder.jar"


source=$1
sourceFilename=$(basename "$source")
lod=$2
CLOUD_FILE=$3
STRATEGY=$4


taskFile=${PWD}/data/${sourceFilename%.*}-${lod}.tasks
jsonFile=${PWD}/data/${sourceFilename%.*}-${lod}.json
outFile=${PWD}/data/${sourceFilename%.*}-${lod}.out
errFile=${PWD}/data/${sourceFilename%.*}-${lod}.err


./json2tasks.py $lod $source > $taskFile 2> $errFile
if [ "$?" -ne 0 ] ; then
	echo "Error $lod: Json file incomplete" >&2
	echo -en "\tNA\tNA\tNA"
	exit 2
fi


echo "Simulating $lod" >&2
cd simschlouder
java -classpath $SS_CLASSPATH simschlouder.SimSchlouder $CLOUD_FILE $taskFile $STRATEGY $jsonFile 2> $outFile 1>&2
cd ..

echo "Calculating diameters $lod" >&2
./json2diameter.py $jsonFile concurrent-jobs/data/simschlouder-${lod} >&2

echo "Drawing Tickz $lod" >&2
stat=`./json2tikz.py $jsonFile btuviewer/data/simschlouder-${lod}`

echo -en "\t$stat"