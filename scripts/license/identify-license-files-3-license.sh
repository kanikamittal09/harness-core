#!/bin/bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
#
# Produces source files intended for use with add_license_header.sh
# This is the "3 license" variation.
# It produces 3 files, each file lists the source files which should be stamped with the associated header.
#   sources-apache.txt      ->  Apache GPL2
#   sources-free-trial.txt  ->  Polyform Free Trial
#   sources-shield.txt      ->  Polyform Shield
#

POLYFORM_SHIELD_FILE=".license-files-polyform-shield.txt"
POLYFORM_FREE_TRIAL_FILE=".license-files-polyform-free-trial.txt"
SUPPORTED_EXTENSIONS_FILE=".license-extensions.txt"

FILE_EXTENSIONS=$(awk 'NR>1 {print $1}' "$SUPPORTED_EXTENSIONS_FILE" | paste -s -d '|' -)

###############################
## Validate file lists       ##
###############################
function print_possible_alternates {
  POTENTIAL_ALTERNATE=$(find . -name "$(basename "$1")")
  if [ ! -z "$POTENTIAL_ALTERNATE" ]; then
    echo "Has the file been moved to         $POTENTIAL_ALTERNATE"
  fi
}

function validate_missing {
  MISSING_FILES=$(sort -f "$1" | uniq | xargs ls 2>&1 | grep 'No such file')
  if [ ! -z "$MISSING_FILES" ]; then
    while read -r LINE; do
      FILE=$(echo "$LINE" | awk 'BEGIN{FS=": "}{print $2}')
      echo "ERROR: Skipping file as it does not exist $FILE"
      print_possible_alternates "$FILE"
      echo
    done <<< "$MISSING_FILES"
    echo ""
    echo "$1 contains non-existent files."
    exit 2
  fi
}

validate_missing "$POLYFORM_SHIELD_FILE"
validate_missing "$POLYFORM_FREE_TRIAL_FILE"

###############################
## Polyform Shield Files     ##
###############################
sort -f "$POLYFORM_SHIELD_FILE" | uniq | grep -v '/src/test/' > sources-shield.txt

###############################
## Apache Files              ##
###############################

function query_source_files {
  bazel query "deps($1)" 2>/dev/null | grep -v '^@' | tr ':' '/' | cut -c 3- | grep -E "\.(${FILE_EXTENSIONS})$" | xargs ls 2>/dev/null | sort -f | uniq
}

# identify source files required to build CD
FILE_EXTENSIONS=$(awk 'NR>1 {print $1}' "$SUPPORTED_EXTENSIONS_FILE" | paste -s -d '|' -)
query_source_files '//120-ng-manager/container:ng_manager'             > 120.txt
query_source_files '//260-delegate:module_deploy.jar'                  > 260.txt
query_source_files '//360-cg-manager/container:manager'                > 360.txt
query_source_files '//800-pipeline-service/container:pipeline_service' > 800.txt
query_source_files '//820-platform-service/container:platform_service' > 820.txt
query_source_files '//960-watcher:module_deploy.jar'                   > 960.txt

# remove files identified for more restrictive license
cat 120.txt 260.txt 360.txt 800.txt 820.txt 960.txt | sort -f | uniq | comm -23i - "$POLYFORM_FREE_TRIAL_FILE" | comm -23i - sources-shield.txt > sources-apache-before-tests-are-added.txt
rm 120.txt 260.txt 360.txt 800.txt 820.txt 960.txt

# identify matching Java unit tests
grep -E '\.java$' sources-apache-before-tests-are-added.txt | sed -e 's#/src/main/#/src/test/#' -e 's/\.java$/Test.java/' | xargs ls 2>/dev/null > sources-apache-found-tests.txt

# combine build and test files as final Apache license files
cat sources-apache-before-tests-are-added.txt sources-apache-found-tests.txt | grep -v '/src/test/' | sort -f | uniq | comm -23i - "$POLYFORM_FREE_TRIAL_FILE" | comm -23i - sources-shield.txt > sources-apache.txt
rm sources-apache-before-tests-are-added.txt sources-apache-found-tests.txt

