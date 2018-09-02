#!/bin/bash
cd prepared_input
for f in *.grb2; do (
	n="${f%.grb2}"
	../app/bin/atmospheric-fronts -i $f --minlat 20 --maxlat 70 --minlon 110 --maxlon 210 -o fronts.csv --maskoutput mask.csv --maskareasoutput maskarea.csv --minangle 90 --searchradius 3 --lookback 2 --minlength 250 --isobaric 90000 --tfpt $1 --gradt $2 --vortt $3
	export NAME=$n
	export FILE="${n}.a"
	export FRONTS="fronts.csv"
	ncl ../fronts.ncl
	ncl ../mask.ncl
	# export FRONTS="maskarea.csv"
	# ncl ../fronts.ncl
	);
done