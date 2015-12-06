#!/usr/bin/env python3
# -*- coding:utf8 -*-

##	This script gather tools to exploit schiaas traces
##  
##  The documentation of processed data is done with the file trace_util.doc:
##  <data_name>\t<description of the data>
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
from collections import OrderedDict
import re
import tempfile

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
parser.add_argument('--schiaas', dest='traces_dump', action='store_const', const=True, default=False, 
	help='dump the whole trace in the schiaas format.')
parser.add_argument('--info', dest='dump_info', action='store_const', const=True, default=False, 
	help='print the properties and events available in the trace, in the json format.')
parser.add_argument('--regex', dest='dump_regex', action='store_const', const=True, default=False, 
	help='print the properties and events available in the trace, in the regex format')
parser.add_argument('-d', dest='print_doc', action='store_const', const=True, default=False, 
	help='print documentation whenever it is possible')
parser.add_argument('-f', dest='prefix', metavar=('prexif'), nargs='*', 
	help='write the output to file with the given prefix')
parser.add_argument('-o', dest='output_dir', metavar=('output directory'), nargs='?', default=".", 
	help='write the output to files')

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
			self.traces = self.build_traces_from_obj()
		elif src_type == 'schiaas':
			self.traces = self.read_schiaas(src_filename)
		else:
			self.root = self.read_paje(src_filename)

		self.doc = self.read_doc()

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
			root = json.load(src_file, object_pairs_hook=collections.OrderedDict)
		return root

	def read_schiaas(self, schiaas_filename):
		traces = []
		with open(schiaas_filename) as src_file:
			for log in src_file:
				traces.append(log.split(self.field_sep))
		return traces

	def build_obj_from_traces(self, traces=None):
		if traces == None: traces = self.traces
		
		root = collections.OrderedDict()
		for [entities, key, val] in traces:
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

	def trace_key(self, trace):
		try:
			return float(trace[1])
		except:
			return 0


	def build_traces_from_obj(self, root=None):
		if root == None: root = self.root
		traces = []
		
		self.sub_build_traces_from_obj(root, "", "root", traces)

		return sorted(traces, key=self.trace_key)

	def sub_build_traces_from_obj(self, node, entities, entity, traces):
		if type(node) is OrderedDict:
			for k,v in node.items():
				self.sub_build_traces_from_obj(v, entities+':'+entity, k, traces)
		elif type(node) is list:
			for i in range(len(node)):
				try: item_id=str(node[i]['name'])
				except:
					try: item_id="id-"+str(node[i]['id'])
					except:
						try: item_id=str(node[i]['host'])
						except :
							item_id="item-"+str(i)
				self.sub_build_traces_from_obj(node[i], entities+':'+entity, item_id, traces)
		elif type(node) is str:
			traces.append([entities[1:], entity, node])


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

	def read_doc(self):
		doc = {}
		try:
			with open(os.path.dirname(os.path.realpath(__file__))+'/trace-util.doc') as doc_file:
				line = doc_file.readline()
				while line != "":
					(key,val) = line.strip().split(self.field_sep)
					doc[key] = val
					line = doc_file.readline()
		except FileNotFoundError:
			pass
		return doc


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
					out_file.write(pattern.translate(tableR)+self.field_sep+str(last_date)+self.field_sep+str(last_count)+'\n')
					last_printed_count = last_count
				last_count = count
				last_date = date

		out_file.write(pattern.translate(tableR)+self.field_sep+str(last_date)+self.field_sep+str(last_count)+'\n')
		if (date != last_date):
			out_file.write(pattern.translate(tableR)+self.field_sep+str(date)+self.field_sep+str(count)+'\n')

	def grep(self, pattern, out_file):
		header = 'date'
		res = ''

		for [entities, key, val] in self.traces:
			if re.search(pattern, entities) is not None:
				res = res + entities+self.field_sep+key+self.field_sep+val+'\n'
				try: float(key)
				except ValueError: header = 'key'

		out_file.write("entity"+self.field_sep+header+self.field_sep+"value\n")
		out_file.write(res)

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
		traces = []
		for (entities, key, val) in self.sub_get_regex():
			entities = entities.split(self.entity_sep)
			if re.search("^event", val) is not None: entities.pop()
			traces.append((self.entity_sep.join(entities),key,val))

		obj = self.build_obj_from_traces(traces)
		out_file.write(json.dumps(obj,indent=2, separators=(',', ': ')))
		return

	def get_regex(self, out_file):
		regexes = self.sub_get_regex()

		for (entities, key, val) in regexes:
			out_file.write(self.field_sep.join((entities,key,val))+'\n')

	def sub_get_regex(self):
		regexes = []
		for [entities, key, val] in self.traces:
			entities = entities.split(self.entity_sep)	
			try: #event
				float(key)
				key = entities[-1]
				val = "event"
			except ValueError: #prop
				val = "property"

			if args.print_doc and key in self.doc:
				val = val+': '+self.doc[key]

			for (e,k,v) in regexes :
				if (k==key and v==val and len(e) == len(entities)):
					for i in range(0, len(e)) :
						if (e[i] != entities[i]):
							e[i] = '.*'
					key = None
					break
			if key is not None :

				regexes.append((entities, key, val))

		res = []
		for (entities, key, val) in regexes:
			res.append((self.entity_sep.join(entities),key,val))

		return sorted(res, key=lambda e:e[0])


	def get_json(self, out_file):
		if self.root is None:
			self.root = self.build_obj_from_traces()
		out_file.write(json.dumps(self.root, indent=2, separators=(',', ': ')))


	def get_traces(self, out_file):
		if self.traces is None:
			self.traces = self.build_traces_from_obj()
		for [e,k,v] in self.traces:
			out_file.write(e+self.field_sep+k+self.field_sep+v+'\n')


########################### MAIN ##############################################

tableFilename = str.maketrans(":-","__",".*/\\!%?+%\$$")
tableR = str.maketrans("+-/\*:","______",'\$$')


if args.prefix is not None: 
	output_dir=args.output_dir.rstrip('/')+'/'
	try: 
		prefix = args.prefix[0].translate(tableR)+'.'
	except IndexError:
		prfix = ''
else: prefix = None

def exec_and_write(command, serie):
	serie_fn = serie.translate(tableFilename)
	if (prefix is not None):
		out_file = open(output_dir+prefix+serie_fn+'.dat','w')
	else:
		out_file = sys.stdout

	command(out_file)

	if (prefix is not None): out_file.close()

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


if (args.dump_info):
	exec_and_write(lambda out_file: trace.get_info(out_file), 'info')

if (args.dump_regex):
	exec_and_write(lambda out_file: trace.get_regex(out_file), 'regex')

if (args.json_dump):
	exec_and_write(lambda out_file: trace.get_json(out_file), 'json')

if (args.traces_dump):
	exec_and_write(lambda out_file: trace.get_traces(out_file), 'schiaas')
