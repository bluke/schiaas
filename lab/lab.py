#!/usr/bin/python3

"""
Error exit codes
1 : config file not found
2 : SIM_ARG order number is not a number
3 : failed to resolve a path
"""

import argparse
import os
import subprocess
import sys

def getPath(path,config):
    if not config['setupdir'] == None and os.path.exists(os.path.join(config['setupdir'],path)):
        return os.path.abspath(os.path.join(config['setupdir'],path))
    elif os.path.exists(path):
        return os.path.abspath(path)
    elif os.path.exists(os.path.join(os.path.dirname(os.path.realpath(__file__)),path)):
        return os.path.abspath(os.path.join(os.path.dirname(os.path.realpath(__file__)),path))
    else:
        print("Error : file or directory '{0}' not found relative to setupdir,cwd,or script".format(path),file=sys.stderr)
        sys.exit(3)

def parseSimArg(line):
    res = {}
    lst = line.split(maxsplit=1)
    res['argument'] = lst[1]
    name = None
    order = lst[0].split(':',maxsplit=1)
    try:
        res['index'] = int(order[0])
    except ValueERROR:
        print("Error : in SIM_ARG order '{0}' is not a digit".order[0],file=sys.stderr)
        sys.exit(2)
    if len(order)==2:
        res['name']=order[1]
    return res

def readFile(fileName,config):
    included = []
    precommandsetup = []
    print("Parsing {0}".format(fileName),file=sys.stderr)
    with open(fileName,'r') as f:
        for readLine in f:
            line = readLine.split(maxsplit=1)
            if len(line) == 0:
                continue
            elif line[0] == 'SETUP_DIR' and config['setupdir'] == None:
                config['setupdir'] = getPath(line[1].rstrip(),config)
            elif line[0] == 'NEEDED':
                config['needed']+= line[1].split()
            elif line[0] == 'PRE_COMMAND_SETUP':
                precommandsetup.append(line[1].rstrip())
            elif line[0] == 'POST_COMMAND_DATA':
                config['postcommanddata'].append(line[1].rstrip())
            elif line[0] == 'TU_ARG':
                config['tuarg'].append(line[1].rstrip())
            elif line[0] == 'SIM_ARG':
                config['simarg'].append(parseSimArg(line[1].rstrip()))
            elif line[0] == 'INCLUDE':
                included.append(line[1].rstrip()) 
    for command in precommandsetup:
        print("Running pre-command '{0}' in '{1}'".format(command,"." if config['setupdir']== None else config['setupdir']))
        process = subprocess.Popen(command,shell=True,cwd=config['setupdir'])
        process.wait()
    config['precommandsetup']+=precommandsetup
    for inc in included:
        print("Including config file '{0}'".format(inc))        
        readFile(getPath(inc,config),config)

def setDir(config):
    config['cdir'] = os.path.abspath('.')
    config['scriptdir'] = os.path.abspath(os.path.dirname(os.path.realpath(__file__)))
    config['bindir'] = os.path.join(config['scriptdir'],"bin")
    config['simdir'] = os.path.join(config['cdir'],"simulation")
    config['datadir'] = os.path.join(config['cdir'],"data")

def planExperiements(config):
    pass

def main():
    parser = argparse.ArgumentParser(description="Execute a simulation according to a config file")
    parser.add_argument("-p",action="store",required=False,
            default=1,dest="numParal",type=int,
            help="Number of parallel simulations")
    parser.add_argument("confFile",action="store",type=str,
            help="Config file describing the experiment")
    args = parser.parse_args()

 
    config={'setupdir':None,'needed':[],
            'precommandsetup':[],'postcommanddata':[],
            'tuarg':[],'simarg':[]}
    setDir(config)

    if not os.path.isfile(args.confFile):
        print("Error : the config file '{0}' was not found.".format(args.confFile),file=sys.stderr)
        sys.exit(1)
    else:
        readFile(args.confFile,config)
              
    print(max(config['simarg'],key=lambda x : x['index']))
    print([x for x in config['simarg'] if x['index']==3])


if __name__ == '__main__':
    main()
