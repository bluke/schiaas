# Author: Emiliano Carlos de Moraes Firmino @ 10/2012
SHELL=/bin/sh
NAME=montecarlo-simulation

.SUFFIXES:
.SUFFIXES: .bib .pdf .tex
.PHONY: run

regen: | clean run

run: $(NAME).pdf

$(NAME).pdf: $(NAME).bbl $(NAME).tex
	pdflatex $(NAME).tex -draftmode
	pdflatex $(NAME).tex 

$(NAME).bbl: $(NAME).aux
	bibtex $(NAME).aux

$(NAME).aux: $(NAME).bib
	pdflatex $(NAME).tex -draftmode
	pdflatex $(NAME).tex -draftmode

clean:
	rm -rf *.aux *.lof *.log *.lot *.toc *.bbl *.blg *pdf
