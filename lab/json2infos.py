#!/usr/bin/python -O
# -*- coding:utf8 -*-

"""
	This script take a JSON output from Schlouder as input, and output 
	a bot file in the simschlouder format
"""

import sys
import os
import json
import argparse


def usage():
	print >> sys.stderr, 'Usage: json_file bot_file'.format(sys.argv[0])

parser = argparse.ArgumentParser(description='Extract info from json.')
parser.add_argument('json_file', help='input json file')

args = parser.parse_args()

# Job comparator: by sub_date, then by start_date
def cmpjob(j1, j2):
	if int(j1['submission_date']) < int(j2['submission_date']):
		return -1
	elif int(j1['submission_date']) > int(j2['submission_date']):
		return 1
	else:
		if j1['start_date'] < j2['start_date']:
			return -1
		elif j1['start_date'] > j2['start_date']:
			return 1
		else:
			return 0
	
# Vm comparator: by start_date, then by boot_time_prediction
def cmpvm(vm1, vm2):
	if int(vm1['start_date']) < int(vm2['start_date']):
		return -1
	elif int(vm1['start_date']) > int(vm2['start_date']):
		return 1
	else:
		if int(vm1['boot_time']) < int(vm2['boot_time']):
			return -1
		elif int(vm1['boot_time']) > int(vm2['boot_time']):
			return 1
		else:
			return 0


# Date of the first vm start
beginDate = 0


# Open the JSON
with open(args.json_file) as fp:
	results = json.load(fp)
	# If the json is a more recent version
	if 'nodes' in results:
		results = results['nodes']
results.sort(key=lambda vm:int(vm['start_date']))
beginDate = int(results[0]['start_date'])

# compute t=0

jobs = []
for vm in results:
	jobs += vm['jobs']
	# Get the first submission date of the jobs
first_submission_date = sorted(jobs, key=lambda j: j['submission_date'])[0]['submission_date']

results.sort(cmp=cmpvm)
for vm in results:
	print("= VM {0}\t{1} {2}\t{3} ({4}) =".format(
		vm['instance_id'], 
		vm['start_date']-first_submission_date, 
		vm['stop_date']-first_submission_date,
		vm['boot_time'], vm['boot_time_prediction']))

	lastDate = vm['boot_time']+vm['start_date']-first_submission_date

	jobs = vm['jobs']

	# Sort it all. WARNING: Only based on job ids
	jobs.sort(cmp=cmpjob)
	# jobs.sort(key=lambda j: j['id'])

	# Associate a job name with its ID
	jobs_name_dict = {} 
	for job in jobs:
		if not 'name' in job:
			job['name'] = job['id']
		jobs_name_dict[job['name']] = job['id']

	predictedIdleDate = vm['boot_time_prediction']

	for job in jobs:
		predictedIdleDate = predictedIdleDate + job['walltime_prediction']
		print("{0}\t{1:.0f}\t{2:3.1f}\t{3:7.1f}\t{4:7.1f} -\t{5:5.1f}\t({6:5.0f}\t{7:5.0f})".format(job['id'], 
			job['submission_date'] - first_submission_date, 
			job['start_date'] - first_submission_date - lastDate,
			job['start_date'] - first_submission_date, 
			job['start_date'] - first_submission_date + job['walltime'],
			job['walltime'],
			job['walltime_prediction'],
			predictedIdleDate )),
		lastDate = job['start_date'] - first_submission_date + job['walltime']

		# if 'dependencies' in job and len(job['dependencies']) > 0:
		#	print(" ->"),
		#	for dependency_name in job['dependencies']:
		#		print(" {0}".format(jobs_name_dict[dependency_name])),
		print("")

