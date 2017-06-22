#!/bin/bash

for taskfile in $*
do
	echo "SIM_ARG 2:`./task2info.sh -ixp $taskfile` $taskfile"
done