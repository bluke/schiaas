#!/usr/bin/python -O
# -*- coding:utf8 -*-

"""
	This script take a JSON output from Schlouder as input, and output 
	a tikz representing the different BTUs

"""

import sys
import os
import json
import math


def usage():
	print >> sys.stderr, 'Usage: {0} json_file out_prefix'.format(sys.argv[0])

# Date of the first vm start
beginDate = 0

# Statistics
makespan=0
BTUCount=0
VMCount=0

# Layouts	
btuH = 0.2  # Height of btu/vm
btuM = 0.5    # Margin between 2 btu/vm
jobM = 0.0  # Up and down margin of jobs inside BTU

# Time to Point
def t2p(s):
	return (float(s)/360.0)
# Date to Point
def d2p(d):
	return t2p(d-beginDate)



if __name__ == '__main__':
	if len(sys.argv) != 3:
		usage()
		sys.exit(-1)

	# Open the JSON
	with open(sys.argv[1]) as fp:
		results = json.load(fp)

		# Output description
		
		output=sys.argv[2] + ".info"
		if (os.path.isfile(output)):
			os.unlink(output)
		with open(output, 'w') as fp:
			if hasattr(results, 'info') and hasattr(results['info'], 'description'):
				fp.write("{0}".format(results['info']['description']))
			else :
				fp.write("no description")

		# If the json is a more recent version
		if 'nodes' in results:
			results = results['nodes']

	# Sort vms
	results.sort(key=lambda vm:int(vm['start_date'])+int(vm['boot_time']))
	beginDate = int(results[0]['start_date'])


	# Output the file with the number of concurrent jobs
	output=sys.argv[2] + ".tikz"
	if (os.path.isfile(output)):
		os.unlink(output)
	with open(output, 'w') as fp:
		#fp.write("\\begin{tikzpicture}")

