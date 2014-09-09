package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the (weighted) ||x||^2 Function in n-dimensions.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Norm2Function implements FunctionIntf {
  /**
   * public no-arg constructor
   */
  public Norm2Function() {
  }


  /**
   * evaluates the (Ax)'x function at x=arg, where A is a diagonal matrix
   * giving the weights for each component.
   * @param arg Object must be a <CODE>double[]</CODE> or a <CODE>VectorIntf</CODE>
   * object.
   * @param p Hashtable if not null, may contain pairs of the form
   * <li> <"a"$i$, Double v> where v is the weight associates with the (i+1)-st
   * variable.
   * @return double
   * @throws IllegalArgumentException if the arguments don't adhere to the above
   * specifications.
   */
  public double eval(Object arg, Hashtable p) throws IllegalArgumentException {
    try {
      if (arg instanceof VectorIntf) {
        VectorIntf x = (VectorIntf) arg;
        int n = x.getNumCoords();
        double res = 0;
        for (int i = 0; i < n; i++) {
          double xi = x.getCoord(i);
          double val = xi * xi;
          if (p!=null) {
            Double aiD = (Double) p.get("a" + i);
            if (aiD != null) val *= aiD.doubleValue();
          }
          res += val;
        }
        return res;
      }
      else {
        double[] x = (double[]) arg;
        int n = x.length;
        double res = 0;
        for (int i = 0; i < n; i++) {
          double val = x[i] * x[i];
          if (p!=null) {
            Double aiD = (Double) p.get("a" + i);
            if (aiD != null) val *= aiD.doubleValue();
          }
          res += val;
        }
        return res;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("function cannot be evaluated with given arguments");
    }
  }


  /**
   * invoke as <CODE>java -cp &ltclasspath&gt tests.Norm2Function &ltx_1&gt ... &ltx_n&gt</CODE>
   * where each argument is the corresponding component of the vector point at
   * which the function ||x||^2 is to be evaluated (A=1 in this case).
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    Hashtable p = new Hashtable();
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    Norm2Function f = new Norm2Function();
    double res=f.eval(x,p);
    System.out.println(res);
  }
}

