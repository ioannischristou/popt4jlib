package popt4jlib.MSSC1D;

import java.io.*;
import java.util.*;
import cern.colt.matrix.DoubleMatrix1D;


public class Params {
  private double[] _array;  // the array to optimally cluster (must be sorted
                            // in asc. order)
  private SortAux[] _sa;  // aux array that will be sorted. The index component
                          // of each SortAux obj is the position of the value
                          // in the original array
  private double _p;  // any cluster must have a sum of distances from
                             // its center that is less then _p
  private int _M;  // max. number of clusters to create

  private int _metric=0;  // 0 -> L1 norm
                          // 1 -> L2 norm

  public static final int _L1 = 0;
  public static final int _L2 = 1;


  public Params(double[] arr, double p, int m) {
    _p = p;
    _M = m;
    if (arr!=null) sortArray(arr);
  }


  public Params(double[] arr, double p, int k, int metric) {
    _p = p;
    _M = k;
    if (arr!=null) sortArray(arr);
    _metric = metric;
  }


  public Params(DoubleMatrix1D arr, double p, int m) {
    _p = p;
    _M = m;
    if (arr!=null) sortArray(arr);
  }


  public Params(DoubleMatrix1D arr, double p, int k, int metric) {
    _p = p;
    _M = k;
    if (arr!=null) sortArray(arr);
    _metric = metric;
  }


  void readTestData(int n, int k, int step, double gapm) {
    // read the array from a file.
    // for now set values here
/*
    _array = new double[]{1, 3, 5, 8, 12, 13, 14, 16, 17, 31, 32, 40, 50, 51, 52, 54, 57, 65, 70, 80,
                          81, 90, 100, 103, 108, 150, 160, 165, 170, 175, 180, 190, 191, 200, 201,
                          300, 301, 302, 310, 320 };
*/
    double[] arr = new double[n];
    double cm = 0;
    double gap = 10;
    for (int i=0; i<n; i++) {
      if (i % step == 0) { gap *= gapm; }
      arr[i] = cm + Math.floor(Math.random() * gap);
      // cm = arr[i];  // by commenting this line out, arr[] is NOT sorted
    }
    // itc: HERE rm next print out ASAP
    System.out.print("array  =");
    for (int i=0; i<arr.length; i++) {
      System.out.print(arr[i]+", ");
    }

    _p = Double.MAX_VALUE;
    _M = k;
    sortArray(arr);
  }


  public double getSequenceValueAt(int i) { return _array[i]; }


  public int getSequenceLength() { return _array.length; }


  public int getOriginalIndexAt(int i) {
    return _sa[i]._i;
  }


  public double getP() { return _p; }


  public int getM() { return _M; }


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
