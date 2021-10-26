#!/bin/bash

cd btuviewer
rm *.pdf simschlouder.*
./json2tikz.py ../simschlouder.json simschlouder
pdflatex -jobname=simschlouder template-btu.tex 
