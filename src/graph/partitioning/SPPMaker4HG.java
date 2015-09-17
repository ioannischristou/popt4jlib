package graph.partitioning;

import graph.HGraph;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import java.util.*;


/**
 * creates SPPMakerIntf objects that set-up the A matrix and c cost vector in
 * a set partitioning problem (SPP) of n variables of the form
 * min c'x such that Ax=e, x Binary.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SPPMaker4HG implements SPPMakerIntf {
  protected int _k;
  protected HashMap _params;
  protected DoubleMatrix2D _A;
  protected DoubleMatrix1D _c;
  protected HObjFncIntf _objfnc;

  public SPPMaker4HG() {
    _params = new HashMap();
  }


  public SPPMaker4HG(HashMap p) {
    _params = new HashMap(p);
  }

  /**
   * partitions is a Vector<int[] partition>
   * Each partition index starts at 1, and is in the range [1,k] inclusive.
   * @param g HGraph
   * @param partitions Vector
   * @param k int
   * @param obj HObjFncIntf
   * @throws PartitioningException
   */
  public void createSPP(HGraph g, Vector partitions, int k, HObjFncIntf obj)
      throws PartitioningException {
    try {
      final int rows = g.getNumNodes();
      final int cols = partitions.size()*k;
      _A = new SparseDoubleMatrix2D(rows, cols);
      _c = new DenseDoubleMatrix1D(cols);
      _objfnc = obj;
      // initialize
      for (int j=0; j<cols; j++) {
        _c.setQuick(j, 0.0);
        for (int i=0; i<rows; i++)
          _A.setQuick(i,j,0.0);
      }
      // set values for _A
      for (int i=0; i<partitions.size(); i++) {
        int[] partition = (int[]) partitions.elementAt(i);
        // create the k columns [ i*k ... ik+k-1 ] corresponding to this partition
        for (int r=0; r<rows; r++) {
          int pr = partition[r]-1;  // partition indices start at 1
          int pos = i*k+pr;
          _A.set(r, pos, 1.0);
        }
      }
      // set values for _c
      for (int i=0; i<partitions.size(); i++) {
        int[] partition = (int[]) partitions.elementAt(i);
        double[] vals = _objfnc.values(g, partition);
        for (int j=0; j<k; j++) {
          _c.set(i*k+j, vals[j]);
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PartitioningException("createSPP() failed");
    }
  }


  public DoubleMatrix1D getCostVector() throws PartitioningException {
    if (_c==null) throw new PartitioningException("no cost vector");
    return _c;
  }


  public DoubleMatrix2D getConstraintsMatrix() throws PartitioningException {
    if (_A==null) throw new PartitioningException("no constraints matrix");
    return _A;
  }


  public void setParam(String param, Object val) {
    _params.put(param, val);
  }


  public Object getParam(String param) {
    return _params.get(param);
  }


  public void addParams(HashMap params) {
    _params.putAll(params);
  }

}

