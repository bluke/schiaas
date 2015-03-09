#!/usr/bin/python -O
# -*- coding:utf8 -*-

##	This script takes a JSON output from Schlouder as input, and output 
##  an updated JSON, with missing values added, and things ordered.
##
##  @author julien.gossa@unistra.fr

import sys
import os
import json
import argparse
import subprocess

def usage():
	print >> sys.stderr, 'Usage: json_file bot_file'.format(sys.argv[0])

parser = argparse.ArgumentParser(description='Extract info from json.')
parser.add_argument('json_file', help='input json file')

args = parser.parse_args()

ordered_job_names = []

#  is montage?
if 'pleiade' in args.json_file: 
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

	return cmpjob_name(first_job1, first_job2)


# Job comparator: by name, for pleiade
task_types = ['project', 'overlaps', 'diff', 'bgmodel', 'background', 'add', 'gather']
def cmpjob_name(j1, j2):
	# if not pleiade	
	if not is_montage :
		if j1['name'] == j2['name'] : return 0
		if j1['name'].replace("_","+") < j2['name'].replace("_","+"): return -1
		return 1

	js1 = j1['name'].split('_')
	js2 = j2['name'].split('_')

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


lastDate = 0

# Open the JSON
with open(args.json_file) as fp:
	fullresults = json.load(fp)
	# If the json is a more recent version
	if 'nodes' in fullresults:
		results = fullresults['nodes']
	else:
		results = fullresults
		fullresults = {}
		fullresults['nodes'] = results
		fullresults['info'] = {}

jobs = []
results.sort(cmpvm)
for vm in results:
	if not 'host' in vm:
		vm['host'] = vm['instance_id']	
	jobs += vm['jobs']

# Get the first submission date of the jobs
first_submission_date = sorted(jobs, key=lambda j: j['submission_date'])[0]['submission_date']


# to fix the missing scheduled_date
current_min_scheduled_date = first_submission_date
future_min_scheduled_date = results[0]['start_date']
min_scheduled_date = {}
for vm in results:
	if vm['start_date'] > future_min_scheduled_date:
		current_min_scheduled_date = future_min_scheduled_date
		future_min_scheduled_date = vm['start_date']
#	print ("{0} {1} {2}".format(
#		vm['instance_id'], current_min_scheduled_date, future_min_scheduled_date))
	for job in vm['jobs']:
		min_scheduled_date[job['id']] = current_min_scheduled_date
		if job['submission_date'] == 0:
			min_scheduled_date[job['id']] += 1



# update the jobs
scheduled_dates_status = "logged"
jobs.sort(cmpjob_name)
scheduled_date = 0
for job in jobs:
	# add the standard_walltimes
	if not 'standard_walltime' in job:
		job['standard_walltime'] = job['walltime']
	# fix the missing scheduled_dates
	if not 'scheduled_date' in job:
		scheduled_dates_status = "infered"
		
		job['scheduled_date'] = max(
			scheduled_date,
			job['submission_date'] + min_scheduled_date[job['id']] - first_submission_date )
		
		if 'dependencies' in job and len (job['dependencies']) > 0:
			for dep in job['dependencies']:
				if job['scheduled_date'] < jobs_dict[dep]['start_date']+jobs_dict[dep]['walltime']:
					job['scheduled_date'] = jobs_dict[dep]['start_date']+jobs_dict[dep]['walltime']
		else:
			scheduled_date = job['scheduled_date']

	# fix the dates
	job['submission_date'] -= first_submission_date
	job['start_date'] -= first_submission_date
	job['scheduled_date'] -= first_submission_date

	# add the index
	if 'index' not in job:
		job['index'] = jobs.index(job)
	

# Update the vms
results.sort(cmp=cmpvm)
for vm in results:
	vm['start_date'] -= first_submission_date
	if vm['stop_date'] is not None:
		vm['stop_date'] -= first_submission_date
	if 'index' not in vm:
		vm['index'] = results.index(vm)


# Update the general infos
fullresults['info']['updated_to_version'] = subprocess.check_output(['git', 'log', '-n1', '--format=format:"%H"', 'jsonUpdater.py'])
fullresults['info']['first_date'] = first_submission_date
fullresults['info']['scheduled_dates'] = scheduled_dates_status

# Output the result
print json.dumps(fullresults, sort_keys=True,
	indent=2, separators=(',', ': '))