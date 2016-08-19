#!/usr/bin/python3
#pylint: disable=invalid-name,missing-docstring,line-too-long
import argparse
import copy
import os
from rpy2.robjects.packages import importr

def writeFile(fileName, data):
    with open(fileName, 'w') as out_file:
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
        res['subDate'] = int(next(itr))
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
                else:
                    res['inbound'] = float(v)
                    res['outbound'] = float(next(itr))
    return res

def loadFile(fileName):
    data = []
    if os.path.isfile(fileName):
        with open(fileName, 'r') as f:
            for line in f:
                dic = loadLine(line.split())
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

def base(dic):
    if 'runtime' in dic:
        return dic['runtime']
    else:
        return dic['predicted']

def updateData(inData, rVals, treat, expect):
    res = []
    r = iter(rVals)
    for line in inData:
        if treat == 'add':
            line['runtime'] = base(line)+next(r)
        elif treat == 'norm':
            line['runtime'] = base(line)*(expect/next(r))
        else:
            line['runtime'] = next(r)
        if line['runtime'] < 0:
            line['runtime'] = 0
        res.append(line)
    return res

def generateTaskfile(inData, itr, args):
    rVals = getRandomValues(len(inData), args.drawFunc, args.expect, args.shape)
    outData = updateData(copy.deepcopy(inData), rVals, args.treatment, args.expect)
    fileName = "{0}_{1}_{2}-{3}-{4}_{5}".format(os.path.splitext(os.path.basename(args.inputFile))[0], args.treatment, args.drawFunc, args.expect, args.shape, itr).replace(".",",")
    writeFile(args.path+"/"+fileName+".tasks", outData)
    print("SIM_ARG 3:{0} {1}/{0}.tasks".format(fileName, args.path))


def main():
    parser = argparse.ArgumentParser(description="Generates Simschlouder job files and the corresponding lab file")
    parser.add_argument("-f", "--file", action='store', required=True, type=str, dest='inputFile',
                        help="The source simschlouder jobfile")
    parser.add_argument("-g", "--genrator", action="store", required=True, type=str, dest="drawFunc",
                        choices=["weibull", "normal"],
                        help="The type of random generator to use : weibull,or normal")
    parser.add_argument("-e", "--expectation", action="store", required=True, type=float, dest="expect",
                        help="Expectation or scale of the random process")
    parser.add_argument("-s", "--shape", "--sd", action="store", required=True, type=float, dest="shape",
                        help="Shape/Standard Deviation or otherwise second moment of the random process")
    parser.add_argument("-t", "--treatment", action="store", required=True, type=str, dest="treatment",
                        choices=["raw", "add", "norm"],
                        help="Way the random values are used : RAW, ADDed to the prevision, or \"NORMalized\" (prevision*(expect/rand_val))")
    parser.add_argument("-n", "--number", action="store", required=False, type=int, dest="num", default=1,
                        help="Number of taskfile to generate")
    parser.add_argument("-p", "--path", action="store", required=False, type=str, dest="path", default="./",
                        help="Path to store taskfiles")
    args = parser.parse_args()

    inData = loadFile(args.inputFile)
    for i in range(args.num):
        generateTaskfile(inData, i, args)



if __name__ == '__main__':
    main()
