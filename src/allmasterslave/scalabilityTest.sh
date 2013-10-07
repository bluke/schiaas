#! /bin/bash

nhosts="1 10 100 1000"
ncores="1 10 100 1000 10000"
nvms="1 10 100 1000"

echo -e "nhosts \tncores \tntotalcores \tnvms \tcloud \tvm \thost"

for nhost in $nhosts; do
	for ncore in $ncores; do
		for nvm in $nvms; do

ntotalcore=$(( ncore * nhost ))

if [ $nvm -gt $ntotalcore ]; then
	break
fi

for file in platform deploy cloud ; do
cat template-${file}.xml \
	| sed "s/%NHOST/$nhost/g" \
	| sed "s/%NCORE/$ncore/g" \
	| sed "s/%NVM/$nvm/g" > ${file}.xml
done


exectimeC=`( command time -f '%e' java -cp /usr/local/java/simgrid.jar:../bin/schiaas.jar:../bin/cloudmasterslave.jar \
		cloudmasterslave.Masterslave \
		platform.xml deploy.xml cloud.xml CLOUD \
		--log=root.thres:critical \
		3>&1 1>&2- 2>&3- ) | tail -1 ` 
	
exectimeV=`( command time -f '%e' java -cp /usr/local/java/simgrid.jar:../bin/schiaas.jar:../bin/cloudmasterslave.jar \
		cloudmasterslave.Masterslave \
		platform.xml deploy.xml cloud.xml VM \
		--log=root.thres:critical \
		3>&1 1>&2- 2>&3- ) | tail -1 ` 

exectimeH=`( command time -f '%e' java -cp /usr/local/java/simgrid.jar:../bin/schiaas.jar:../bin/cloudmasterslave.jar \
		cloudmasterslave.Masterslave \
		platform.xml deploy.xml cloud.xml HOST \
		--log=root.thres:critical \
		3>&1 1>&2- 2>&3- ) | tail -1 ` 
echo -e "$nhost \t$ncore \t$ntotalcore \t$nvm \t$exectimeC \t$exectimeV \t$exectimeH"

		done
	done
done