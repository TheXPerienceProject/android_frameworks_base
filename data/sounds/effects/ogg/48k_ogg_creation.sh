#!/bin/bash
# Author: Carlos 'klozz' jesus
# Date: 10/jun/2023

oggname=$1

echo -e "Generating 48kz ogg from ${oggname}.ogg"
oggdec -o temp.wav ${oggname}.ogg
sox temp.wav -r 48000 temp48k.wav
oggenc -b 80 -o ${oggname}_48k.ogg temp48k.wav

echo " cleaning temp files"
rm *temp*
echo " done!!!"