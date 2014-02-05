#!/bin/bash

if [ $# -ne 2 ]
then 
	echo "Usage : $0 nb_of_bags nb_of_tasks_per_bags"
	exit -1
fi

iB=0
ST=0
while [ $iB -lt $1 ]
do
	iT=0
	while [ $iT -lt $2 ]
	do 
		RT=$(($RANDOM % 4000))
		input_size=$(($RANDOM*10000))
		output_size=$(($RANDOM*10000))
		echo -e "${iB}_${iT}\t$ST\t$RT\t$input_size\t$output_size"
		((iT++))
	done
	ST=$((ST+$RANDOM % 10000))
	((iB++))
done
