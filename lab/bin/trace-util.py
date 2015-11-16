#!/usr/bin/python3 -O
# -*- coding:utf8 -*-

##	This script gather tools to exploit simgrid traces
##  Three format are handled: paje, schiaas, and json
##
##  @author julien.gossa@unistra.fr
##
## TODO make grep events, and add prop, add interval
## TODO add info, to get the available 

import sys
import os
import json
import argparse
import collections
import re

def usage():
	print >> sys.stderr, 'Usage: src_trace_file command [option] [[command [option]...]'.format(sys.argv[0])

parser = argparse.ArgumentParser(description='Collection of tools to exploit schiaas traces.')
parser.add_argument('src_filename', help='trace file to exploit')
parser.add_argument('--grep', dest='dump_greps', metavar='pattern', nargs='+', 
	help='dump the traces regarding the entities matching the pattern')
parser.add_argument('--event', dest='dump_events', metavar='pattern', nargs='+', 
	help='dump the traces regarding the entities matching the pattern. For one given date, only the last value is kept')
parser.add_argument('--count-if', dest='count_if_args', metavar=('pattern cmp val'), nargs='+', 
	help='for each date, count the entities matching the pattern for which the comparison cmp is true between value and val. cmp is bash_like: be eq, ne, gt, ge, lt, le.')
parser.add_argument('--json', dest='json_dump', action='store_const', const=True, default=False, 
	help='dump the whole trace in the json format.')
parser.add_argument('--info', dest='info_dump', action='store_const', const=True, default=False, 
	help='print the properties and events available in the trace, in the json format.')
parser.add_argument('-r', dest='output_r', action='store_const', const=True, default=False, 
	help='write R script')
parser.add_argument('-p', dest='prefix', metavar=('prexif'), nargs=1, 
	help='add a prefix to file names and R data name')
parser.add_argument('-f', dest='output_to_file', action='store_const', const=True, default=False, 
	help='write the output to files')
parser.add_argument('-d', dest='output_dir', metavar=('directory'), nargs=1, 
	help='set the output directory')

args = parser.parse_args()

class Trace:
	entity_sep = ":"
	field_sep = "\t"

	def __init__(self, src_filename):
		self.traces = None
		self.root = None
		src_type = self.get_type(src_filename)
		if src_type == 'json':
			self.root = self.read_json(src_filename)
		elif src_type == 'schiaas':
			self.traces = self.read_schiaas(src_filename)
		else:
			self.root = self.read_paje(src_filename)

	def get_type(self, src_filename):
		with open(src_filename, errors='ignore') as src_file:
			first_line = src_file.readline().strip()
			first_word = first_line.split(self.field_sep)[0]
			if first_word == '{':
				return 'json'
			if first_word == 'root':
				return 'schiaas'
			return 'paje'

	def read_json(self, json_filename):
		with open(json_filename) as src_file:
			root = json.load(src_file)
		return root

	def read_schiaas(self, schiaas_filename):
		traces = []
		with open(schiaas_filename) as src_file:
			for log in src_file:
				traces.append(log.split(self.field_sep))
		return traces

	def build_obj_from_traces(self):
		root = collections.OrderedDict()
		for [entities, key, val] in self.traces:
			e = root
			val = val.strip()
			for entity in entities.split(self.entity_sep):
				if not entity in e:
					e[entity] = collections.OrderedDict()
				e = e[entity]
			if key in e:
				e[key] = e[key]+','+val
			else:
				e[key] = val
		return root

	# Portable, complete, but really messy:
	# information should be gathered by entity instead of event types.
	def read_paje(self, paje_filename):
		root = collections.OrderedDict()
		events = []
		with open(paje_filename, errors='ignore') as src_file:
			root['paje_ver'] = src_file.readline().strip()[1:]
			root['paje_command'] = src_file.readline()[1:].strip()

			line = src_file.readline()
			while line != "":
				
				if (line[0:9] == '%EventDef'):
					[e, etype, eindex] = line.split()
					event = [etype]
					line = src_file.readline().strip()
					while line != '%EndEventDef':
						event.append(line.split()[1])
						line = src_file.readline().strip()
					events.append(event)
				else:
					eline = line.strip().split()
					ei = int(eline[0])
					if events[ei][0] not in root:
						root[events[ei][0]] = []
					event = {}
					for ti in range(1,len(events[ei])):
						event[events[ei][ti]]= eline[ti].strip('"')
					root[events[ei][0]].append(event)

				line = src_file.readline()
		return root


	def count_if(self, pattern, test, testval, out_file):
		out_file.write("entity"+self.field_sep+"date"+self.field_sep+"value\n")
		try :
			testval = float(testval)
		except ValueError:
			pass

		in_set = set()

		last_date = 0
		count = 0
		last_count = 0
		last_printed_count = -1
		for [entities, date, val] in self.traces:
			try: 
				date = float(date)
			except ValueError:
				pass

			if re.search(pattern, entities) is not None:
				val=val.strip()
				try :
					val = float(val)
				except ValueError:
					pass
				#print('test ',val, testval, test(val,testval))
				if test(val,testval) :
					if entities not in in_set:
						in_set.add(entities)
						count = count+1
				else:
					if entities in in_set:
						in_set.remove(entities)
						count = count-1
				#print(entities,date,val,count)
				if last_date != date and  last_count != last_printed_count:
					#print(last_date,last_count)
					out_file.write(pattern+self.field_sep+str(last_date)+self.field_sep+str(last_count)+'\n')
					last_printed_count = last_count
				last_count = count
				last_date = date

		out_file.write(pattern+self.field_sep+str(last_date)+self.field_sep+str(last_count)+'\n')
		if (date != last_date):
			out_file.write(pattern+self.field_sep+str(date)+self.field_sep+str(count)+'\n')

		return 's'

	def grep(self, pattern, out_file):
		out_file.write("entity"+self.field_sep+"key"+self.field_sep+"value\n")
		for [entities, date, val] in self.traces:
			if re.search(pattern, entities) is not None:
				out_file.write(entities+self.field_sep+date+self.field_sep+val)

		return 'p'

	def grep_event(self, pattern, out_file):
		out_file.write("entity"+self.field_sep+"date"+self.field_sep+"value\n")
		last_date = 0.0
		last_val = {}
		for [entities, date, val] in self.traces:
			if re.search(pattern, entities) is not None:				
				if float(date) != last_date:
					for e,v in last_val.items():
						out_file.write(e+self.field_sep+str(last_date)+self.field_sep+v)
					last_val = {}
					last_date=float(date)
				last_val[entities] = val

		for e,v in last_val.items():
			out_file.write(e+self.field_sep+str(last_date)+self.field_sep+v)

		return 'p'	


	def get_info(self, out_file):
		if self.root is None:
			self.root = self.build_obj_from_traces()
		out_file.write(json.dumps(self.sub_get_info(self.root), 
			indent=2, separators=(',', ': ')))
		out_file.write('\n')

	def node_eq(self,node1,node2):
		try:
			for k,v in node1.items():
				if k not in node2:
					return False
		except AttributeError: # node is a leaf
			return node1 == node2

		return True


	def sub_get_info(self, node):
		#print("SDS: ",fields)

		info = collections.OrderedDict()
		subs = {}
		occurences = {}

		try: #event
			k = next(iter(node.keys()))
			float(k)
			#print("event")
			return 'event'
		except AttributeError: #property
			#print("property")
			return 'property'
		except ValueError: #sub
			#print("subs")
			for k,v in node.items():
				sub = self.sub_get_info(v)
				if sub == 'event' or sub =='property':
					info[k] = sub
				else:
					for k1,v1 in subs.items():
						if self.node_eq(v1, sub):
							occurences[k1]=occurences[k1]+1
							sub = None
							break
					if sub is not None:
						subs[k]=sub
						occurences[k]=1

			i=0
			for k1,v1 in subs.items():
				if occurences[k1] == 1:
					info[k1]=v1
				else:
					if i == 0: idi=''
					else: idi= '('+str(i)+')'
					info['*'+idi+' x'+str(occurences[k1])] = v1
					i = i +1

		return info


	def get_json(self, out_file):
		if self.root is None:
			self.root = self.build_obj_from_traces()
		out_file.write(json.dumps(self.root, indent=2, separators=(',', ': ')))



