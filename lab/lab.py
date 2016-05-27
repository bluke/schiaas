#!/usr/bin/python3
#pylint: disable=too-many-branches, line-too-long, consider-using-enumerate
"""
This script executes simulations according to one config file.
It takes at least one configuration file as argument.
It needs the java classpath to be set.

The configuration file format is as follows:
SETUP_DIR indicates the directory containing setup files (should be called first)
INCLUDE indicates a file to read as addtional configuration file
NEEDED indicates file or directory that are to be in the simuation directory
NEEDED_POST indicates file or directory that are to be in the data directory
PRE_COMMAND_SETUP indicates a command to run before the simulation in the setup directory
POST_COMMAND_DATA indicates a command to run in the data directory
TU_ARG indicates the arguments to pass to the trace_util.py script
SIM_ARG x[:id] arg indicates the argument of the simulation
 - x: the id of the argument (must be grouped);
 - id: the id of the simulations using this argument;
 - arg: the argument.

see lab/setup/cmp-scheduler/cmp-scheduler.cfg for example
Two options are available:
-k : keeps the data from previous executions
-p x : run x simulations and x trace-util.py in parallel,

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

def get_path(path, config):
    """
    Gets the absolute path corresponding to a given path.

    This function searches relative to the setupdir, the cwd, or the script file.

    Args:
        path: the path to look for
        config: configuration containing the setupdir

    Returns: the absolute path of the file found or None
    """
    if not config['setupdir'] is None and os.path.exists(os.path.join(config['setupdir'], path)):
        return os.path.abspath(os.path.join(config['setupdir'], path))
    elif os.path.exists(path):
        return os.path.abspath(path)
    elif os.path.exists(os.path.join(os.path.dirname(os.path.realpath(__file__)), path)):
        return os.path.abspath(os.path.join(os.path.dirname(os.path.realpath(__file__)), path))
    else:
        return None

def parse_simarg(line):
    """
    Parse a SIMARG line form the config file
    Arg: the tail of a SIMARG line from the cfg file
    Returns: a SimArg dictionnary
    """
    res = {}
    lst = line.split(maxsplit=1)
    res['argument'] = lst[1]
    order = lst[0].split(':', maxsplit=1)
    try:
        res['index'] = int(order[0])
    #pylint: disable=undefined-variable
    except ValueERROR:
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
    included = []
    precommandsetup = []
    print("Parsing {0}".format(filename), file=sys.stderr)
    with open(filename, 'r') as cfg:
        for readline in cfg:
            line = readline.split(maxsplit=1)
            if len(line) == 0:
                continue
            elif line[0] == 'SETUP_DIR' and config['setupdir'] is None:
                givenpath = line[1].rstrip()
                setdpath = get_path(givenpath, config)
                if not setdpath is None and os.path.isdir(setdpath):
                    config['setupdir'] = setdpath
                else:
                    print("Error : the SETUP_DIR '{0}' was not found.".format(givenpath),
                          file=sys.stderr)
                    sys.exit(3)
            elif line[0] == 'NEEDED':
                config['needed'] += line[1].split()
            elif line[0] == 'NEEDED_POST':
                config['neededpost'] += line[1].split()
            elif line[0] == 'PRE_COMMAND_SETUP':
                precommandsetup.append(line[1].rstrip())
            elif line[0] == 'POST_COMMAND_DATA':
                config['postcommanddata'].append(line[1].rstrip())
            elif line[0] == 'TU_ARG':
                config['tuarg'].append(line[1].rstrip())
            elif line[0] == 'SIM_ARG':
                config['simarg'].append(parse_simarg(line[1].rstrip()))
            elif line[0] == 'INCLUDE':
                included.append(line[1].rstrip())
    for command in precommandsetup:
        print("Running pre-command '{0}' in '{1}'".format(command, "." if config['setupdir'] is None else config['setupdir']))
        process = subprocess.Popen(command, shell=True, cwd=config['setupdir'])
        process.wait()
    config['precommandsetup'] += precommandsetup
    for inc in included:
        includepath = get_path(inc, config)
        if not includepath is None and os.path.isfile(includepath):
            print("Including config file '{0}'".format(inc))
            read_cfg_file(get_path(inc, config), config)
        else:
            print("Error : the INCLUDE file {0} can not be found".format(inc), file=sys.stderr)
            sys.exit(4)

def set_dirs(config):
    """
    Sets directories in config.
    """
    config['cdir'] = os.path.abspath('.')
    config['scriptdir'] = os.path.abspath(os.path.dirname(os.path.realpath(__file__)))
    config['bindir'] = os.path.join(config['scriptdir'], "bin")
    config['simdir'] = os.path.join(config['cdir'], "simulations")
    config['datadir'] = os.path.join(config['cdir'], "data")
    config['archdir'] = os.path.join(config['cdir'], "archive")

def plan_experiments(config):
    """
    Prepares all possible combinations of experiements.
    """
    sort = []
    for i in range(1, max(config['simarg'], key=lambda x: x['index'])['index']+1):
        sort.append([x for x in config['simarg'] if x['index'] == i])
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

def setup_simulation(in_queue, out_queue, config):
    """
    Sets up simulations dir of sims taken from queue

    Args :
        in_queue : queue from where to read simulation
        out_queue : where to ouptput prepared experiment
        config : the configuration object including the NEEDED files
                and the simulation directory
    """
    while True:
        exp = in_queue.get()
        if exp is None:
            break
        print("Setting up simulation {0}".format(exp[0]))
        exp_dir = os.path.join(config['simdir'], exp[0])
        os.makedirs(exp_dir)
        for need in config['needed']:
            src_path = get_path(need, config)
            dst_path = os.path.join(exp_dir, need)
            if src_path is None:
                print("Error NEEDED object '{0}' has not been found".format(need), file=sys.stderr)
                out_queue.put(None)
                return
            elif os.path.isdir(src_path):
                if not os.path.exists(dst_path):
                    shutil.copytree(src_path, dst_path)
            elif os.path.isfile(src_path):
                if not os.path.exists(os.path.dirname(dst_path)):
                    os.makedirs(os.path.dirname(dst_path))
                shutil.copy(src_path, dst_path)
            else:
                print("Error NEEDED object '{0}' is not a file or a directory".format(need), file=sys.stderr)
                out_queue.put(None)
                return

        for arg in exp[1]:
            src_path = get_path(arg, config)
            dst_path = os.path.join(exp_dir, arg)
            if src_path is None:
                pass
            elif os.path.isdir(src_path):
                if not os.path.exists(dst_path):
                    shutil.copytree(src_path, dst_path)
            elif os.path.isfile(src_path):
                if not os.path.exists(os.path.dirname(dst_path)):
                    os.makedirs(os.path.dirname(dst_path))
                shutil.copy(src_path, dst_path)

        out_queue.put(exp)
    out_queue.put(None)

def run_simulation(in_queue, out_queue, config):
    """
    Runs simulations from queue
    """
    while True:
        exp = in_queue.get()
        if exp is None:
            break
        exp_dir = os.path.join(config['simdir'], exp[0])
        command = ['java']
        command += exp[1]
        print("Run simulation {0}".format(exp[0]))
        with open(os.path.join(exp_dir, "simgrid.out"), 'w') as out_file:
            process = subprocess.Popen(command, stdout=out_file, stderr=out_file, cwd=exp_dir)
            process.wait()

        if process.returncode == 0:
            out_queue.put(exp)

    out_queue.put(None)


def extract_trace(in_queue, config):
    """
    Runs trace utils on experiments from queue
    """
    util = os.path.join(config['bindir'], "trace-util.py")
    while True:
        exp = in_queue.get()
        if exp is None:
            break
        exp_dir = os.path.join(config['simdir'], exp[0])
        print("Processing traces for {0}".format(exp[0]))
        for tuarg in config['tuarg']:
            command = [util, "schiaas.trace", "-o", config['datadir'], "-f", exp[0], tuarg]
            process = subprocess.Popen(" ".join(command), shell=True, cwd=exp_dir)
            process.wait()


def clean_simdir(keep, config):
    """
    cleans the simdir

    keep : bool whether to keep prevous results
    """
    if keep:
        if os.path.exists(os.path.join(config['simdir'], 'simulations.args')):
            with open(os.path.join(config['simdir'], 'simulations.args'), 'r') as simargs_file:
                archname = simargs_file.readline().strip()
            if not os.path.exists(config['archdir']):
                os.makedirs(config['archdir'])
            shutil.copytree(config['simdir'], os.path.join(config['archdir'], archname))

    if os.path.exists(config['simdir']):
        shutil.rmtree(config['simdir'])
    if os.path.exists(config['datadir']):
        shutil.rmtree(config['datadir'])
    os.mkdir(config['simdir'])
    os.mkdir(config['datadir'])

def print_plan(plan, config):
    """
    Writes the simulations.args file
    """
    now = datetime.datetime.now()
    file_path = os.path.join(config['simdir'], 'simulations.args')
    with open(file_path, 'w') as simargs_file:
        print("{}-{}-{}_{}{}".format(now.year, now.month, now.day, now.hour, now.minute), file=simargs_file)
        for exp in plan:
            print(exp[0], " ".join(exp[1]), file=simargs_file)

def run_post_commands(config):
    """
    Runs post commands in the data dir
    """
    for need in config['neededpost']:
        src_path = get_path(need, config)
        dst_path = os.path.join(config['datadir'], need)
        if src_path is None:
            print("Error NEEDED_POST object '{0}' has not been found".format(need), file=sys.stderr)
            return
        elif os.path.isdir(src_path):
            if not os.path.exists(dst_path):
                shutil.copytree(src_path, dst_path)
        elif os.path.isfile(src_path):
            if not os.path.exists(os.path.dirname(dst_path)):
                os.makedirs(os.path.dirname(dst_path))
            shutil.copy(src_path, dst_path)
        else:
            print("Error NEEDED_POST object '{0}' is not a file or a directory".format(need), file=sys.stderr)
            return

    for command in config['postcommanddata']:
        print("Running post-command {0} in {1}".format(command, config['datadir']))
        process = subprocess.Popen(command, shell=True, cwd=config['datadir'])
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
    parser.add_argument("confFile", action="store", type=str,
                        help="Config file describing the experiment")
    args = parser.parse_args()

    config = {'setupdir':None, 'needed':[], 'neededpost':[],
              'precommandsetup':[], 'postcommanddata':[],
              'tuarg':[], 'simarg':[]}

    # set minimal config
    set_dirs(config)

    # cleanup previous configuration
    clean_simdir(args.keep, config)

    # load configuration file
    if not os.path.isfile(args.confFile):
        print("Error : the config file '{0}' was not found.".format(args.confFile), file=sys.stderr)
        sys.exit(1)
    else:
        read_cfg_file(args.confFile, config)

    # plan simulations
    plan = plan_experiments(config)
    print_plan(plan, config)

    # prepare pipelines
    set_simspace_queue = queue.Queue(len(plan)+args.numParal+1)
    run_sim_queue = queue.Queue(len(plan)+args.numParal+1)
    trace_output_queue = queue.Queue(len(plan)+args.numParal+1)

    for exp in plan:
        set_simspace_queue.put(exp)
    for _ in range(args.numParal):
        set_simspace_queue.put(None)

    # Run workers
    setters = []
    runners = []
    tracers = []
    for _ in range(args.numParal):
        setters.append(threading.Thread(target=setup_simulation, args=(set_simspace_queue, run_sim_queue, config)))
        runners.append(threading.Thread(target=run_simulation, args=(run_sim_queue, trace_output_queue, config)))
    for setter in setters:
        setter.start()
    for runner in runners:
        runner.start()

    # waiting for setter
    for setter in setters:
        setter.join()

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
