'''
short script to convert a "standard" csv file describing a training
data set to "matrix" format suitable for reading by popt4jlib DataMgr
method readMatrixFromFile(filename), optionally together with its 
associated labels result file --that can then be read by popt4jlib method 
DataMgr.readDoubleLabelsFromFile(filename).
'''

import csv

savelbl_gl = False         # great coding practice: global variables...
convertlbls2cat_gl = False  # likewise

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
		if convertlbls2cat_gl:
			labels_list=[]
	with open(output_matrix_file, mode='w') as outfile:
		csv_writer = csv.writer(outfile, delimiter=' ')
		row0 = [len(data_rows), num_cols-len(exclude_columns)]
		csv_writer.writerow(row0)
		for row in data_rows:
			L = []
			j = 0
			for i in range(len(row)):
				# exclude columns?
				if (i+1) in exclude_columns: 
					if savelbl_gl:
						if not convertlbls2cat_gl:
							labels_file.write(row[i]+'\n')
						else:
							labels_list.append(float(row[i]))
					continue
				j += 1
				if (float(row[i])==0): continue  # sparse format allows skipping zeros
				#L.append(str(i+1)+","+row[i])
				L.append(str(j)+","+row[i])
			csv_writer.writerow(L)
	if savelbl_gl:
		if convertlbls2cat_gl:
			# do the work now, and write labels 0, 1, 2, ... and map file
			unique_lbls_set = set(labels_list)
			sorted_unique_labels = sorted(unique_lbls_set)
			# print("sorted unique labels:", sorted_unique_labels)  ## itc: HERE rm asap
			labels_map_file = open(output_matrix_file+".lbls.map.txt",mode='w')
			mp = dict()
			for i in range(len(sorted_unique_labels)):
				mp[sorted_unique_labels[i]] = i
				labels_map_file.write(str(sorted_unique_labels[i])+' , '+str(i)+'\n')
			labels_map_file.close()
			# finally, write the labels
			for lbl in labels_list:
				labels_file.write(str(mp[lbl])+'\n')
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
				convert_label = input("Convert labels to categorical (0, 1, 2, ...)? (y/n)")
				if convert_label is 'y':
					convertlbls2cat_gl = True
	convert(inpf, outf, exclude_columns=Li)
