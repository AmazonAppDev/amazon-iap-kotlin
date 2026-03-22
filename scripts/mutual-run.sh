#!/bin/bash
set -e

echo "Starting Mutual Agent System..."

TASK=$1

if [ -z "$TASK" ]; then
  echo "Usage: mutual-run.sh <task_file>"
  exit 1
fi

echo "DOT thinking..."
cat .ai/DOT.md
cat "$TASK"

echo "ECHO reviewing..."
cat .ai/ECHO.md

echo "Loop until approved."
