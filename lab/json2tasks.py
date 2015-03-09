#!/usr/bin/python -O
# -*- coding:utf8 -*-

##	This script takes a JSON output from Schlouder as input, and outputs
##	a tasks file in the simschlouder format.
##
##  The lod (level_of_details) argument can be:
##  - wpo: Walltime Prediction Only, to validate SimSchlouder as a prediction 
##  - wto: Walltime Only, to validate SimSchlouder without communications
##  - rio: Real Input Output, validate SimSchlouder with communications
##  - psm: Provisioning Simulation Module, validate SimSchlouder as a prediction with communictions
##
##  @author julien.gossa@unistra.fr


import sys
import os
import json
import argparse


def usage():
	print >> sys.stderr, 'Usage: {0} level_of_details json_file bot_file'.format(sys.argv[0])

parser = argparse.ArgumentParser(description='Convert Schlouder json file to SimSchlouder tasks file.')
parser.add_argument('lod', choices=['wpo', 'wto', 'rio', 'psm'],
	help='level of details: wpo=walltime prediction only, wto=walltime only, rio=runtime input output, psm=psm data')
parser.add_argument('json_file', help='input json file')

args = parser.parse_args()

def bootTimePredictionFix(btp):
	n=int((btp-48)/6)
	return btp+0.5-n*0.02


# Job comparator
def cmpjob(j1, j2):
	if j1['scheduled_date'] != j2['scheduled_date']:
		return int(j1['scheduled_date'] - j2['scheduled_date'])
	return int(j1['index'] - j2['index'])


# Open the JSON
with open(args.json_file) as fp:
	fullresults = json.load(fp)
	results = fullresults['nodes']

jobs = []
for vm in results:
	jobs += vm['jobs']

# Normal sort
#jobs.sort(key=lambda job:int(job['index']))

# Sort to imitate the schlouder threads
jobs.sort(cmpjob)


# Output the validation data
if args.lod == 'wto':

	# Output the boot_times
	print "[boots]"

	# messy patch to add more btp. DOES NO WORK WITH ONLY ONE VM.
	old_bt = 0
	old_btp = 0
	for vm in results:
		first_start_date = sorted(vm['jobs'], key=lambda j: j['start_date'])[0]['start_date']
		first_sub_date = sorted(vm['jobs'], key=lambda j: j['submission_date'])[0]['submission_date']
		
		print("{0}\t{1}\t{2}".format(
			vm['boot_time'],
			vm['start_date'],
			first_start_date-vm['start_date']-vm['boot_time']))

	print "[tasks]"

submission_date = 0
for job in jobs:

	# True to imitate schlouder threads
	if True or 'dependencies' not in job or len(job['dependencies']) == 0:
		submission_date = job['scheduled_date']

	print("{0}\t{1}\t{2}".format(
		job['name'], 
		submission_date, 
		job['walltime_prediction'])),
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
			# print(" {0}".format(jobs_name_dict[dependency_name])),
			print(" {0}".format(dependency_name)),
	print("")

