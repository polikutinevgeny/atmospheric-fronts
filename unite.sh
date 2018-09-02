#!/bin/bash
for i in *.a; do 
  j="${i%*.a}.b"
  if [[ -e "$j" ]]; then
    grib_copy $i $j "${i%*.a}.grb2"
  fi
done 
