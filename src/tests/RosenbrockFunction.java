package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the Rosenbrock Function in n-dimensions
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RosenbrockFunction implements FunctionIntf {
  /**
   * public no-arg constructor
   */
  public RosenbrockFunction() {
  }


  /**
   * evaluates the Rosenbrock function at x=arg
   * @param arg Object must be a <CODE>double[]</CODE> or <CODE>VectorIntf</CODE>
   * @param p Hashtable unused
   * @return double
   * @throws IllegalArgumentException if arg does not adhere to the specification
   * above.
   */
  public double eval(Object arg, Hashtable p) throws IllegalArgumentException {
    try {
      if (arg instanceof VectorIntf) {
        VectorIntf x = (VectorIntf) arg;
        int n = x.getNumCoords();
        double res = 0;
        for (int i = 0; i < n - 1; i++) {
          double xi = x.getCoord(i);
          double xip1 = x.getCoord(i + 1);
          res += (1 - xi) * (1 - xi) +
              100 * (xip1 - xi * xi) * (xip1 - xi * xi);
        }
        return res;
      }
      else {
        double[] x = (double[]) arg;
        int n = x.length;
        double res = 0;
        for (int i = 0; i < n - 1; i++) {
          res += (1 - x[i]) * (1 - x[i]) +
              100 * (x[i + 1] - x[i] * x[i]) * (x[i + 1] - x[i] * x[i]);
        }
        return res;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("arg is not double[] or VectorIntf");
    }
  }


  /**
   * invoke as <CODE>java -cp &ltclasspath&gt tests.RosenbrockFunction &ltx_1&gt ... &ltx_n&gt
   * where the parameters are the components of the vector point at which the
   * function must be evaluated.
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n];
    for (int i=0; i<n; i++) {
      x[i] = Double.parseDouble(args[i]);
    }
    RosenbrockFunction f = new RosenbrockFunction();
    double res=f.eval(x,null);
    System.out.println(res);
  }
}

