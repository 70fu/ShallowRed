#!/usr/bin/env python3
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
import tempfile
from pathlib import Path
from subprocess import Popen, PIPE
import json

#OPTIONS TO CONFIGURE

jvmOptions="-Xmx2g"

#path to ShallowRedCLI jar file
pathToJarFile = str(Path("..","out","artifacts","ShallowRedCLI","shallowred_main.jar").resolve())

computingTime=2
enemyAI="SHALLOWRED"
#loads this config for the ShallowRed ai and changes it by the parameter values provided by CLOP
baseConfigPath = Path("BaseConfig.json").resolve()
enemyConfigPath = str(Path("CLOPEnemy.json").resolve())
#create a log directory in the temp directory of the os
logDirectory=str(Path(tempfile.gettempdir(), "ShallowRedCLOP").resolve())
playOptions="--randomBoard --switchSides --repeatOnError --repeatOnSameSideWin"

def is_float(value):
  try:
    float(value)
    return True
  except:
    return False

def is_int(value):
  try:
    int(value)
    return True
  except:
    return False

#player config as json string enclosed with " and " have backslashes in json string
def constructCommandString(playerConfig):
	result = "java " + jvmOptions + " -jar " + pathToJarFile + " play"
	result += " " + playOptions
	result += " -t " + str(computingTime)
	result += " -p " + playerConfig
	result += " --enemyConfigFile " + enemyConfigPath
	result += " -l " + logDirectory
	
	result += " SHALLOWRED"
	#enemy
	result += " " + enemyAI
	
	return result

def argumentsToJSONConfig(arguments):
	with baseConfigPath.open() as file:
		config = json.load(file)

	for i in range(0,len(arguments), 2):
		#split parameter name with / and use it as path
		dictV=config
		pathList = arguments[i].split('/')
		for key in pathList[:-1]:
			dictV = dictV[key]

		#set value, try to convert to number
		value = arguments[i+1]
		if is_float(value) and not is_int(value):
			value = float(value)
		elif is_int(value):
			value = int(value)
		dictV[pathList[-1]] = value

	configString = json.dumps(config)
	return "\"" + configString.replace('"', '\\"') + "\""

def main(argv = None):
	if argv is None:
		argv = sys.argv[1:]

	if len(argv) == 0 or argv[0] == '--help':
		sys.stdout.write(__doc__)
		return 0

	#cut off processor name, not needed
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
	#cut off clop seed
	argv=argv[1:]
	
	command = constructCommandString(argumentsToJSONConfig(argv))
	# Run command and wait for it to finish
	process = Popen(command, shell=True, stdout=PIPE, universal_newlines=True)
	output = process.communicate()[0]
	if process.returncode != 0:
		sys.stderr.write('failed to execute command: %s\n' % command)
		return 2

	sys.stdout.write(output)

	return 0

if __name__ == "__main__":
	sys.exit(main())