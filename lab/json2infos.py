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
parser.add_argument('--name', action='store_const', const=True, default=False, help='print full job name')
parser.add_argument('json_file', help='input json file')
parser.add_argument('job_to_track', nargs="?", default=-1, help='the job to track')

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


lastDate = 0

def print_job(vm, job):
	jname = ordered_job_names.index(job['name'])
	if args.name == True:
		jname = job['name']
	print("{0}\t{1:4.0f} {2:5.1f}\t{3:7.1f}\t{4:7.1f} - {5:5.1f}({6:.0f})\t{7:5.0f}\t{8:6.0f}{9:6.0f}\t{10:6.0f}{11:6.0f}".format(
		jname, 
		job['submission_date'] - first_submission_date, # sub date
		job['start_date'] - lastDate, #gap
		job['start_date'] - first_submission_date, #start
		job['start_date'] + job['walltime']- first_submission_date, #end
		job['walltime'], 
		job['standard_walltime'],
		job['walltime_prediction'],
		predictedIdleDate,
		vm['start_date']+predictedIdleDate - first_submission_date,
		predictedIdleDate - vm['boot_time_prediction'],
		vm['start_date']+predictedIdleDate - vm['boot_time_prediction'] - first_submission_date))

# Open the JSON
with open(args.json_file) as fp:
	results = json.load(fp)
	# If the json is a more recent version
	if 'nodes' in results:
		results = results['nodes']
results.sort(key=lambda vm:int(vm['start_date']))

# compute t=0

jobs = []
for vm in results:
	if not 'host' in vm:
		vm['host'] = vm['instance_id']
	jobs += vm['jobs']
	# Get the first submission date of the jobs
first_submission_date = sorted(jobs, key=lambda j: j['submission_date'])[0]['submission_date']

jobs.sort(cmpjob)
jobs_dict = {}
for job in jobs:
	if not 'name' in job:
		job['name'] = job['id']
	if not 'standard_walltime' in job:
		job['standard_walltime'] = job['walltime']
	ordered_job_names.append(job['name'])
	
	jobs_dict[job['name']] = job
	if 'dependencies' in job:
		for dep in job['dependencies']:
			if 'scheduling_date' not in job or job['scheduling_date'] < jobs_dict[dep]['start_date']+jobs_dict[dep]['walltime']:
				job['scheduling_date'] = jobs_dict[dep]['start_date']+jobs_dict[dep]['walltime']

ordered_job_names.sort(cmpjob_name)

#look for the job to track
job_to_track = None
job_to_track_index = len(jobs)+1
if args.job_to_track != -1:
	try:
		job_to_track_name = ordered_job_names[int(args.job_to_track)]
		job_to_track_index = int(args.job_to_track)
	except TypeError:
		job_to_track_index = ordered_job_names.index(args.job_to_track)
		job_to_track_name = args.job_to_track
	for job in jobs:
		if job['name'] == job_to_track_name:
			job_to_track = job
			break

#print ordered_job_names
results.sort(cmp=cmpvm)
for vm in results:
	print("= VM {0} / {1}\t{2} {3}\t{4} ({5}) =".format(
		vm['host'], 
		vm['instance_id'],
		vm['start_date']-first_submission_date, 
		vm['stop_date']-first_submission_date,
		vm['boot_time'], 
		vm['boot_time_prediction']))

	lastDate = vm['boot_time']+vm['start_date']

	jobs = vm['jobs']
	jobs.sort(cmp=cmpjob)

	predictedIdleDate = vm['boot_time_prediction']

	lastJob = None
	for job in jobs:
		if 'scheduling_date' in job:
			predictedIdleDate = vm['boot_time_prediction']
			#print (job['scheduling_date']-first_submission_date)
			for job2 in jobs:
				if job2 == job:
					break
				if job2['start_date']+job2['walltime'] <= job['scheduling_date']:
					predictedIdleDate = job2['start_date']-vm['start_date']+job2['walltime']
				else:
					predictedIdleDate += job2['walltime_prediction']
				#print ("+- {0}\t{1}".format(predictedIdleDate, job2['name']))

		#if job_to_track != None and job['start_date'] > job_to_track['start_date']:
		if job_to_track != None and ordered_job_names.index(job['name']) > job_to_track_index:
			break
		if ordered_job_names.index(job['name']) == job_to_track_index:
			print("+"),

		predictedIdleDate = predictedIdleDate + job['walltime_prediction']
		print_job(vm,job)

		lastDate = job['start_date'] + job['walltime']
		lastJob = job

		if ordered_job_names.index(job['name']) == job_to_track_index:
			print ""
			# job_to_track = None
			break

	if job_to_track != None and job != job_to_track :
		job = job_to_track
		predictedIdleDate = predictedIdleDate + job['walltime_prediction']
		print("-"),
		print_job(vm,job)
		print("")
