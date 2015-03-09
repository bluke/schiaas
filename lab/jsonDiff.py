#!/usr/bin/python -O
# -*- coding:utf8 -*-

##	This script takes 2 json files, supposed to concern the same experiment,
##  and outputs for each task whether the sceduling decision is the same.
##
##  @author julien.gossa@unistra.fr

import sys
import os
import json
import argparse


def usage():
	print >> sys.stderr, 'Usage: json_file_to_update json_file_from_simulation'.format(sys.argv[0])

parser = argparse.ArgumentParser(description='Extract info from json.')
parser.add_argument('src_json_file', help='json file to update')
parser.add_argument('sim_json_file', help='simulation json file')


args = parser.parse_args()

# Job comparator
def cmpjob(j1, j2):
	if j1['scheduled_date'] != j2['scheduled_date']:
		return int(j1['scheduled_date'] - j2['scheduled_date'])
	return int(j1['index'] - j2['index'])


# Open the simulation JSON
with open(args.sim_json_file) as fp:
	results = json.load(fp)['nodes']
results.sort(key=lambda vm:int(vm['index']))

#i = 0
#for vm in results:<
#	print("{0}\t{1}\t{2}\t{3}\t{4}".format(
#		i,
#		vm['host'],
#		vm['start_date'],
#		vm['boot_time_prediction'],
#		vm['boot_time']))
#	i=i+1


# Check on which vm each job is mapped
vm_of_jobs_sim = {}
for vm in results:
	for job in vm['jobs']:
		vm_of_jobs_sim[job['name']]=results.index(vm)

# Open the JSON to compare to
with open(args.src_json_file) as fp:
	results = json.load(fp)['nodes']
results.sort(key=lambda vm:int(vm['index']))

#i = 0
#for vm in results:
#	print("{0}\t{1}\t{2}\t{3}\t{4}\t{5}".format(
#		i,
#		vm['host'],
#		vm['start_date'],
#		vm['boot_time_prediction'],
#		vm['boot_time'],
#		vm['start_date']+vm['boot_time']))
#	i=i+1


jobs = []
vm_of_jobs_src = {}
for vm in results:
	jobs += vm['jobs']
	for job in vm['jobs']:
		vm_of_jobs_src[job['name']]=results.index(vm) 

jobs.sort(cmpjob)
for job in jobs:
	print("{0}\t{1}\t{2}\t{3}\t{4}".format(
		job['index'],
		job['name'],
		vm_of_jobs_src[job['name']],
		vm_of_jobs_sim[job['name']],
		(vm_of_jobs_src[job['name']] == vm_of_jobs_sim[job['name']])
		))
		
print("")

