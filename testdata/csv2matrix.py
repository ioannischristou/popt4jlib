'''
short script to convert a "standard" csv file describing a training
data set to "matrix" format suitable for reading by popt4jlib DataMgr
method readMatrixFromFile(filename), optionally together with its 
associated labels result file --that can then be read by popt4jlib method DataMgr.readDoubleLabelsFromFile(filename).
'''

import csv

savelbl_gl = False  # great coding practice: global variables...

def convert(input_csv_file, output_matrix_file, header=True, exclude_columns=None):
	data_rows = []
	num_cols = -1
	with open(input_csv_file) as csvfile:
		csv_reader = csv.reader(csvfile, delimiter=',')
		line_count = 0
		header_attrs = []
		for row in csv_reader:
			if line_count == 0 and header is True:
				header_attrs = row
				num_cols = len(row)
				line_count += 1
			else:
				if len(row)>0:
					num_cols = len(row)
					data_rows.append(row)
	# now let's write the file
	labels_file = None
	if savelbl_gl:
		labels_file = open(output_matrix_file+".lbls",mode='w')
	with open(output_matrix_file, mode='w') as outfile:
		csv_writer = csv.writer(outfile, delimiter=' ')
		row0 = [len(data_rows), num_cols-len(exclude_columns)]
		csv_writer.writerow(row0)
		for row in data_rows:
			L = []
			for i in range(len(row)):
				# exclude columns?
				if (i+1) in exclude_columns: 
					if savelbl_gl:
						labels_file.write(row[i]+'\n')
					continue
				if (float(row[i])==0): continue
				L.append(str(i+1)+","+row[i])
			csv_writer.writerow(L)
	if savelbl_gl:
		labels_file.close()

if __name__=="__main__":
	inpf = input("Enter csv file name to convert to popt4jlib matrix format:")
	outf = input("Enter output matrix file name:")
	exclude_str = input("Enter any exclude columns in {1,...#num_cols} separated by comma:")
	Li=None
	if len(exclude_str)>0:
		L = exclude_str.split(",")
		Li = [int(s) for s in L]
		if len(Li)==1:
			save_label = input("Save exclude column as labels file? (y/n)")
			if save_label is 'y':
				savelbl_gl = True
	convert(inpf, outf, exclude_columns=Li)
