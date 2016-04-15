#! /bin/bash
# This script randomize tasks files and output corresponding lab.cfg lines

RANDTASK_DIR=./montecarlo

for i in `seq 1 $2`
do
	taskfile=$i-`basename $1`
	cp $1 $RANDTASK_DIR/$taskfile
	echo 3:$taskfile $taskfile
done