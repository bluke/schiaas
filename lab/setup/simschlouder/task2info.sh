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

if [[ $command == *i* ]] ; then
	echo -n "`echo $1 | tr '/' '.' | cut -f2-7,9 -d'.'` "
fi

if [[ $command == *x* ]] ; then
	[[ $1 == *remoteio* || $1 == *migration* || $1 == *montecarlo* ]] && vmlimit="-10" || vmlimit=""
	[[ $1 == *v3.standard.[23]x[23].asap.regular.openstack-icps.[234]* ]] && vmlimit="-10"
	[[ $1 == *monitoring* ]] && monitoring="-m" || monitoring=""

	stopla=`echo $1 | cut -f5,6 -d"."`
	btp=`echo $1 | cut -f7 -d"."`

	echo -n "./xml/simschlouder.$stopla$vmlimit$monitoring.$btp.xml "
fi

if [[ $command == *p* ]] ; then
	echo -n "`echo $1 | cut -f4 -d'.' | tr 'a-z' 'A-Z'` "
fi

if [[ $command == *j* ]] ; then
	echo -n "`echo ${1#*\/} | cut -f1-6 | sed 's/\./\//'` "
fi

echo
