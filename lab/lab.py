#!/usr/bin/python3
#pylint: disable=too-many-branches, line-too-long, consider-using-enumerate
"""
This script executes simulations according to one config file.
It takes at least one configuration file as argument.
It needs the java classpath to be set.

The configuration file format is as follows:
SETUP_DIR indicates the directory containing setup files (should be called first)
INCLUDE indicates a file to read as addtional configuration file
NEEDED indicates file or directory that are to be in the simuation directory (make symbolic links)
NEEDED_POST indicates file or directory that are to be in the data directory (make symbolic links)
PRE_COMMAND_SETUP indicates a command to run before the simulations in the setup directory
POST_COMMAND_SETUP indicates a command to run after the simulations in the setup directory
POST_COMMAND_DATA indicates a command to run after the simulations in the data directory
TU_ARG indicates the arguments to pass to the trace_util.py script
SIM_ARG x[:id] arg indicates the argument of the simulation
 - x: the id of the argument (must be grouped);
 - id: the id of the simulations using this argument;
 - arg: the argument.

see lab/setup/cmp-scheduler/cmp-scheduler.cfg for example
Two options are available:
-k : keeps the data from previous executions
-p x : run x simulations and x trace-util.py in parallel,
-t x : set the timeout x for the simulations,

@author julien.gossa@unistra.fr
@author lbertot@unistra.fr
"""
"""
Error exit codes
1 : config file not found
2 : SIM_ARG order number is not a number
3 : failed to find Setup dir
4 : failed to find included file.
"""

import argparse
import datetime
import glob
import itertools
import os
import queue
import shutil
import subprocess
import sys
try:
    import threading
except ImportError:
    print("Threading module missing using dummy threading.", file=sys.stderr)
    import dummy_threading as threading

def labspath(path, config):
    """
    Return the absolute path corresponding to a given path,
    searching relative to the SETUP_DIR, the cwd, or the script file.
    If not found return the given path.

    Args:
        path: the path to look for
        config: configuration containing the SETUP_DIR

    Returns: the absolute path of the file found or None
    """
    for prefix in [ config['SETUP_DIR'], "", os.path.dirname(sys.argv[0]) ]:
        try:
            fullpath = os.path.abspath(os.path.join(prefix, path))
            if len(glob.glob(fullpath)) != 0:
                return fullpath
        except TypeError:
            pass

    return path

def parse_SIM_ARG(line):
    """
    Parse a SIM_ARG line form the config file
    Arg: the tail of a SIM_ARG line from the cfg file
    Returns: a SIM_ARG dictionnary
    """
    res = {}
    lst = line.split(maxsplit=1)
    try:
        res['argument'] = lst[1]
    except IndexError:
        res['argument'] = "" #foir void arguement
    order = lst[0].split(':', maxsplit=1)
    try:
        res['index'] = int(order[0])
    #pylint: disable=undefined-variable
    except ValueError:
        print("Error : in SIM_ARG order '{0}' is not a digit".format(order[0]), file=sys.stderr)
        sys.exit(2)
    if len(order) == 2:
        res['name'] = order[1]
    return res

