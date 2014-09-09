package analysis;

import popt4jlib.VecFunctionIntf;
import popt4jlib.VectorIntf;
import popt4jlib.DblArray1Vector;
import java.util.Hashtable;

/**
 * implements the linear operator T(x)=Ax+b.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LinearOpFunction implements VecFunctionIntf {

  /**
   * single public no-arg constructor.
   */
  public LinearOpFunction() {
  }


  /**
   * evaluate Ax+b.
   * @param x VectorIntf
   * @param p Hashtable must contain the following pairs:
   * <li> <"A", double[][]> the matrix A, optional.
   * <li> <"b", double[]> the vector b, optional.
   * @throws IllegalArgumentException if A=b=null, or if A, b, and x dimensions
   * don't match.
   * @return VectorIntf the vector y=Ax+b
   */
  public VectorIntf eval(VectorIntf x, Hashtable p) throws IllegalArgumentException {
    try {
      double[][] A = (double[][]) p.get("A");
      double[] b = (double[]) p.get("b");
      if (A==null) {
        if (b!=null && x.getNumCoords()==b.length) return new DblArray1Vector(b);
        else throw new IllegalArgumentException("A==b==null OR x and b dimensions don't match");
      }
      final int r = A.length;
      final int c = A[0].length;
      VectorIntf res = new DblArray1Vector(new double[r]);
      for (int i=0; i<r; i++) {
        double s = 0.0;
        for (int j=0; j<c; j++) s += A[i][j]*x.getCoord(j);
        if (b!=null) s += b[i];
        res.setCoord(i,s);
      }
      return res;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("couldn't evaluate Ax+b");
    }
  }


  public double evalCoord(VectorIntf x, Hashtable p, int coord) throws IllegalArgumentException {
    try {
      double[][] A = (double[][]) p.get("A");
      double[] b = (double[]) p.get("b");
      if (A==null) {
        if (b!=null && x.getNumCoords()==b.length) return b[coord];
        else throw new IllegalArgumentException("A==b==null OR x and b dimensions don't match");
      }
      final int c = A[0].length;
      double s = 0.0;
      for (int j=0; j<c; j++) s += A[coord][j]*x.getCoord(j);
      if (b!=null) s += b[coord];
      return s;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("couldn't evaluate Ax+b");
    }
  }
}

