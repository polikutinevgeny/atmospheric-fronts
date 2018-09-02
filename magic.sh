#!/bin/bash
k1=( "0.25" "0.5" "0.75" )
k2=( "1.0" "1.5" "2.0" )
k3=( "0.3" "0.5" "0.8" )
for kk1 in "${k1[@]}"; do
	for kk2 in "${k2[@]}"; do
		for kk3 in "${k3[@]}"; do
			(
				./generate_pics_cmd.sh $kk1 $kk2 $kk3
			)
			(
				cd pics
				../make_mp4.sh
			)
			(
				cd mask_pics
				../make_mp4.sh
			)
			mkdir "${kk1} ${kk2} ${kk3}"
			cp -r pics "${kk1} ${kk2} ${kk3}"
			cp -r mask_pics "${kk1} ${kk2} ${kk3}"
		done
	done
done
