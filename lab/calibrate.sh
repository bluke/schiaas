#!/bin/bash

## This scripts computes B0 and B1 of predicted boot times from a set of json fils

if [ $# -lt 1 ]
then 
	echo "Usage : $0 json_file [json_files...]"
	exit -1
fi

DATAFILE=/tmp/boottimes.dat
RRESFILE=/tmp/r.res

echo -e "boots \t time" > $DATAFILE
for file in $*
do
	cat $file | grep '"boot_time"' | cut -f2 -d":" | sed 's/,//' | sort -n | nl >> $DATAFILE
done

echo 'btdat <- read.csv(file="/tmp/boottimes.dat",head=TRUE,sep="\t") ; res <- lm(btdat$time ~ btdat$boots) ;  res$coefficients[1];  res$coefficients[2] ; res$coefficients[1]+res$coefficients[2] ' | R --no-save --silent > $RRESFILE

B0=`cat $RRESFILE | head -3 | tail -1`
B1=`cat $RRESFILE | head -5 | tail -1`
BT=`cat $RRESFILE | head -7 | tail -1`

echo -e " B0 =            $B0 \n B1 =              $B1 \n Image BootTime = $BT "

