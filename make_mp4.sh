#!/bin/bash
ffmpeg -y -framerate 2 -pattern_type glob -i '*.png' -c:v libx264 -pix_fmt yuv420p animated.mp4 
