#!/bin/bash
cd $(cd `dirname $0` && pwd)

if [ $# -ne 2 ] 
then 
	echo -e "usage: $0 TASKS_FILE STRATEGY \nSTRATEGY can be either OneVM4All, AFAP, or ASAP"
	exit
fi

java -cp /usr/local/java/simgrid.jar:simschlouder.jar simschlouder.SimSchlouder \
	simschlouder.xml $1 $2 --log="root.thres:info" 2> /dev/null | grep -v "#"
