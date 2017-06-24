#!/usr/bin/python3
#pylint: disable=invalid-name,missing-docstring,line-too-long
import argparse
import copy
import numpy
import os
import sys
from numpy import arange

def writeFile(fileName, outlist, boottimes):
    with open(fileName, 'w') as out_file:
        for line in boottimes:
            out_file.write(line)
        out_file.write("[tasks]\n")
        data = sorted(outlist, key=lambda k: k['index'])
        for line in data:
            out = "{0}\t{1}\t{2}".format(line['id'], line['subDate'], line['predicted'])
            if 'runtime' in line:
                out += "\t~ {0} 0 0 0 1".format(line['runtime'])
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
    boot = []
    res={}
    if os.path.isfile(fileName):
        with open(fileName, 'r') as f:
            line = f.readline()
            if "[boots]" in line:
                while not '[tasks]' in line:
                    boot.append(line)
                    line = f.readline()
            else:
                f.seek(0)
            for line in f:
                dic = loadLine(line.split())
                dic["index"] = len(data)
                data.append(dic)
        res["boot"]=boot
        res["data"]=data
    return res

def updateDrawData(inData, scale, treat):
    res = []
    for line in inData:
        if treat == 'add':
            line['runtime'] = numpy.random.uniform(line['runtime']-scale,line['runtime']+scale)
        elif treat == 'multiply':
            line['runtime'] = numpy.random.uniform(line['runtime']*(1-scale),line['runtime']*(1+scale))
        if line['runtime'] < 0:
            line['runtime'] = 0
        res.append(line)
    return res

def updateRangeData(inData, scale, treat):
    res = []
    for line in inData:
        if treat == 'add':
            line['runtime'] = line['runtime']+scale
        elif treat == 'multiply':
            line['runtime'] = round(line['runtime']*(1+scale),5)
        if line['runtime'] < 0:
            line['runtime'] = 0
        res.append(line)
    return res

def zeroSubs(inData):
    for ent in inData:
        ent['subDate']=0


def generateDrawTaskfile(inData, itr, value, treatment, basename, path, Btimes):
    outData = updateDrawData(copy.deepcopy(inData), value, treatment)
    fileName = "{0}_{1}-draw-{2}_{3}".format(basename, treatment, value, itr)
    writeFile(path+"/"+fileName+".tasks", outData, Btimes)
    print("{1}/{0}.tasks".format(fileName, path))

def generateRangeTaskfile(inData, value, treatment, basename, path, Btimes):
    outData = updateRangeData(copy.deepcopy(inData), value, treatment)
    fileName = "{0}_{1}_{2}".format(basename, treatment, value)
    writeFile(path+"/"+fileName+".tasks", outData, Btimes)
    print("{1}/{0}.tasks".format(fileName, path))

def drawVal(scale):
    return numpy.random.uniform(-scale,scale)


def main():
    parser = argparse.ArgumentParser(description="Generates Simschlouder job files and the corresponding lab file")
    parser.add_argument("-f", "--file", action='store', required=True, type=str, dest='inputFile',
                        help="The source simschlouder jobfile")
    parser.add_argument("-z", "--zero", action='store_true', required=False, dest='zero',
                        help="bring all sub dates to 0, but preserve sub order")
    parser.add_argument("-m", "--method", action="append", required=True, type=str, dest="methods",
                        choices=["draw", "range", "global"])
    parser.add_argument("-t", "--treatment", action="store", required=True, type=str, dest="treatment",
                        choices=["raw", "add", "multiply"],
                        help="Way the random values are used")
    parser.add_argument("-s","--scale", action='append', required=True, type=float,dest="scale",
                        help="Parameter of the operation, draw interval size applied value")
    parser.add_argument("-r", "--repetitions", action="store", required=False, type=int, dest="num", default=1,
                        help="Number of taskfile to generate for draw or interval modes")
    parser.add_argument("-p", "--path", action="store", required=False, type=str, dest="path", default="./",
                        help="Path to store taskfiles")
    args = parser.parse_args()

    inputs = loadFile(args.inputFile)
    inData = inputs["data"]
    inBoot = inputs["boot"]
    baseName = os.path.splitext(os.path.basename(args.inputFile))[0]
    
    if args.zero:
        zeroSubs(inData)
    
    if "draw" in args.methods:
        for value in args.scale:
            for i in range(args.num):
                generateDrawTaskfile(inData, i, value, args.treatment, baseName, args.path, inBoot)
    if "range" in args.methods:
        for value in args.scale:
            generateRangeTaskfile(inData, -(value) , args.treatment, baseName, args.path, inBoot)
            generateRangeTaskfile(inData, value , args.treatment, baseName, args.path, inBoot)
    if "global" in args.methods:
        for i in range(args.num):
            for scale in args.scale:
                value = drawVal(scale)
                #generateRangeTaskfile(inData, -(value) , args.treatment, baseName, args.path, inBoot)
                generateRangeTaskfile(inData, value , args.treatment, baseName, args.path, inBoot)




if __name__ == '__main__':
    main()
