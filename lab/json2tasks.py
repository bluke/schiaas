#!/usr/bin/python -O
# -*- coding:utf8 -*-

"""
	This script takes a JSON output from Schlouder as input, and output 
	a tasks file in the simschlouder format
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


ordered_job_names = []

#  is montage?
if 'pleiade' in args.json_file: 
	is_montage = True
else:
	is_montage = False

# Job comparator: by sub_date, then by start_date
def cmpjob_date(j1, j2):
	if int(j1['submission_date']) < int(j2['submission_date']):
		return -1
	elif int(j1['submission_date']) > int(j2['submission_date']):
		return 1
	else:
		if j1['start_dte'] < j2['start_date']:
			return -1
		elif j1['start_date'] > j2['start_date']:
			return 1
		else:
			return 0
	
# Job comparator: by name, for montage
task_types = ['project', 'overlaps', 'diff', 'bgmodel', 'background', 'add', 'gather']
def cmpjob_name(j1, j2):
	# if OMSSA
	if not is_montage :
		if j1['name'] == j2['name'] : return 0
		if j1['name'].replace("_","+") < j2['name'].replace("_","+") : return -1
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


# Vm comparator
def cmpvm(vm1, vm2):
	first_job1 = sorted(vm1['jobs'], key=lambda j: j['start_date'])[0]
	first_job2 = sorted(vm2['jobs'], key=lambda j: j['start_date'])[0]

	if vm1['start_date'] != vm2['start_date']:
		return int(vm1['start_date'] - vm2['start_date'])

	if first_job1['start_date'] != first_job2['start_date']:
		return int(first_job1['start_date']) - int(first_job2['start_date'])

	return (ordered_job_names.index(first_job1['name']) - ordered_job_names.index(first_job2['name']) )


# Open the JSON
with open(args.json_file) as fp:
	results = json.load(fp)
	# If the json is a more recent version
	if 'nodes' in results:
		results = results['nodes']
# Fill an unique list of jobs
jobs = []
for vm in results:
	#vm['jobs'].sort(cmp=cmpjob_name)
	for job in vm['jobs']:
		job['vm_start_date'] = vm['start_date']
	jobs += vm['jobs']

# Sort it all. 
jobs.sort(cmp=cmpjob_name)
# jobs.sort(key=lambda j: j['id'])

# Associate a job name with its ID
jobs_name_dict = {} 
for job in jobs:
	jobs_name_dict[job['name']] = job['id']
	ordered_job_names.append(job['name'])

results.sort(cmp=cmpvm)


# Get the first submission date of the jobs
first_submission_date = sorted(jobs, key=lambda j: j['submission_date'])[0]['submission_date']


# Output the validation data
if args.lod == 'wto':

	# Output the provisioning thread dates
	print "[provisioning_dates]"
	last_prov_date = 0
	for vm in results:
		if last_prov_date < vm['start_date'] and vm['start_date'] < first_submission_date + 300:
			last_prov_date = vm['start_date']
			print(vm['start_date'] - first_submission_date)

	# Output the boot_times
	print "[boots]"
#	btps = []
#	bts = []

	# messy patch to add more btp. DOES NO WORK WITH ONLY ONE VM.
	old_bt = 0
	old_btp = 0
	for vm in results:
#		btps.append(vm['boot_time_prediction'])
		#bts.append(vm['boot_time'])
		first_start_date = sorted(vm['jobs'], key=lambda j: j['start_date'])[0]['start_date']
		first_sub_date = sorted(vm['jobs'], key=lambda j: j['submission_date'])[0]['submission_date']
#		bts.append(first_start_date-vm['start_date'])
		
		print("{0}\t{1}\t{2}".format(
			vm['boot_time_prediction'], 
			first_start_date-vm['start_date'],
			first_sub_date-first_submission_date))

		d_btp = vm['boot_time_prediction'] - old_btp
		d_bt = first_start_date-vm['start_date'] - old_bt
		old_btp = vm['boot_time_prediction']
		old_bt = first_start_date-vm['start_date']

	print("{0}\t{1}".format(
		old_btp + d_btp, 
		old_bt + d_bt ))


#	bts.sort()
#	i = 0
#	for btp in btps:
#		print("{0}\t{1}".format(btp, bts[i]))
#		i = i+1

	print "[tasks]"

last_prov_date = jobs[0]['submission_date']
for job in jobs:
	#patch to fix provisioning threads dates
	if job['submission_date'] != jobs[0]['submission_date']	or last_prov_date != jobs[0]['submission_date']:
		if job['vm_start_date'] - first_submission_date < 300 and last_prov_date < job['vm_start_date']-1:
			last_prov_date = job['vm_start_date']-1
		job['submission_date'] = last_prov_date
	
	print("{0}\t{1}\t{2}".format(
		job['name'], 
		job['submission_date'] - first_submission_date, 
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

