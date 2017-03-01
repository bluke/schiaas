#!/usr/bin/python3
#pylint: disable=invalid-name,missing-docstring,line-too-long
import argparse
import copy
import os
import sys
from rpy2.robjects.packages import importr
from numpy import arange

def loadDict(args, data):
    if args.dict is not None:
        baseVals = {}
        if os.path.isfile(args.dict):
            with open(args.dict) as dictFile:
                for line in dictFile:
                    array = line.split()
                    baseVals[array[0]] = float(array[1])
        for entry in data:
            if not entry['id'] in baseVals:
                print("Dictionary does no contain any reference for job {0}".format(entry['id']), file=sys.stderr)
        return baseVals
    else:
        return None

def writeFile(fileName, outlist):
    with open(fileName, 'w') as out_file:
        data = sorted(outlist, key=lambda k: k['index'])
        for line in data:
            out = "{0}\t{1}\t{2}".format(line['id'], line['subDate'], line['predicted'])
            if 'runtime' in line:
                out += "\t~ {0}".format(line['runtime'])
            if ('inbound' in line) and ('outbound' in line):
                out += "\t{0}\t{1}".format(line['inbound'], line['outbound'])
            if 'dependencies' in line:
                out += "\t->"
                for s in line['dependencies']:
                    out += " "+s
            out += "\n"
            out_file.write(out)

def loadLine(lineArray):
    res = {}
    if len(lineArray) >= 3:
        itr = iter(lineArray)
        res['id'] = next(itr)
        res['subDate'] = float(next(itr))
        res['predicted'] = float(next(itr))
        if len(lineArray) >= 3:
            for v in itr:
                if v == '~':
                    res['runtime'] = float(next(itr))
                elif v == '->':
                    deps = []
                    for d in itr:
                        deps.append(d)
                    res['dependencies'] = deps
    return res

def loadFile(fileName):
    data = []
    if os.path.isfile(fileName):
        with open(fileName, 'r') as f:
            line = f.readline()
            if "[boots]" in line:
                while not '[tasks]' in line:
                    line = f.readline() 
            else:
                f.seek(0)
            for line in f:
                dic = loadLine(line.split())
                dic["index"] = len(data)
                data.append(dic)
    return data

def getRandomValues(size, func, expect, shape):
    stats = importr('stats')
    res = []

    if func == 'weibull':
        lst = stats.rweibull(size, shape, expect)
    elif func == 'normal':
        lst = stats.rnorm(size, expect, shape)
    else:
        lst = None

    for i in lst:
        res.append(i)

    return res

def base(dic, baseData):
    if baseData is not None:
        return baseData[dic['id']]
    #elif 'runtime' in dic:
    #    return dic['runtime']
    else:
        return dic['predicted']

def updateDrawData(inData, rVals, treat, baseData):
    res = []
    r = iter(rVals)
    for line in inData:
        if treat == 'add':
            line['runtime'] = base(line, baseData)+next(r)
        elif treat == 'multiply':
            line['runtime'] = base(line, baseData)*next(r)
        else:
            line['runtime'] = next(r)
        if line['runtime'] < 0:
            line['runtime'] = 0
        res.append(line)
    return res

def updateRangeData(inData, value, treat, baseData):
    res = []
    for line in inData:
        if treat == 'add':
            line['runtime'] = base(line, baseData)+value
        elif treat == 'multiply':
            line['runtime'] = base(line, baseData)*value
        else:
            line['runtime'] = value
        res.append(line)
    return res

def zeroSubs(inData):
    for ent in inData:
        ent['subDate']=0


def generateDrawTaskfile(inData, itr, args, baseData):
    rVals = getRandomValues(len(inData), args.drawFunc, args.drawExpect, args.drawDeviation)
    outData = updateDrawData(copy.deepcopy(inData), rVals, args.treatment, baseData)
    fileName = "{0}_{1}_{2}-{3}-{4}_{5}".format(os.path.splitext(os.path.basename(args.inputFile))[0], args.treatment, args.drawFunc, args.drawExpect, args.drawDeviation, itr)
    writeFile(args.path+"/"+fileName+".tasks", outData)
    print("{1}/{0}.tasks".format(fileName, args.path))

def generateRangeTaskfile(inData, value, args, baseData):
    outData = updateRangeData(copy.deepcopy(inData), value, args.treatment, baseData)
    fileName = "{0}_{1}_{2}".format(os.path.splitext(os.path.basename(args.inputFile))[0], args.treatment, value)
    writeFile(args.path+"/"+fileName+".tasks", outData)
    print("{1}/{0}.tasks".format(fileName, args.path))


def main():
    parser = argparse.ArgumentParser(description="Generates Simschlouder job files and the corresponding lab file")
    parser.add_argument("-f", "--file", action='store', required=True, type=str, dest='inputFile',
                        help="The source simschlouder jobfile")
    parser.add_argument("-z", "--zero", action='store_true', required=False, dest='zero',
                        help="bring all sub dates to 0, but preserve sub order")
    parser.add_argument("-d", "-dict", action='store', required=False, type=str, dest='dict',
                        default=None, help="A dictonary of base values.")
    parser.add_argument("-m", "--method", action="store", required=True, type=str, dest="method",
                        choices=["draw", "range"])
    parser.add_argument("--draw_function", action="store", required=False, type=str, dest="drawFunc",
                        choices=["weibull", "normal", "uniform"],
                        help="The type of random generator to use : weibull,or normal")
    parser.add_argument("--draw_expectation", action="store", required=False, type=float, dest="drawExpect",
                        help="Expectation or scale of the random process")
    parser.add_argument("--draw_deviation", action="store", required=False, type=float, dest="drawDeviation",
                        help="Shape/Standard Deviation or otherwise second moment of the random process")
    parser.add_argument("--min", action="store", required=False, type=float, dest="range_min",
                        help="Lowest point for range method")
    parser.add_argument("--max", action="store", required=False, type=float, dest="range_max",
                        help="highest point for range method")
    parser.add_argument("--step", action="store", required=False, type=float, dest="range_step",
                        help="step for range method")
    parser.add_argument("-t", "--treatment", action="store", required=True, type=str, dest="treatment",
                        choices=["raw", "add", "multiply"],
                        help="Way the random values are used")
    parser.add_argument("-r", "--repetitions", action="store", required=False, type=int, dest="num", default=1,
                        help="Number of taskfile to generate for draw or interval modes")
    parser.add_argument("-p", "--path", action="store", required=False, type=str, dest="path", default="./",
                        help="Path to store taskfiles")
    args = parser.parse_args()


    if args.method == "draw":
        if "drawFunc" is None or "drawExpect" is None or "drawDeviation" is None:
            print("With draw method user must provide --draw_function, --draw_expectation and, --draw_deviation", file=sys.stderr)
            sys.exit(1)
    elif args.method == "range":
        if "range_min" is None or "range_max" is None or "range_step" is None:
            print("With range method user mus provide --min, --max and, --step", file=sys.stderr)
            sys.exit(0)


    inData = loadFile(args.inputFile)
    
    if args.zero:
        zeroSubs(inData)
    
    baseData = loadDict(args, inData)

    if args.method == "draw":
        for i in range(args.num):
            generateDrawTaskfile(inData, i, args, baseData)
    if args.method == "range":
        for i in arange(args.range_min, args.range_max, args.range_step):
            generateRangeTaskfile(inData, i, args, baseData)




if __name__ == '__main__':
    main()
