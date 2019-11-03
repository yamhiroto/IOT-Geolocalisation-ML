#!/bin/bash
d="$1"
[ -d "${d}" ] &&  echo "Directory $d found." || echo "Directory $d not found."