###############################
## Polyform Free Trial Files ##
###############################

# find all files which should receive header
find . -type f | grep -v '/src/test/' | grep -E "\.($(awk 'NR>1 {print $1}' "$SUPPORTED_EXTENSIONS_FILE" | paste -s -d '|' -))$" | cut -c 3- | sort -f | uniq > sources-all.txt

# remove Apache and Polyform Shield files
comm -23i sources-all.txt sources-apache.txt | comm -23i - sources-shield.txt > sources-free-trial.txt

###############################
## Identify all files within directories ##
###############################

function find_within_dir {
  DIRECTORIES=$(awk '{gsub(/\/[^\/]*$/, ""); print}' "$1" | sort -f | uniq)
  LAST_DIR=""
  test -e sources-all-within-dir.txt && rm sources-all-within-dir.txt
  while read -e DIR; do
    if [ $(grep -m 1 -cE "^$LAST_DIR/" <<< "$DIR") -eq 0 ]; then
      find "$DIR" -type f | grep -E "\.($FILE_EXTENSIONS)$" >> sources-all-within-dir.txt
      LAST_DIR="$DIR"
    fi
  done <<< "$DIRECTORIES"

  sort -f sources-all-within-dir.txt | uniq
}

find_within_dir sources-shield.txt | grep -v '/src/test/' | sort -f | uniq > sources-shield-all-within-dir.txt
find_within_dir sources-free-trial.txt | grep -v '/src/test/' | sort -f | uniq > sources-free-trial-final.txt

cat sources-shield-all-within-dir.txt sources-free-trial-final.txt | sort -f | uniq | comm -23i sources-apache.txt - > sources-apache-final.txt
comm -23i sources-shield-all-within-dir.txt sources-free-trial-final.txt > sources-shield-final.txt

rm sources-all-within-dir.txt sources-shield-all-within-dir.txt
mv sources-apache-final.txt sources-apache.txt
mv sources-shield-final.txt sources-shield.txt
mv sources-free-trial-final.txt sources-free-trial.txt

###############################
## Compute Counts            ##
###############################

FILE_COUNTS=$(wc -l sources-apache.txt sources-free-trial.txt sources-shield.txt)

# sanity check file counts
FILE_COUNT_SUM_OF_EACH_LICENSE=$(grep total <<< "$FILE_COUNTS" | awk '{print $1}')
FILE_COUNT=$(wc -l sources-all.txt | awk '{print $1}')
if [ "$FILE_COUNT_SUM_OF_EACH_LICENSE" -ne "$FILE_COUNT" ]; then
  echo "ERROR: File counts do not match!"
  echo "Sum of separate licenses: $FILE_COUNT_SUM_OF_EACH_LICENSE"
  echo "All licensed files: $FILE_COUNT"
  exit 1
fi

# print file and line counts
echo "File Count:"
echo "$FILE_COUNTS"
echo
echo "Line Count:"
LINE_COUNT_APACHE=$(cat sources-apache.txt | tr \\n \\0 | xargs -0 wc -l | grep total | awk '{print $1}' | paste -s -d '+' - | bc)
LINE_COUNT_FREE_TRIAL=$(cat sources-free-trial.txt | tr \\n \\0 | xargs -0 wc -l | grep total | awk '{print $1}' | paste -s -d '+' - | bc)
LINE_COUNT_SHIELD=$(cat sources-shield.txt | tr \\n \\0 | xargs -0 wc -l | grep total | awk '{print $1}' | paste -s -d '+' - | bc)
printf "%8d Apache\n" "${LINE_COUNT_APACHE}"
printf "%8d Polyform Free Trial\n" "${LINE_COUNT_FREE_TRIAL}"
printf "%8d Polyform Shield\n" "${LINE_COUNT_SHIELD}"
printf "%8d total\n" "$(echo $LINE_COUNT_APACHE+$LINE_COUNT_FREE_TRIAL+$LINE_COUNT_SHIELD | bc)"

#rm sources-all.txt sources-apache.txt sources-free-trial.txt sources-shield.txt