def read_cfg_file(filename, config):
    """
    Recursively parse content of cfg files, and adds content to dictionary.
    Also executes PRE_COMMANDS

    Args:
        filename : path to file to parse
        config : lab configuration dictionnary
    """
    #print("Parsing {0}".format(filename), file=sys.stderr)
    with open(filename, 'r') as cfg:
        for readline in cfg:
            line = readline.rstrip().split()

            if len(line) == 0 or line[0][0] == "#":
                continue

            for i in range(1,len(line)):
                line[i]=labspath(line[i], config)

            if line[0] == 'SETUP_DIR':
                config['SETUP_DIR'] = line[1]
                if not os.path.isdir(config['SETUP_DIR']):
                    print("Error : the SETUP_DIR '{0}' was not found.".format(config['SETUP_DIR']),
                          file=sys.stderr)
                    sys.exit(3)

            elif line[0] == 'PRE_COMMAND_SETUP':
                command = ' '.join(line[1:])
                print("Running pre-command '{0}' in '{1}'".format(command, config['SETUP_DIR']))
                process = subprocess.Popen(command, shell=True, cwd=config['SETUP_DIR'])
                process.wait()

            elif line[0] == 'INCLUDE':
                for arg in line[1:]:
                    read_cfg_file(arg, config)

            elif line[0] == 'POST_COMMAND_DATA':
                config[line[0]].append(' '.join(line[1:]))

            elif line[0] == 'POST_COMMAND_SETUP':
                config[line[0]].append(' '.join(line[1:]))

            elif line[0] == 'SIM_ARG':
                config[line[0]].append(parse_SIM_ARG(' '.join(line[1:])))

            elif line[0] == 'TU_ARG':
                try:
                    config[line[0]][line[1]]+=line[2:]
                except KeyError:
                    config[line[0]][line[1]]=line[2:]

            elif line[0] == 'NEEDED' or line[0] == 'NEEDED_POST':
                config[line[0]]+=line[1:]

            else:
                print("WARNING: config not recognized {0}".format(' '.join(line)), file=sys.stderr)

def set_dirs(config):
    """
    Sets directories in config.
    """
    config['cdir'] = os.path.abspath('.')
    config['scriptdir'] = os.path.abspath(os.path.dirname(os.path.realpath(__file__)))
    config['bindir'] = os.path.join(config['scriptdir'], "bin")
    config['simdir'] = os.path.join(config['cdir'], "simulations")
    config['datadir'] = os.path.join(config['cdir'], "data")

    # making dirs
    try:
        os.mkdir(config['simdir'])
    except FileExistsError:
        pass
    try:
        os.mkdir(config['datadir'])
    except FileExistsError:
        pass

def plan_experiments(config):
    """
    Prepares all possible combinations of experiements.
    """
    sort = []
    for i in range(1, max(config['SIM_ARG'], key=lambda x: x['index'])['index']+1):
        sort.append([x for x in config['SIM_ARG'] if x['index'] == i])
    planner = list(itertools.product(*sort))
    res = []
    for i in planner:
        name = 'xp'
        command = []
        for j in range(len(i)):
            command.append(i[j]['argument'])
            if 'name' in i[j]:
                name += '_' + i[j]['name']
        res.append((name, command))
    return res

def run_simulation(in_queue, out_queue, config):
    """
    Runs simulations from queue
    """
    while True:
        exp = in_queue.get()
        if exp is None:
            break

        exp_dir = os.path.join(config['simdir'], exp[0])

        if config['keep'] and os.path.exists(os.path.join(exp_dir,'simgrid.out')):
            print("Skipping simulation {0}".format(exp[0]))
            out_queue.put(exp)
        else :
            print("Running simulation {0}".format(exp[0]))
    
            # make the directory
            if os.path.exists(exp_dir):
                shutil.rmtree(exp_dir)
            os.mkdir(exp_dir)

            # make symbolic link to needed files
            for need in config['NEEDED']:
                for file in glob.glob(need):
                    os.symlink(file,os.path.join(exp_dir,os.path.basename(file)))

            # run the simulation
            command = ['java']
            for e in exp[1]:
                command += e.split()
            
            with open(os.path.join(exp_dir, "simgrid.out"), 'w') as out_file:
                process = subprocess.Popen(command, stdout=out_file, stderr=out_file, cwd=exp_dir)
                try:
                    process.wait(timeout=config['timeout'])
                except:
                    process.kill()
                    process.returncode="timeout expired"
            if process.returncode == 0:
                out_queue.put(exp)
            else:
                print("WARNING simulation {0} exits with code {1}".format(exp[0],process.returncode), file=sys.stderr)

    out_queue.put(None)


def extract_trace(in_queue, config):
    """
    Runs trace utils on experiments from queue
    """

    if len(config['TU_ARG']) == 0:
        return

    util = os.path.join(config['bindir'], "trace-util.py")
    while True:
        exp = in_queue.get()
        if exp is None:
            break
        exp_dir = os.path.join(config['simdir'], exp[0])
        print("Processing traces for {0}".format(exp[0]))
        command = [util, "schiaas.trace", "-o", config['datadir'], "-f", exp[0]]
        for k in config['TU_ARG']:
            command.append(k)
            command+=config['TU_ARG'][k]
        #print(' '.join(command))
        process = subprocess.Popen(" ".join(command), shell=True, cwd=exp_dir)
        process.wait()