#		for d in diametersJobs:
#			fp.write("{0}\t{1}\n".format(d[0]-beginDate, d[1]))

		#stat
		VMCount=len(results)


		iv = 0
		for vm in results:

			# Correct null stop_dates
			if vm['stop_date'] == None:
			   # Get the last job execution date
			   last_job = sorted(vm['jobs'], key=lambda j: int(j['start_date']) + int(j['walltime']))[-1]
			   end_date = int(last_job['start_date']) + int(last_job['walltime'])
			   vm['stop_date'] = int(vm['start_date'] + math.ceil((end_date - vm['start_date']) / 3600.0) * 3600)

			(start_date, boot_time_prediction, boot_time, stop_date) = (d2p(int(vm['start_date'])), t2p(int(vm['boot_time_prediction'])), t2p(int(vm['boot_time'])), d2p(int(vm['stop_date'])))

			#Not accurate
			if (stop_date == d2p(0)):
				stop_date = start_date+t2p(3600);

			fp.write("\n%%%%%%%%%%%%%%%%%%% VM {0}\n".format(iv))
			dY=iv*(btuH+btuM)

			#stat
			btu = int((int(vm['stop_date'])-int(vm['start_date']))/3600)+1
			BTUCount += btu


			#boottime
			fp.write("\\filldraw[draw=black,fill=lightgray,very thick] ({0},{1}) rectangle ({2},{3});\n"
			.format(start_date,dY,start_date+boot_time,dY+btuH))

			#btu
			fp.write("\\filldraw[draw=black,fill=white, very thick] ({0},{1}) rectangle ({2},{3});\n"
			.format(start_date+boot_time,dY,stop_date,dY+btuH))

			#jobs
			jobs = vm['jobs']			
			jobs.sort(key=lambda j: int(j['submission_date']), reverse=True)


			for job in jobs:

				#stat
				if (makespan<job['start_date']+job['walltime']) :
					makespan=job['start_date']+job['walltime']

				#conversion to points
				(jwalltime,jwalltime_prediction,jstart_date,jid,jsubmission_date) = (t2p(job['walltime']), t2p(job['walltime_prediction']), d2p(job['start_date']), job['id'], d2p(job['submission_date']))
				fp.write("%%%% JOB {0}\n".format(jid))


				# real job
				fp.write("\\filldraw[draw=black,fill=cyan, very thin] ({0},{1}) rectangle ({2},{3});\n"
				.format(jstart_date,dY+jobM,jstart_date+jwalltime,dY+btuH-jobM))



			for job in jobs:
				(jidn, jsubmission_date, jstart_date,\
					jwalltime_prediction, jwalltime) \
				= (job['id'], d2p(job['submission_date']), d2p(job['start_date']), \
					t2p(job['walltime_prediction']), t2p(job['walltime']) )

				if hasattr(job,'runtime'):
					(jruntime, jinput_time, joutput_time, jmanagement_time) \
					= (t2p(job['runtime']), t2p(job['input_time']), t2p(job['output_time']), t2p(job['management_time']))
				else:
					(jruntime, jinput_time, joutput_time, jmanagement_time) \
					= (t2p(job['walltime']), 0,0,0)

				# sub date
				# nicer, but more limited 
				fp.write("\\draw[->,color=red,>=latex,very thin] ({0},{1}) -- ({4},{5}) .. controls ({6},{5}) .. ({2},{3});\n"
				.format(jsubmission_date,dY, jstart_date,dY+jobM, jsubmission_date,dY-btuM/2.0,(2*jstart_date+jsubmission_date)/3.0))



			for job in jobs:
				(jidn, jsubmission_date, jstart_date,\
					jwalltime_prediction, jwalltime) \
				= (job['id'], d2p(job['submission_date']), d2p(job['start_date']), \
					t2p(job['walltime_prediction']), t2p(job['walltime']) )

				if hasattr(job,'runtime'):
					(jruntime, jinput_time, joutput_time, jmanagement_time) \
					= (t2p(job['runtime']), t2p(job['input_time']), t2p(job['output_time']), t2p(job['management_time']))
				else:
					(jruntime, jinput_time, joutput_time, jmanagement_time) \
					= (t2p(job['walltime']), 0,0,0)

				# forecasted walltime_prediction
				#fp.write("\\filldraw[draw=black,fill=green,very thin] ({0},{1}) rectangle ({2},{3});\n"
				#.format(jstart_date+jwalltime,dY+jobM,jstart_date+jwalltime_prediction,dY+btuH/2.0))

				# input time
				fp.write("\\filldraw[draw=black,fill=red,very thin] ({0},{1}) rectangle ({2},{3});\n"
				.format(jstart_date,dY+jobM,jstart_date+jinput_time,dY+btuH/2.0))

				# run time
				fp.write("\\filldraw[draw=black,fill=green,very thin] ({0},{1}) rectangle ({2},{3});\n"
				.format(jstart_date+jinput_time,dY+jobM,jstart_date+jinput_time+jruntime,dY+btuH/2.0))

				# output time
				fp.write("\\filldraw[draw=black,fill=orange,very thin] ({0},{1}) rectangle ({2},{3});\n"
				.format(jstart_date+jinput_time+jruntime,dY+jobM,jstart_date+jinput_time+jruntime+joutput_time,dY+btuH/2.0))

			iv+=1

			fp.write("\\filldraw[draw=black,fill=yellow,very thin] ({0},{1}) rectangle ({2},{3});\n"
			.format(start_date+boot_time_prediction,dY,start_date+boot_time,dY+btuH/2))
		#fp.write("\\end{tikzpicture}")

		
	#Output stats
	makespan-=beginDate
	output=sys.argv[2] + ".stats"
	if (os.path.isfile(output)):
		os.unlink(output)
	with open(output, 'w') as fp:
		fp.write("{0} & {1} & {2}".format(VMCount, BTUCount, int(makespan)))

	print("{0}\t{1}\t{2}".format(VMCount, BTUCount, int(makespan))),

	#Output dimensions
	output=sys.argv[2] + ".latexdim"
	if (os.path.isfile(output)):
		os.unlink(output)
	with open(output, 'w') as fp:
		fp.write("\\setlength{{\\btutikzwidth}}{{{0}cm}}\n".format(t2p((int(makespan/3600)+1)*3600)))
		fp.write("\\setlength{{\\btutikzheight}}{{{0}cm}}\n".format(VMCount*(btuH+btuM)))



