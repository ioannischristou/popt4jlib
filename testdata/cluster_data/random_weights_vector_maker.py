if __name__ == '__main__':
  import random
  n = int(input('Enter # non-negative random numbers to generate:'))
  maxw = float(input('Enter maximum (positive) weight value:'))
  fname = input('Enter filename to write numbers onto:')
  f = open(fname, 'w')
  for i in range(n):
    r = random.random()*maxw
    if r <= 0:
      r = maxw
    f.write(str(r)+'\n')
  f.flush()
  f.close()
 
