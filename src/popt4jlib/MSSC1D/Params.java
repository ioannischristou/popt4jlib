package popt4jlib.MSSC1D;

import java.io.*;
import java.util.*;
import cern.colt.matrix.DoubleMatrix1D;


/**
 * auxiliary class that implements a container to hold the MSSC1D problem 
 * inputs and various auxiliary data-structures.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Params {
  private double[] _array;  // the array to optimally cluster (must be sorted
                            // in asc. order)
  private SortAux[] _sa;  // aux array that will be sorted. The index component
                          // of each SortAux obj is the position of the value
                          // in the original array
  private double _p;  // any cluster must have a sum of distances from
                      // its center that is less than _p
  private int _M;  // max. number of clusters to create

  private int _metric=0;  // 0 -> L1 norm
                          // 1 -> L2 norm

  public static final int _L1 = 0;
  public static final int _L2 = 1;


	/**
	 * 3-argument public constructor. Distance metric to be used is the L1 norm.
	 * @param arr double[] the sequence to be clustered. Doesn't have to be sorted 
	 * in ascending order, and is not modified in any way.
	 * @param p double the maximum value for the sum of intra-distances of points
	 * from their center for any cluster. May be Double.MAX_VALUE to indicate no
	 * constraint.
	 * @param m int the number of clusters required.
	 */
  public Params(double[] arr, double p, int m) {
    _p = p;
    _M = m;
    if (arr!=null) sortArray(arr);
  }


	/**
	 * 4-argument public constructor, is like the 3-arg constructor, but also 
	 * specifies in its last argument, the distance metric to be used.
	 * @param arr double[] the sequence to be clustered. Doesn't have to be sorted 
	 * in ascending order, and is not modified in any way.
	 * @param p double the maximum value for the sum of intra-distances of points
	 * from their center for any cluster. May be Double.MAX_VALUE to indicate no
	 * constraint.
	 * @param k int the number of clusters required.
	 * @param metric int 0 indicates the L1-norm (Manhattan distance), 1 indicates
	 * the L2-norm (Euclidean distance).
	 */
  public Params(double[] arr, double p, int k, int metric) {
    _p = p;
    _M = k;
    if (arr!=null) sortArray(arr);
    _metric = metric;
  }


	/**
	 * 3-argument public constructor for COLT 1D arrays. Distance metric is set to
	 * the L1 norm.
	 * @param arr DoubleMatrix1D
	 * @param p double as in double[]-based 3-argument constructor.
	 * @param m int as in double[]-based 3-argument constructor.
	 */
  public Params(DoubleMatrix1D arr, double p, int m) {
    _p = p;
    _M = m;
    if (arr!=null) sortArray(arr);
  }


	/**
	 * 4-argument public constructor for COLT 1D arrays.
	 * @param arr DoubleMatrix1D
	 * @param p double as in double[]-based 4-argument constructor.
	 * @param k int as in double[]-based 4-argument constructor.
	 * @param metric int as in double[]-based 4-argument constructor.
	 */
  public Params(DoubleMatrix1D arr, double p, int k, int metric) {
    _p = p;
    _M = k;
    if (arr!=null) sortArray(arr);
    _metric = metric;
  }


	/**
	 * reads test data for debugging purposes.
	 * @param n int length of the array
	 * @param k int number of clusters sought
	 * @param step int used in a random number creation experiment
	 * @param gapm double also used in a random number creation experiment.
	 */
  void readTestData(int n, int k, int step, double gapm) {
    // read the array from a file.
		double[] arr=null;
		try {
			BufferedReader br = new BufferedReader(new FileReader("test_MSSC1D_input.txt"));
	    arr = new double[n];  // all elems init to zero
			int i=0;
			while (true) {
				String line = br.readLine();
				if (line==null) break;  // EOF
				StringTokenizer st = new StringTokenizer(line);
				while (st.hasMoreTokens()) {
					double v = Double.parseDouble(st.nextToken());
					if (i<n) arr[i] = v;
					++i;
				}
			}
			System.err.println("read total of "+i+" numbers, stored "+n+" in array.");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
/*
    // for now set values here
    _array = new double[]{1, 3, 5, 8, 12, 13, 14, 16, 17, 31, 32, 40, 50, 51, 52, 54, 57, 65, 70, 80,
                          81, 90, 100, 103, 108, 150, 160, 165, 170, 175, 180, 190, 191, 200, 201,
                          300, 301, 302, 310, 320 };
*/
/*
    double cm = 0;
    double gap = 10;
    for (int i=0; i<n; i++) {
      if (i % step == 0) { gap *= gapm; }
      arr[i] = cm + Math.floor(Math.random() * gap);
      // cm = arr[i];  // by commenting this line out, arr[] is NOT sorted
    }
*/
		
		System.out.print("array = ");
    for (int i=0; i<arr.length; i++) {
      System.out.print(arr[i]+", ");
    }
		
    _p = Double.MAX_VALUE;
    _M = k;
    sortArray(arr);
  }


	/**
	 * return the i-th value of the sorted sequence of data.
	 * @param i int
	 * @return double
	 */
  public double getSequenceValueAt(int i) { return _array[i]; }


	/**
	 * return the length of the data array.
	 * @return int
	 */
  public int getSequenceLength() { return _array.length; }


	/**
	 * return the position of the value in the i-th element of the sorted array in
	 * the original data array.
	 * @param i int
	 * @return int
	 */
  public int getOriginalIndexAt(int i) {
    return _sa[i]._i;
  }


	/**
	 * return the value of the intra-cluster sum of distances constraint.
	 * @return double
	 */
  public double getP() { return _p; }


	/**
	 * return the number of clusters required.
	 * @return int
	 */
  public int getM() { return _M; }


	/**
	 * return the metric used in clustering.
	 * @return int 0 indicates L1-norm, 1 indicates L2-norm.
	 */
  public int getMetric() { return _metric; }


  private void sortArray(double[] arr) {
    // sort arr
    _sa = new SortAux[arr.length];
    for (int i=0; i<arr.length; i++) {
      _sa[i] = new SortAux(i, arr[i]);
    }
    Arrays.sort(_sa);
    _array = new double[arr.length];
    for (int i=0; i<arr.length; i++) _array[i] = _sa[i]._v;
  }


  private void sortArray(DoubleMatrix1D arr) {
    // sort arr
    _sa = new SortAux[arr.size()];
    for (int i=0; i<arr.size(); i++) {
      _sa[i] = new SortAux(i, arr.getQuick(i));
    }
    Arrays.sort(_sa);
    _array = new double[arr.size()];
    for (int i=0; i<arr.size(); i++) _array[i] = _sa[i]._v;
  }

}
