#!/usr/bin/env python3
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
import datetime
import collections


def usage():
	print >> sys.stderr, 'Usage: {0}'.format(sys.argv[0])

parser = argparse.ArgumentParser(description='Collection of tools to exploit schlouder traces.')
parser.add_argument('json_file', help='input json file')
parser.add_argument('-u', dest='update', action='store_const', const=True, default=False, 
	help='update the data to the last version (for old json version)')
parser.add_argument('-s', dest='add_states', action='store_const', const=True, default=False, 
	help='add the states of nodes')
parser.add_argument('-t', dest='to_tasks', action='store_const', const=True, default=False, 
	help='output the tasks in the simschlouder format')

args = parser.parse_args()


# Application/usecase dicts
applications = {}
applications['1x1'] = "montage"
applications['2x2'] = "montage"
applications['3x3'] = "montage"
applications['4x4'] = "montage"
applications['brt'] = "omssa"
applications['brs'] = "omssa"
applications['hrt'] = "omssa"
applications['hrs'] = "omssa"

# Montage task types
montage_task_types = ['project', 'overlaps', 'diff', 'bgmodel', 'background', 'add', 'gather']


# Return the index of the first job of the node, to reorder nodes
def node_to_sortableindex(node):
	return min(job['index'] for job in node['jobs'])


# Build job names that are comparable to reorder jobs
def job_to_sortablename(job):

	if job['name'].split('_')[0] == '2mass' :
		# montage h j k
		ml = str(job['name'].split('_')[2])
		if ml not in 'hjk':
			ml = 'z'
		# montage task type
		try:
			mtt = str(montage_task_types.index(job['name'].split('_')[4]))
		except IndexError or ValueError: # for gather. 
			mtt = '7'
		# montage
		try:	
			mnum = '{0:09}'.format(int(job['name'].split('_')[5]))
		except IndexError or ValueError: # for gather. 
			mnum = ''
		#job['namo'] = ml+'.'+mtt+'.'+mnum+'.'+job['name']
		return ml+'.'+mtt+'.'+mnum+'.'+job['name']
	
	else :
		return job['name'].replace('+','_')

# Update the results, whatever the version of the input file is
def update(results, json_file):

	json_file = os.path.basename(json_file)

	scheduled_dates_status = "logged"
	index_status = "logged"
	scheduled_date = 0

	# Fix missing 'info' section
	if 'nodes' in results:
		nodes = results['nodes']
	else:
		nodes = results
		results = {}
		results['nodes'] = results
		results['info'] = {}
		results['info']['version'] = "1"

	# Detection of the application and the platform
	uc = json_file.split('.')[0]
	try:
		results['info']['application'] = applications[uc]
		results['info']['uc'] = uc
		results['info']['platform'] = json_file.split('.')[3]
	except KeyError:
		results['info']['application'] = 'unknown'
		results['info']['uc'] = 'unknown'
		results['info']['platform'] = 'unknown'
	
	# Gathering and sorting jobs 
	jobs = []
	for node in nodes:
		jobs += node['jobs']
	try:
		jobs.sort(key=lambda job:job['index'])
	except KeyError:
		jobs.sort(key=job_to_sortablename)

	# Get the first submission date of the jobs
	first_submission_date = jobs[0]['submission_date']

	# Preprocessing the missing scheduled_date
	current_min_scheduled_date = first_submission_date
	future_min_scheduled_date = nodes[0]['start_date']
	min_scheduled_date = {}
	for node in nodes:
		if node['start_date'] > future_min_scheduled_date:
			current_min_scheduled_date = future_min_scheduled_date
			future_min_scheduled_date = node['start_date']
	#	print ("{0} {1} {2}".format(
	#		node['instance_id'], current_min_scheduled_date, future_min_scheduled_date))
		for job in node['jobs']:
			min_scheduled_date[job['id']] = current_min_scheduled_date
			if job['submission_date'] == 0:
				min_scheduled_date[job['id']] += 1	

	# Updating the jobs
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

		
	# Update the nodes
	index = 0
	nodes = sorted(nodes, key = lambda node:node['jobs'][0]['index'])
	for node in nodes:

		# Fix missing host
		if not 'host' in node:
			node['host'] = node['instance_id']	

		node['start_date'] -= first_submission_date
		if node['stop_date'] is not None and node['stop_date'] != -1:
			node['stop_date'] -= first_submission_date
		else:
			last_job = sorted(node['jobs'], key=lambda job:job['index'])[-1]
			last_idle_date = last_job['start_date'] + last_job['walltime']
			stop_date=node['start_date']+(((last_idle_date-node['start_date'])/3600)+1)*3600
		
		if 'index' not in node:
			node['index'] = index
			index += 1


	# detection of the boottime prediction regression
	btp0=nodes[0]['boot_time_prediction']

	# Update the general infos
	results['info']['updated'] = True
	results['info']['first_date'] = first_submission_date
	results['info']['scheduled_dates'] = scheduled_dates_status
	results['info']['index'] = index_status
	results['info']['boottime_prediction_0'] = btp0


# Add the states to the nodes, mainly for plotting purpose
def add_states(results, with_com=False):
	
	for node in results['nodes']:

		state = collections.OrderedDict()
		state_prediction = collections.OrderedDict()

		node_start_date = node['start_date']
		state[node_start_date]='pending'
		state_prediction[node_start_date]='pending'

		boot_date = node_start_date + node['boot_time']
		state[boot_date]='idle'

		boot_date_prediction = node_start_date + node['boot_time_prediction']
		state_prediction[boot_date_prediction]='idle'
		
		old_idle_date=boot_date
		odl_idle_date_prediction=boot_date_prediction
		for job in node['jobs']:

			#scheduled_date = job['scheduled_date']
			if job['start_date'] > old_idle_date :
				start_date = job['start_date']
			else:
				start_date = old_idle_date
			state[start_date]='busy'

			walltime_prediction = job['walltime_prediction']
			walltime = job['walltime']
			
			# if the job is in error
			if walltime == -1 : walltime = 0.1

			try:
				input_time = job['input_time']
				output_time = job['output_time']				
				if (with_com):
					state[start_date]='inputting'
					state[start_date+input_time]='computing'
					state[start_date+walltime-output_time]='outputting'
			except KeyError:
				pass

			state[start_date+walltime]='idle'


			state_prediction[odl_idle_date_prediction]='busy'
			state_prediction[odl_idle_date_prediction+walltime_prediction]='idle'

			old_idle_date=start_date+walltime
			odl_idle_date_prediction=odl_idle_date_prediction+walltime_prediction
		
		stop_date = node['stop_date']
		state[stop_date]='terminating'
		stop_date_prediction=(((odl_idle_date_prediction/3600)+1)*3600)
		state_prediction[stop_date_prediction]='terminating'

		node['state'] = state
		node['state_prediction'] = state_prediction


# Output the tasks in the simschlouder format
def print_tasks(results):
	# Gathering and sorting jobs 
	jobs = []
	for node in results['nodes']:
		jobs += node['jobs']
	try:
		jobs.sort(key=lambda job:job['index'])
	except KeyError:
		jobs.sort(key=job_to_sortablename)

	for job in jobs:
		print("{0} {1}".format(job['index'], job['name']))


# Open the JSON
with open(args.json_file) as fp:
	results = json.load(fp, object_pairs_hook=collections.OrderedDict)

if (args.update):
	update(results, args.json_file)
if (args.add_states):
	add_states(results)

if (args.to_tasks):
	print_tasks(results)
else:
	print(json.dumps(results, indent=2, separators=(',', ': ')))

