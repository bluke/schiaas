#!/bin/bash

if [ $# -eq 0 ]; then
	echo "Do the pre-processing of the validation."
	echo "Usage : $0 tasks_file [tasks_file...]"
	exit 1
fi

resdir="/tmp/mctasks"
rm -rf $resdir
mkdir -p $resdir

echo "$*" | tr " " "\n" > $resdir/taskfiles
setups="`cat $resdir/taskfiles | cut -f1-7 -d'.' | sort -u`"

for setup in $setups
do
	tasksfile=`grep "$setup" $resdir/taskfiles | head -1`

	id=`echo ${tasksfile##*/} | cut -f1-8 -d"."`
	provisioning=`echo $id | cut -f4 -d"." | tr "a-z" "A-Z"`
	xml="simschlouder.`echo $id | cut -f5-7 -d"."`.xml"
	
	./generate_sim_numrange.py -f $tasksfile -m draw -t multiply  \
		-s 0.05	\
		-r 500 \
		-p $resdir \
		| awk "{ print \"SIM_ARG 2:${id}_\"NR\" ${xml} $provisioning \",\$1 }"
done

