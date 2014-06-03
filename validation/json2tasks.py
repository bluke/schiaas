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
	print >> sys.stderr, 'Usage: {0} [-psm] json_file bot_file'.format(sys.argv[0])

parser = argparse.ArgumentParser(description='Convert Schlouder json file to SimSchlouder tasks file.')
parser.add_argument('lod', choices=['wpo', 'wto', 'rio', 'psm'],
	help='level of details: wpo=walltime prediction only, wto=walltime only, rio=runtime input output, psm=psm data')
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
		if int(vm1['boot_time_prediction']) < int(vm2['boot_time_prediction']):
			return -1
		elif int(vm1['boot_time_prediction']) > int(vm2['boot_time_prediction']):
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
results.sort(cmp=cmpvm)
beginDate = int(results[0]['start_date'])

# Output the boot_times
if args.lod == 'wto':
	btps = []
	bts = []
	print "[boots]"
	for vm in results:
		btps.append(vm['boot_time_prediction'])
		bts.append(vm['boot_time'])
	bts.sort()
	i = 0
	for btp in btps:
		print("{0}\t{1}".format(btp, bts[i]))
		i = i+1
	print "[tasks]"

# Fill an unique list of jobs
jobs = []
for vm in results:
	jobs += vm['jobs']

# Sort it all. 
jobs.sort(cmp=cmpjob)
# jobs.sort(key=lambda j: j['id'])

# Associate a job name with its ID
jobs_name_dict = {} 
for job in jobs:
	jobs_name_dict[job['name']] = job['id']

# Get the first submission date of the jobs
first_submission_date = sorted(jobs, key=lambda j: j['submission_date'])[0]['submission_date']

for job in jobs:
	print("{0}\t{1}\t{2}".format(job['id'], job['submission_date'] - first_submission_date, job['walltime_prediction'])),
	runtime = input_size = output_size = None

	if args.lod == 'psm' and 'PSM_data' in job and 'runtime_prediction' in job['PSM_data'] and job['PSM_data']['runtime_prediction'] is not None:
		runtime = job['PSM_data']['runtime_prediction']
		if 'data_in' in job['PSM_data'] and job['PSM_data']['data_in'] is not None:
			# Convert bytes into MBytes
			input_size = float(job['PSM_data']['data_in']) / 1e6
		else:
			input_size = 0
		if 'data_out' in job['PSM_data'] and job['PSM_data']['data_out'] is not None:
			# Convert bytes into MBytes
			output_size = float(job['PSM_data']['data_out']) / 1e6
		else:
			output_size = 0

	elif args.lod == 'rio':
		runtime = job['runtime']
		input_size = job['input_size'] / 1e6
		output_size = job['output_size'] / 1e6

	elif args.lod == 'wto':
		runtime = job['walltime']

	if runtime is not None:
		if runtime <= 0.0:
			runtime = 0.0
		print(" ~ {0}".format(runtime)),
	if input_size is not None and output_size is not None:
		print("{0}\t{1}".format(input_size, output_size)),

	if 'dependencies' in job and len(job['dependencies']) > 0:
		print("\t->"),
		for dependency_name in job['dependencies']:
			print(" {0}".format(jobs_name_dict[dependency_name])),
	print("")

