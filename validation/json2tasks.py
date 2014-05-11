#!/usr/bin/python -O
# -*- coding:utf8 -*-

"""
	This script take a JSON output from Schlouder as input, and output 
	a bot file in the simschlouder format
"""

import sys
import os
import json


def usage():
	print >> sys.stderr, 'Usage: {0} json_file bot_file'.format(sys.argv[0])

# Job comparator: by sub_date, then by id
def cmpjob(j1, j2):
	if int(j1['submission_date']) < int(j2['submission_date']):
		return -1
	elif int(j1['submission_date']) > int(j2['submission_date']):
		return 1
	else:
		if j1['id'] < j2['id']:
			return -1
		elif j1['id'] > j2['id']:
			return 1
		else:
			return 0
	

# Date of the first vm start
beginDate = 0

if __name__ == '__main__':
	if len(sys.argv) != 3:
		usage()
		sys.exit(-1)

	# Open the JSON
	with open(sys.argv[1]) as fp:
		results = json.load(fp)
		# If the json is a more recent version
		if 'nodes' in results:
			results = results['nodes']
	results.sort(key=lambda vm:int(vm['start_date']))
	beginDate = int(results[0]['start_date'])

	# Fill an unique list of jobs
	jobs = []
	output=sys.argv[2]
	if (os.path.isfile(output)):
		os.unlink(output)
	for vm in results:
		jobs += vm['jobs']

	# Sort it all. WARNING: Only based on job ids
	jobs.sort(cmp=cmpjob)
	# jobs.sort(key=lambda j: j['id'])

	# Output the file with the number of concurrent jobs
	output=sys.argv[2]
	if (os.path.isfile(output)):
		os.unlink(output)

	# Associate a job name with its ID
	jobs_name_dict = {} 
	for job in jobs:
		jobs_name_dict[job['name']] = job['id']

	# Get the first submission date of the jobs
	first_submission_date = sorted(jobs, key=lambda j: j['submission_date'])[0]['submission_date']

	# Does the runtime comes from PSM_data or from the real output?
	use_psm_data = True

	if use_psm_data:
		walltime_key = 'walltime_prediction'
	else:
		walltime_key = 'walltime'

	with open(output, 'w') as fp:
		for job in jobs:
			fp.write("{0} {1} {2}".format(job['id'], job['submission_date'] - first_submission_date, job[walltime_key]))
			runtime = input_size = output_size = None
			if use_psm_data and 'PSM_data' in job and 'runtime_prediction' in job['PSM_data'] and job['PSM_data']['runtime_prediction'] is not None:
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
			elif not use_psm_data:
				runtime = job['runtime']
				input_size = job['input_size'] / 1e6
				output_size = job['output_size'] / 1e6
			if runtime is not None:
				fp.write(" ~ {0} {1} {2}".format(runtime, input_size, output_size))

			if 'dependencies' in job and len(job['dependencies']) > 0:
				fp.write(" ->")
				for dependency_name in job['dependencies']:
					fp.write(" {0}".format(jobs_name_dict[dependency_name]))
			fp.write("\n")

