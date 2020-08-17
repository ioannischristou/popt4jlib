'''
short python script reads two files containing "labels" represented by
numbers, rounds them up, and prints the confusion matrix between the 
two. It also requires to enter as third input argument the number of
classes present (it's faster this way.)
'''

import sys
import math
import numpy as np

from itertools import (takewhile,repeat)

def rawincount(filename):
	with open(filename, 'rb') as f:
		bufgen = takewhile(lambda x: x, (f.raw.read(1024*1024) for _ in repeat(None)))
		return sum( buf.count(b'\n') for buf in bufgen )


f1_str = sys.argv[1]
f2_str = sys.argv[2]
n = int(sys.argv[3])

f1 = open(f1_str, "r")
f2 = open(f2_str, "r")

matrix = np.zeros((n,n),dtype=int)

for line1 in f1:
	line2 = f2.readline()
	n1 = int(round(float(line1.strip())))
	n2 = int(round(float(line2.strip())))
	matrix[n1,n2] += 1
for i in range(n):
	for j in range(n):
		print("%3d " % matrix[i,j], end='')
	print("")
