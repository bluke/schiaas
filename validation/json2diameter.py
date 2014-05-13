#!/usr/bin/python -O
# -*- coding:utf8 -*-

"""
	This script take a JSON output from Schlouder as input and output 
	for each event (job start or end date) the number of concurrent jobs

"""

import sys
import os
import json
import math
from interval_tree import *

def unique(seq, keepstr=True):
	t = type(seq)
	if t==str:
		t = (list, ''.join)[bool(keepstr)]
	seen = []
	return t(c for c in seq if not (c in seen or seen.append(c)))

def _diameter(tree):
	diameters = []
	for i in tree:
		diameters.append((i.start, len(tree.find(Interval(i.start, i.start)))))
		diameters.append((i.stop, len(tree.find(Interval(i.stop, i.stop)))))
	return diameters

def diameter(tree):
	diameters = _diameter(tree)
	previous = None
	diameters.sort(key=lambda d:d[0])
	diameters = unique(diameters)
	return diameters

def usage():
	print >> sys.stderr, 'Usage: {0} json_file output_prefix'.format(sys.argv[0])

if __name__ == '__main__':
	if len(sys.argv) != 3:
		usage()
		sys.exit(-1)

	# Date of the first vm start
	beginDate = 0
	# List of all jobs
	jobs = []
	with open(sys.argv[1]) as fp:
		results = json.load(fp)
		# If the json is a more recent version
		if 'nodes' in results:
			results = results['nodes']
	results.sort(key=lambda vm:vm['start_date'])
	beginDate = int(results[0]['start_date'])

	intervalsVMs = []
	for vm in results:

		#Correct null stop_date
		if vm['stop_date'] == None:
		   # Get the last job execution date
		   last_job = sorted(vm['jobs'], key=lambda j: int(j['start_date']) + int(j['walltime']))[-1]
		   end_date = int(last_job['start_date']) + int(last_job['walltime'])
		   vm['stop_date'] = int(vm['start_date'] + math.ceil((end_date - vm['start_date']) / 3600.0) * 3600)


		(start_date, boot_time, stop_date) = (int(vm['start_date']), int(vm['boot_time']), int(vm['stop_date']))
	
		# If the stop date is not correct (dos not work if BTU>1)
		if stop_date == 0:
			stop_date=start_date+3600

		intervalsVMs.append(Interval(start_date+boot_time, stop_date))
		jobs += vm['jobs']
	jobs.sort(key=lambda j: j['submission_date'])

	nbJobs = len(jobs)
	if nbJobs == 0:
		print >> sys.stderr, 'No job in the given json'
		sys.exit(-1)

	intervalsJobs = []
	for job in jobs:
		(start_date, runtime) = (int(job['start_date']), int(job['walltime']))
		intervalsJobs.append(Interval(start_date, start_date + runtime ))
	
	diametersJobs = diameter(IntervalTree(intervalsJobs))
	# Output the file with the number of concurrent jobs
	output=sys.argv[2] + "-jobs.dat"
	if (os.path.isfile(output)):
		os.unlink(output)
	with open(output, 'w') as fp:
		fp.write("{0}\t{1}\n".format(0, 0))
		for d in diametersJobs:
			fp.write("{0}\t{1}\n".format(d[0]-beginDate, d[1]))
	
	diametersVMs = diameter(IntervalTree(intervalsVMs))
	output=sys.argv[2] + "-vms.dat"
	if (os.path.isfile(output)):
		os.unlink(output)
	with open(output, 'w') as fp:
		fp.write("{0}\t{1}\n".format(0, 0))
		for d in diametersVMs:
			fp.write("{0}\t{1}\n".format(d[0]-beginDate, d[1]))

