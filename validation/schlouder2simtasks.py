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
	if int(j1['submission_date']) == int(j2['submission_date']):
		return (j1['id'] > j2['id'])
	else:
		return (int(j1['submission_date']) > int(j2['submission_date']))

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
	#jobs.sort(cmp=cmpjob, reverse=True)
	jobs.sort(key=lambda j: j['id'])

	# Output the file with the number of concurrent jobs
	output=sys.argv[2]
	if (os.path.isfile(output)):
		os.unlink(output)
	sub_date = 0

	# Associate a job name with its ID
	jobs_name_dict = {} 
	for job in jobs:
		jobs_name_dict[job['name']] = job['id']

	with open(output, 'w') as fp:
		for job in jobs:
			fp.write("{0} {1} {2}".format(job['id'], sub_date, job['walltime_prediction']))
			if 'runtime' in job and job['runtime'] is not None:
				fp.write(" ~ {0} ".format(job['runtime']))
				if 'input_size' in job and job['input_size'] is not None:
					input_size = job['input_size']
				else:
					input_size = 0
				if 'output_size' in job and job['output_size'] is not None:
					output_size = job['output_size']
				else:
					output_size = 0
				fp.write("{0} {1}".format(input_size, output_size))
			if 'dependencies' in job and len(job['dependencies']) > 0:
				fp.write(" ->")
				for dependency_name in job['dependencies']:
					fp.write(" {0}".format(jobs_name_dict[dependency_name]))
			fp.write("\n")

