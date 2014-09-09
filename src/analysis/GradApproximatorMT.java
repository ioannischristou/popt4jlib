package analysis;

import popt4jlib.*;
import parallel.TaskObject;
import parallel.ParallelException;
import parallel.distributed.PDBatchTaskExecutor;
import java.util.*;
import java.io.*;

/**
 * The class implements a multi-threaded version of the Richardson extrapolation
 * method to evaluate the derivative of a differentiable function (serial
 * version implemented in GradApproximator class of this package).
 * Note1: unfortunately, this implementation does not have a way of checking
 * when an nmax value in the Richardson recursion is "too much" or "too little".
 *  Note2: there is currently no way to check if the values returned are
 * "close" to the real derivative value, or even if the derivative exists.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class GradApproximatorMT implements VecFunctionIntf {
  private FunctionIntf _f=null;

  /**
   * public constructor.
   * @param f FunctionIntf
   */
  public GradApproximatorMT(FunctionIntf f) {
    _f = f;
  }


  /**
   * compute the gradient of the function f passed in the constructor, at the
   * point x using Richardson extrapolation. The params hash-table may contain
   * the following pairs:
   * <li> <"gradapproximator.nmax", Integer n> optional, the dimension of the
   * Richardson extrapolation. Default is 10.
   * <li> <"gradapproximator.numthreads", Integer nt> optional, the number of
   * threads to use when computing the gradient. Default is 1.
   * @param x VectorIntf the point at which the gradient must be evaluated.
   * @param params Hashtable any parameters to be passed to the function f
   * @throws IllegalArgumentException if x is null or if the algorithm fails
   * @return VectorIntf
   */
  public VectorIntf eval(VectorIntf x, Hashtable params) throws IllegalArgumentException {
    if (x==null) throw new IllegalArgumentException("null arg passed");
    final int n = x.getNumCoords();
    int nt = 1;
    try {
      Integer ntI = (Integer) params.get("gradapproximator.numthreads");
      if (ntI!=null && ntI.intValue()>0)
        nt = ntI.intValue();
      if (nt>n) nt = n;  // cannot have more threads than dimensions
    }
    catch (ClassCastException e) {
      e.printStackTrace();  // ignore
    }
    PDBatchTaskExecutor executor = null;
    try {
      executor = new PDBatchTaskExecutor(nt);
    }
    catch (ParallelException e) {
      e.printStackTrace();  // can never happen
    }

    VectorIntf grad = x.newCopy();
    int wlength = n / nt;
    int start = 0;
    Vector tasks = new Vector();  // Vector<GradApproxTaskObject>
    for (int i=0; i<nt-1; i++) {
      GradApproxTaskObject to = new GradApproxTaskObject(x, _f, params, start, start+wlength-1);
      tasks.addElement(to);
      start += wlength;
    }
    GradApproxTaskObject fin = new GradApproxTaskObject(x, _f, params, start, n-1);
    tasks.addElement(fin);
    try {
      Vector results = executor.executeBatch(tasks);
      for (int i=0; i<results.size(); i++) {
        try {
          GradApproxTaskObject ti = (GradApproxTaskObject) results.elementAt(i);
          if (ti.isDone()==false)  // ensure no thread-local values persist
            throw new IllegalArgumentException("GradApproximatorMT.eval(): executor.executeBatch(): the "+
                                               i+"-th partial derivative failed");
          else {
            for (int k=ti._start; k<=ti._end; k++) grad.setCoord(k, ti._gradf.getCoord(k));
          }
        }
        catch (ClassCastException e) {
          e.printStackTrace();
          throw new IllegalArgumentException("GradApproximatorMT.eval(): executor.executeBatch(): the "+
                                             i+"-th partial derivative failed");
        }
      }
      executor.shutDown();
      return grad;
    }
    catch (ParallelException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("GradApproximatorMT.eval(): executor.executeBatch(): failed");
    }
  }


  /**
   * evaluate the partial derivative of the "coordindex"-th variable at x.
   * @param x VectorIntf
   * @param params Hashtable
   * @param coordindex int
   * @throws IllegalArgumentException
   * @return double
   */
  public double evalCoord(VectorIntf x, Hashtable params, int coordindex) throws IllegalArgumentException {
    if (x==null) throw new IllegalArgumentException("null arg passed");
    // final int n = x.getNumCoords();
    VectorIntf xc = x.newCopy();  // work with copy so as to avoid memory
    // corruption issues in multi-threaded accesses of the vector
    double h = 1.0;
    int nmax = 10;
    Integer nmI = (Integer) params.get("gradapproximator.nmax");
    if (nmI!=null && nmI.intValue()>0)
      nmax = nmI.intValue();
    double[][] d = new double[nmax][nmax];
    int k = coordindex;
    h = 1.0;  // reset h value for each partial derivative
    try {
      for (int i = 0; i < nmax; i++) {
        double xk = xc.getCoord(k);
        xc.setCoord(k, xk + h);
        double fphk = _f.eval(xc, params);
        xc.setCoord(k, xk - h);
        double fmhk = _f.eval(xc, params);
        double gk = (fphk - fmhk) / (2.0 * h);
        xc.setCoord(k, xk); // reset to normal value
        d[i][0] = gk;
        for (int j = 0; j <= i - 1; j++) {
          d[i][j + 1] = d[i][j] +
              (d[i][j] - d[i - 1][j]) / (Math.pow(4.0, j + 1.0) - 1.0);
        }
        h = h / 2.0;
      }
    }
    catch (ParallelException e) {  // can never get here
      e.printStackTrace();
    }
    return d[nmax-1][nmax-1];
  }

}


