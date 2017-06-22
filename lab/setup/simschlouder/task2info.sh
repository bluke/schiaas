#!/bin/bash

#echo "SIM_ARG 2:${out%%.json} ./xml/simschlouder.$storage.$platform$vmlimit$monitoring.$btp.xml $provisioning ./tasks/${out%%json}tasks" >> $LAB_CONFIG_FILE

if [ $# -ne 2 ] ; then
	echo "Output infos from task filename"
	echo "Usage : $0 -[ixpj] task_file"
	echo "i print the id of the xp"
	echo "x print xml platform filename"
	echo "p print provisioning strategy"
	echo "j print json filename"
	exit 1
fi

command=$1
shift

taskfile=`basename $1`

id=`echo $taskfile | cut -f1-8 -d'.'`
xml=simschlouder.`echo $taskfile | cut -f5-7 -d'.'`.xml
provisioning=`echo $taskfile | cut -f4 -d'.' | tr [a-z] [A-Z]`
json=$id.json

if [[ $command == *i* ]] ; then
	echo -n "$id "
fi

if [[ $command == *x* ]] ; then
	echo -n "$xml "
fi

if [[ $command == *p* ]] ; then
	echo -n "$provisioning "
fi

if [[ $command == *j* ]] ; then
	echo -n "$json "
fi