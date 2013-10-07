#! /bin/bash

nhosts="1 1000 1 10 100 1000"
ncores="1 1000 10 100 1000 10000"
nvms="1000 10 100 1000"

infras="CLOUD VM VMHOST HOST"

echo -e "nhosts \tncores \ttcores \tnvms \tcloud \tvm \tvmhost \thost"

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

echo -en "$nhost \t$ncore \t$ntotalcore \t$nvm "

for infra in $infras; do
	exectime=`( command time -f '%e' java -cp /usr/local/java/simgrid.jar:../../bin/schiaas.jar:../../bin/allmasterslave.jar \
			allmasterslave.Masterslave \
			platform.xml deploy.xml cloud.xml $infra \
			--log=root.thres:critical \
			3>&1 1>&2- 2>&3- ) | tail -1 ` 
	
	echo -en "\t$exectime "
done

echo

		done
	done
done