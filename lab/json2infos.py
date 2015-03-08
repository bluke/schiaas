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
parser.add_argument('--queue', action='store_const', const=True, default=False, help='print full queues times')
parser.add_argument('json_file', help='input json file')
parser.add_argument('job_to_track', nargs="?", default=-1, help='the job to track')

args = parser.parse_args()

# Job comparator
def cmpjob(j1, j2):
	if j1['scheduled_date'] != j2['scheduled_date']:
		return int(j1['scheduled_date'] - j2['scheduled_date'])
	return int(j1['index'] - j2['index'])


lastDate = 0

def print_job(vm, job):
	jname = job['index']
	if args.name == True:
		jname = str(job['index'])+":"+job['name']
	print("{0:3}\t{1:4.0f} {2:4.0f} {3:5.1f}\t{4:7.1f}\t{5:7.1f} - {6:5.1f}({7:.0f})\t{8:5.0f}".format(
		jname, 
		job['submission_date'],
		job['scheduled_date'],
		job['start_date'] - lastDate, #gap
		job['start_date'],
		job['start_date'] + job['walltime'], #end
		job['walltime'], 
		job['standard_walltime'],
		job['walltime_prediction'])),

	if args.queue == True:
		print("\t{0:6.0f}{1:6.0f}\t{2:6.0f}{3:6.0f}".format(
			predictedIdleDate,
			vm['start_date']+predictedIdleDate,
			predictedIdleDate - vm['boot_time_prediction'],
			vm['start_date']+predictedIdleDate - vm['boot_time_prediction'])),
	print ""

# Open the JSON
with open(args.json_file) as fp:
	fullresults = json.load(fp)
	results = fullresults['nodes']

jobs = []
for vm in results:
	jobs += vm['jobs']
jobs.sort(key=lambda job:int(job['index']))
	
#look for the job to track
job_to_track = None
if args.job_to_track != -1:
	try:
		job_to_track = jobs[int(args.job_to_track)]
	except ValueError:
		for job in jobs:
			if job['name'] == args.job_to_track:
				job_to_track = job
				break

#print ordered_job_names
results.sort(key=lambda vm:int(vm['index']))
for vm in results:
	print("= VM {0} / {1}\t{2} {3}\t{4} ({5}) =".format(
		vm['host'], 
		vm['instance_id'],
		vm['start_date'], 
		vm['stop_date'],
		vm['boot_time'], 
		vm['boot_time_prediction']))

	lastDate = vm['boot_time']+vm['start_date']

	jobs = vm['jobs']
	jobs.sort(key=lambda job:int(job['start_date']))

	lastJob = None
	for job in jobs:
		predictedIdleDate = vm['boot_time_prediction']
		#print (job['scheduling_date']-first_submission_date)
		for job2 in jobs:
			if job2 == job:
				break
			if job2['start_date']+job2['walltime'] <= job['scheduled_date']:
				predictedIdleDate = job2['start_date']-vm['start_date']+job2['walltime']
			else:
				predictedIdleDate += job2['walltime_prediction']
			#print ("+- {0}\t{1}".format(predictedIdleDate, job2['name']))

		#if job_to_track != None and job['start_date'] > job_to_track['start_date']:
		if job_to_track != None and cmpjob(job,job_to_track) > 0:
			break
		if job == job_to_track:
			print("+"),

		predictedIdleDate = predictedIdleDate + job['walltime_prediction']
		print_job(vm,job)

		lastDate = job['start_date'] + job['walltime']
		lastJob = job

		if job == job_to_track:
			print ""
			# job_to_track = None
			break

	if job_to_track != None and job != job_to_track :
		job = job_to_track
		predictedIdleDate = predictedIdleDate + job['walltime_prediction']
		print("-"),
		print_job(vm,job)
		print("")
