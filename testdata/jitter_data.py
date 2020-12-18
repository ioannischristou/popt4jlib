# reads a CSV file, jitters every value by white noise with specified sigma,
# and writes back results to another specified CSV file.
# Preserves non-number values

import random


def jitter_csv(input_file, sigma, output_file):
    fin = open(input_file, 'r')
    fout = open(output_file, 'w')
    for line in fin:
        data = line.strip().split(',')
        for i in range(len(data)):
            d = data[i]
            dstr = d.strip()
            try:
                n = int(dstr)
                fout.write(dstr)
            except ValueError:
                try: 
                    num = float(dstr)
                    num += random.gauss(0, sigma)
                    fout.write(str(num))
                except ValueError:
                    fout.write(d)
            if i < len(data)-1:
                fout.write(',')
        fout.write('\n')
    fout.close()
    fin.close()


if __name__ == '__main__':
    fi = input("Enter input file name: ")
    fo = input("Enter output file name: ")
    s =  input("Enter sigma: ")
    jitter_csv(fi, float(s), fo)
    print("Done.")
