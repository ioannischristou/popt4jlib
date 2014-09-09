package tests;

import popt4jlib.*;
import analysis.*;
import java.util.*;

/**
 * test-driver for the numerical integration classes.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class NumericalIntegrationTest {

  /**
   * public constructor
   */
  public NumericalIntegrationTest() {
  }


  /**
   * invoke as <CODE>java -cp &ltclasspath&gt tests.NumericalIntegrationTest &ltb&gt [H]</CODE>.
   * Computes the integral of a test function with respect to the first variable
   * from 0 up to b (default is 1000). The second (optional) variable H represents
   * the width of the consecutive sub-intervals that will make up the entire
   * interval of integration.
   * @param args String[]
   */
  public static void main(String[] args) {
    double b2 = 1000;
    Double H = null;
    if (args.length>0) b2 = Double.parseDouble(args[0]);
    if (args.length>1) H = new Double(Double.parseDouble(args[1]));
    double a = 0;
    double b=1;
    while (b<=b2) {
      int ivi = 0;
      DblArray1Vector x = new DblArray1Vector(new double[2]);
      x.setCoord(ivi, b);
      Hashtable params = new Hashtable();
      params.put("integralapproximator.a", new Double(a));
      params.put("integralapproximator.integrandvarindex", new Integer(ivi));
      if (H!=null) params.put("integralapproximator.H", H);
      Hashtable p = new Hashtable();
      p.put("integralapproximatormt.maxnumthreads", new Integer(10));
      IntegralApproximatorMT integrator = new IntegralApproximatorMT(new MyFunction2(), p);
      double val = integrator.eval(x, params);
      System.out.println("integral from a="+a+"to b="+b+" = " + val);
      System.err.println("total num simpson() calls=" +
                         integrator.getTotalNumCalls());
      b+=1;
    }
  }
}


class MyFunction2 implements FunctionIntf {
  public MyFunction2() {
  }
  public double eval(Object x, Hashtable p) throws IllegalArgumentException {
    double[] t;
    if (x==null) throw new IllegalArgumentException("null arg");
    else if (x instanceof VectorIntf) t = ((VectorIntf) x).getDblArray1();
    else if (x instanceof double[]) t = (double[]) x;
    else throw new IllegalArgumentException("arg cannot be converted to double[]");
    return t[0]*t[0]*Math.exp(-t[0]*t[0]/2)/Math.sqrt(2.0*Math.PI);
  }
}

