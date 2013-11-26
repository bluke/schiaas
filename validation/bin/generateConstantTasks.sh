#!/bin/bash

if [ $# -ne 4 ]
then 
	echo "Usage : $0 nb_of_bags nb_of_tasks_per_bags runtime interarrival"
	exit -1
fi

iB=0
ST=0
while [ $iB -lt $1 ]
do
	iT=0
	while [ $iT -lt $2 ]
	do 
		RT=$3
		echo -e "${iB}_${iT}\t$ST\t$RT"
		((iT++))
	done
	ST=$4
	((iB++))
done