class GradApproxTaskObject implements TaskObject {
  private final static long serialVersionUID = -197598046273744656L;
  private VectorIntf _x;
  private FunctionIntf _f;
  private Hashtable _params;
  private boolean _isDone=false;
  VectorIntf _gradf;
  int _start;
  int _end;


  GradApproxTaskObject(VectorIntf x, FunctionIntf f, Hashtable params, int start, int end) {
    _x = x.newCopy();  // work with copy so as to avoid memory corruption issues
                       // in multi-threaded accesses of the vector
    _f = f;
    _gradf = x.newCopyMultBy(0);
    _start = start;
    _end = end;
    _params = params;
  }


  public Serializable run() {
    double h = 1.0;
    int nmax = 10;
    Integer nmI = (Integer) _params.get("gradapproximator.nmax");
    if (nmI!=null && nmI.intValue()>0) nmax = nmI.intValue();
    double[][] d = new double[nmax][nmax];
    System.err.println("Thread computing partial derivatives in ["+_start+", "+_end+"]");
    for (int k=_start; k<=_end; k++) {
      h = 1.0;
      try {
        for (int i = 0; i < nmax; i++) {
          double xk = _x.getCoord(k);
          _x.setCoord(k, xk + h);
          double fphk = _f.eval(_x, _params);
          _x.setCoord(k, xk - h);
          double fmhk = _f.eval(_x, _params);
          double gk = (fphk - fmhk) / (2.0 * h);
          _x.setCoord(k, xk); // reset to normal value
          d[i][0] = gk;
          for (int j = 0; j <= i - 1; j++) {
            d[i][j + 1] = d[i][j] +
                (d[i][j] - d[i - 1][j]) / (Math.pow(4.0, j + 1.0) - 1.0);
          }
          h = h / 2.0;
        }
      }
      catch (ParallelException e) {  // can never get here
        e.printStackTrace();
      }
      System.err.println("Setting "+k+"-th coordinate of the gradient to "+ d[nmax-1][nmax-1]);
      try {
        _gradf.setCoord(k, d[nmax - 1][nmax - 1]);
      }
      catch (ParallelException e) {  // can never get here
        e.printStackTrace();
      }
    }
    synchronized (this) {  // write results to main memory
      _isDone = true;
    }
    return this;
  }


  public synchronized boolean isDone() {  // when queried, ensures subsequent
                                          // values are read from main memory
    return _isDone;
  }


  public void copyFrom(TaskObject other) throws IllegalArgumentException {
    throw new IllegalArgumentException("GradApproxTaskObject.copyFrom(other) operation not supported");
  }
}

