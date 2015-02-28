#!/usr/bin/python -O
# -*- coding:utf8 -*-

"""
	This script updates the JSON output from Schlouder for validation purposes
"""

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


ordered_job_names = []

#  is montage?
if 'pleiade' in args.src_json_file: 
	is_montage = True
else:
	is_montage = False


# Job comparator: by by start_date, then by sub_date
def cmpjob(j1, j2):
	if j1['start_date'] != j2['start_date']:
		return int(j1['start_date'] - j2['start_date'])
	return int(j1['submission_date'] - j2['submission_date'])
	
# Vm comparator: by start_date, then by boot_time_prediction
def cmpvm(vm1, vm2):
	first_job1 = sorted(vm1['jobs'], key=lambda j: j['start_date'])[0]
	first_job2 = sorted(vm2['jobs'], key=lambda j: j['start_date'])[0]

	if vm1['start_date'] != vm2['start_date']:
		return int(vm1['start_date'] - vm2['start_date'])

	if first_job1['start_date'] != first_job2['start_date']:
		return int(first_job1['start_date'] - first_job2['start_date'])

	return (ordered_job_names.index(first_job1['name']) - ordered_job_names.index(first_job2['name']) )


# Job comparator: by name, for pleiade
task_types = ['project', 'overlaps', 'diff', 'bgmodel', 'background', 'add', 'gather']
def cmpjob_name(j1, j2):
	# if not pleiade	
	if not is_montage :
		if j1 == j2 : return 0
		if j1 < j2 : return -1
		return 1

	js1 = j1.split('_')
	js2 = j2.split('_')

	# project types
	if len(js1) < 5 : return 1 # for gather
	if len(js2) < 5 : return -1 # for gather
	if js1[4] != js2[4] :
		return task_types.index(js1[4]) - task_types.index(js2[4])

	# h j k
	if js1[2] != js2[2] :
		if js1[2] < js2[2] : return -1
		return 1

	# number
	if len (js1)<5 : return 0
	return int(js1[5]) - int(js2[5])


# Open the simulation JSON
with open(args.sim_json_file) as fp:
	results = json.load(fp)
	# If the json is a more recent version
	if 'nodes' in results:
		results = results['nodes']

for vm in results:
	for job in vm['jobs']:
		if not 'name' in job:
			job['name'] = job['id']

		ordered_job_names.append(job['name'])
ordered_job_names.sort(cmpjob_name)

results.sort(cmp=cmpvm)


i = 0
for vm in results:
	print("{0}\t{1}\t{2}\t{3}\t{4}".format(
		i,
		vm['host'],
		vm['start_date'],
		vm['boot_time_prediction'],
		vm['boot_time']))
	i=i+1


# Check on which vm each job is mapped
vm_of_jobs_sim = {}
for vm in results:
	for job in vm['jobs']:
		vm_of_jobs_sim[job['name']]=results.index(vm)

# Open the JSON to update
with open(args.src_json_file) as fp:
	results = json.load(fp)
	# If the json is a more recent version
	if 'nodes' in results:
		results = results['nodes']
results.sort(cmp=cmpvm)

i = 0
for vm in results:
	print("{0}\t{1}\t{2}\t{3}\t{4}\t{5}".format(
		i,
		vm['host'],
		vm['start_date'],
		vm['boot_time_prediction'],
		vm['boot_time'],
		vm['start_date']+vm['boot_time']))
	i=i+1


jobs = []
vm_of_jobs_src = {}
for vm in results:
	jobs += vm['jobs']
	for job in vm['jobs']:
		vm_of_jobs_src[job['name']]=results.index(vm) 

ordered_job_names = []
for job in jobs:
	if not 'name' in job:
		job['name'] = job['id']
	ordered_job_names.append(job['name'])
ordered_job_names.sort(cmpjob_name)

#print ordered_job_names

jobs.sort(cmpjob)
for job in jobs:
	print("{0}\t{1}\t{2}\t{3}\t{4}".format(
		ordered_job_names.index(job['name']),
		job['name'],
		vm_of_jobs_src[job['name']],
		vm_of_jobs_sim[job['name']],
		(vm_of_jobs_src[job['name']] == vm_of_jobs_sim[job['name']])
		))
		
print("")

