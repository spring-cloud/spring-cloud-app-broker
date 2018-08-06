#!/usr/bin/env python3
# I couldn't get this working with sed so python it is!
import fileinput
import os
import sys

script_path = os.path.dirname(os.path.realpath(sys.argv[0]))
ci_dir = os.path.dirname(script_path)

with fileinput.FileInput(os.path.join(ci_dir, 'bbl-bosh-lite-environment/bbl-state/bbl-state.json'), inplace=True) as file:
    for line in file:
        print(line.replace('\\\\n', '\\n'), end='')