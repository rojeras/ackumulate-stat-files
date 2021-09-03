#!/usr/bin/env sh

# ------------------------------------------------------------------------------------
# Set the following paths
PATH_TO_STAT_FILES=${PWD}
TARGET_FOLDER="/home/leo/tmp/statistik"
COMBINEPGM="${PWD}/build/install/combineStatFiles/bin/combineStatFiles"
# ------------------------------------------------------------------------------------

BASEFILENAMEPROD="SLL-PROD_2020-05-"
BASEFILENAMEQA="SLL-QA_2020-05-"

DAYLIST="01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31"

cd $PATH_TO_STAT_FILES

for day in $DAYLIST
do
  $COMBINEPGM $TARGET_FOLDER "$PATH_TO_STAT_FILES"/$BASEFILENAMEQA$day*
  $COMBINEPGM $TARGET_FOLDER "$PATH_TO_STAT_FILES"/$BASEFILENAMEPROD$day*
done