########################### MAIN ##############################################

tableFilename = str.maketrans("","",".*/\\!%?+%")
tableR = str.maketrans("+-/\*:","______")
if args.output_dir is None: output_dir = ''
else: output_dir = args.output_dir[0].rstrip('/')+'/'
if args.prefix is None : prefix = ''
else: prefix = args.prefix[0]+'.'

def exec_and_write(command, serie, output_r=args.output_r):
	serie_fn = serie.translate(tableFilename)
	if (args.output_to_file):
		out_file = open(output_dir+prefix+serie_fn+'.dat','w')
	else:
		out_file = sys.stdout

	r_type=command(out_file)

	if (args.output_to_file): out_file.close()

	if output_r:
		dataname = (prefix+serie_fn).translate(tableR)
		with open(output_dir+prefix+'reads.R','a') as r_file:
			r_file.write(dataname+' <<- read.table("'+out_file.name+'",sep="", header=TRUE)\n')
		with open(output_dir+prefix+'plots.R','a') as r_file:
			r_file.write('plot('+dataname+'$date,'+dataname+'$value, type="'+r_type+'")\n')


trace = Trace(args.src_filename)

if args.dump_events is not None:
	for pattern in args.dump_events:
		exec_and_write(lambda out_file : trace.grep_event(pattern, out_file), pattern)

if args.dump_greps is not None:
	for pattern in args.dump_greps:
		exec_and_write(lambda out_file : trace.grep(pattern, out_file), pattern)
			
if args.count_if_args is not None:		
	while len(args.count_if_args) > 0:
		argset = args.count_if_args[0:3]
		args.count_if_args = args.count_if_args[3:]

		if (argset[1] == 'eq'): lambda_cmp= lambda x,y: x == y
		elif (argset[1] == 'ne' or argset[1] == 'neq'): lambda_cmp= lambda x,y: x != y
		elif (argset[1] == 'gt'): lambda_cmp= lambda x,y: x > y
		elif (argset[1] == 'ge'): lambda_cmp= lambda x,y: x >= y
		elif (argset[1] == 'lt'): lambda_cmp= lambda x,y: x < y
		elif (argset[1] == 'le'): lambda_cmp= lambda x,y: x <= y
		else:
			print('Comparator '+argset[1]+' is not reconized: count-if', argset[0],argset[1],argset[2], 'not computed')
			continue
	
		exec_and_write(
			lambda out_file : 
				trace.count_if(argset[0], lambda_cmp, argset[2],out_file), 
				argset[0]+'-'+argset[1]+'-'+argset[2])


if (args.info_dump):
	exec_and_write(lambda out_file: trace.get_info(out_file), 'info', False)		

if (args.json_dump):
	exec_and_write(lambda out_file: trace.get_json(out_file), 'json', False)
