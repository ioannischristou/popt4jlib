'''
short python script reads two files containing "labels" represented by
numbers, computes the difference of each number in each line in the first
file from the corresponding number in the second file, squares the 
difference, sums them all up, and returns the square root of the sum.
'''

import sys
import math

f1_str = sys.argv[1]
f2_str = sys.argv[2]

f1 = open(f1_str, "r")
f2 = open(f2_str, "r")

sum = 0.0
i = 1
for line1 in f1:
	line2 = f2.readline()
	n1 = float(line1.strip())
	n2 = float(line2.strip())
	diff2 = (n1-n2)*(n1-n2)
	print("line-%d diff=%f" % (i,n1-n2))
	i += 1
	sum += diff2
sum = math.sqrt(sum)
print("L2-norm of v1-v2 = %f" % sum)
