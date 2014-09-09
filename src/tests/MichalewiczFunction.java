package tests;

import popt4jlib.*;
import java.util.*;

/**
 * This class implements the Michalewicz Function in n-dimensions. The function
 * is defined as follows:
 * $$f(x) = - \sum_{i=1}^n [sin(x_i) [sin(ix_i^2 / ð)]^{2m}] $$
 * Usually, m=10., and the variables bounds is the hyper-cube [0,ð]^n.
 * For n=5, the global minimum is ~-4.687, for n=10 it is near -9.66, and as
 * n gets larger, its percentage gap from -n should approach zero.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MichalewiczFunction implements FunctionIntf {
  /**
   * public no-arg constructor
   */
  public MichalewiczFunction() {
  }


  /**
   * evaluates the Michalewicz function at x=arg
   * @param arg Object must be <CODE>double[]</CODE> or
   * <CODE>popt4jlib.VectorIntf</CODE>
   * @param p Hashtable must contain the pair:
   * <li> <"m", Double v> indicating the value of the parameter m.
   * @return double
   * @throws IllegalArgumentException if any of the two arguments does not adhere
   * to the above specification
   */
  public double eval(Object arg, Hashtable p) throws IllegalArgumentException {
    try {
      double m = ( (Double) p.get("m")).doubleValue();
      if (arg instanceof VectorIntf) {
        VectorIntf x = (VectorIntf) arg;
        int n = x.getNumCoords();
        double res = 0;
        for (int i = 0; i < n; i++) {
          double xi = x.getCoord(i);
          res += Math.sin(xi) *
              Math.pow(Math.sin( (i + 1) * xi * xi / Math.PI), 2.0 * m);
        }
        return -res;
      }
      else {
        double[] x = (double[]) arg;
        int n = x.length;
        double res = 0;
        for (int i = 0; i < n; i++) {
          double xi = x[i];
          res += Math.sin(xi) *
              Math.pow(Math.sin( (i + 1) * xi * xi / Math.PI), 2.0 * m);
        }
        return -res;
      }
    }
    catch (Exception e) {
      throw new IllegalArgumentException("cannot evaluate the Michalewicz function for the given arguments");
    }
  }


  /**
   * invoke as <CODE>java -cp &ltclasspath&gt tests.MichalewiczFunction &ltm&gt &ltx_1&gt ... &ltx_n&gt</CODE>
   * where the first argument is the parameter m and the remaining form the
   * components of the vector point at which the function will be evaluated.
   * @param args String[]
   */
  public static void main(String[] args) {
    int n = args.length;
    double[] x = new double[n-1];
    double m = Double.parseDouble(args[0]);
    Hashtable p = new Hashtable();
    p.put("m",new Double(m));
    for (int i=1; i<n; i++) {
      x[i-1] = Double.parseDouble(args[i]);
    }
    MichalewiczFunction f = new MichalewiczFunction();
    double res=f.eval(x,p);
    System.out.println(res);
  }
}