def print_plan(plan, config):
    """
    Writes the simulations.args file
    """
    now = datetime.datetime.now()
    file_path = os.path.join(config['simdir'], 'simulations.args')
    with open(file_path, 'w') as SIM_ARGs_file:
        print("{}-{}-{}_{}{}".format(now.year, now.month, now.day, now.hour, now.minute), file=SIM_ARGs_file)
        for exp in plan:
            print(exp[0], " ".join(exp[1]), file=SIM_ARGs_file)

def run_post_commands(config):
    """
    Runs post commands in the data dir
    """

    # make symbolic links to needed files
    for need in config['NEEDED_POST']:
        for file in glob.glob(need):
            try:
                os.symlink(file,os.path.join(config['datadir'],os.path.basename(file)))
            except FileExistsError:
                pass

    for command in config['POST_COMMAND_DATA']:
        print("Running post-command {0} in {1}".format(command, config['datadir']))
        process = subprocess.Popen(command, shell=True, cwd=config['datadir'])
        process.wait()

    for command in config['POST_COMMAND_SETUP']:
        print("Running post-command {0} in {1}".format(command, config['SETUP_DIR']))
        process = subprocess.Popen(command, shell=True, cwd=config['SETUP_DIR'])
        process.wait()


#pylint: disable=too-many-locals
def main():
    """
    Main function of the program.
    """

    # parse arguments
    parser = argparse.ArgumentParser(description="Execute a simulation according to a config file")
    parser.add_argument("-k", action="store_true", dest="keep",
                        help="Keep result for previous run")
    parser.add_argument("-p", action="store", required=False,
                        default=1, dest="numParal", type=int,
                        help="Number of parallel simulations")
    parser.add_argument("-t", action="store", required=False,
                        default=None, dest="timeout", type=int,
                        help="Timeout for the simulations")
    parser.add_argument("confFile", action="store", type=str,
                        help="Config file describing the experiment")
    args = parser.parse_args()

    config = {'SETUP_DIR':'.', 'NEEDED':[], 'NEEDED_POST':[],
              'POST_COMMAND_SETUP':[], 'POST_COMMAND_DATA':[],
              'TU_ARG':{}, 'SIM_ARG':[], 
              'keep':args.keep, 'timeout':args.timeout}

    print(args.keep)

    # set minimal config
    set_dirs(config)

    # check CLASSPATH
    print("CLASSPATH="+os.environ.get('CLASSPATH'))

    # load configuration file
    print("Loading configuration")
    if not os.path.isfile(args.confFile):
        print("ERROR: the config file '{0}' was not found.".format(args.confFile), file=sys.stderr)
        sys.exit(1)
    read_cfg_file(args.confFile, config)

    # plan simulations
    print("Planning experiments")
    plan = plan_experiments(config)
    print_plan(plan, config)

    # prepare pipelines
    print("Preparing pipeline")
    set_simspace_queue = queue.Queue(len(plan)+args.numParal+1)
    trace_output_queue = queue.Queue(len(plan)+args.numParal+1)

    for exp in plan:
        set_simspace_queue.put(exp)
    for _ in range(args.numParal):
        set_simspace_queue.put(None)

    runners = []
    tracers = []

    # Run workers if not keep
    print("Running the simulations")
    for _ in range(args.numParal):
        runners.append(threading.Thread(target=run_simulation, args=(set_simspace_queue, trace_output_queue, config)))

    for runner in runners:
        runner.start()

    # launching analysers
    for i in range(args.numParal):
        tracers.append(threading.Thread(target=extract_trace, args=(trace_output_queue, config)))
        tracers[i].start()

    # Wait for sims and traces
    for runner in runners:
        runner.join()
    for tracer in tracers:
        tracer.join()

    # run post commands
    run_post_commands(config)

if __name__ == '__main__':
    main()
