#!/usr/bin/env python
#############################################################################
"""
 DummyScript.py

 This is an example script for use with parallel optimization. In order to
 apply clop algorithms to your own problem, you should write a script that
 behaves like this one.

 Arguments are:
  #1: processor id (symbolic name, typically a machine name to ssh to)
  #2: seed (integer)
  #3: parameter id of first parameter (symbolic name)
  #4: value of first parameter (float)
  #5: parameter id of second parameter (optional)
  #6: value of second parameter (optional)
  ...

 This script should write the game outcome to its output:
  W = win
  L = loss
  D = draw

 For instance:
  $ ./DummyScript.py node-01 4 param 0.2
  W
"""
#############################################################################
import sys
import math
import random
import time

#OPTIONS TO CONFIGURE
#path to ShallowRedCLI jar file
pathToJarFile = ""

computingTime=10

def main(argv = None):
    if argv is None:
        argv = sys.argv[1:]

    if len(argv) == 0 or argv[0] == '--help':
        sys.stdout.write(__doc__)
        return 0

    argv = argv[1:]
    if len(argv) < 3 or len(argv) % 2 == 0:
        sys.stderr.write('Too few arguments\n')
        return 2

    clop_seed = 0
    try:
        clop_seed = int(argv[0])
    except ValueError:
        sys.stderr.write('invalid seed value: %s\n' % argv[0])
		return 2

	# Run cutechess-cli and wait for it to finish
	#todo adopt
    process = Popen(command, shell = True, stdout = PIPE)
    output = process.communicate()[0]
    if process.returncode != 0:
        sys.stderr.write('failed to execute command: %s\n' % command)
		return 2
