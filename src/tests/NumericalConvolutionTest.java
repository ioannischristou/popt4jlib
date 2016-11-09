package tests;

import popt4jlib.*;
import analysis.*;
import java.util.*;
import java.io.*;


/**
 * test-driver for the numerical integration classes.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class NumericalConvolutionTest {

  /**
   * public constructor
   */
  public NumericalConvolutionTest() {
  }


  /**
   * invoke as: <CODE>java -cp &lt;classpath&gt; tests.NumericalConvolutionTest
   * &lt;lowerlimit&gt; &lt;upperlimit&gt; &lt; a &gt; &lt; b &gt; &lt; step &gt; &lt; jplotfilename &gt;
   * </CODE>.
   * Computes the convolution of the two functions x^2 (x&le;0) and normpdf
   * with respect to the first variable from a to b and creates a jplot file to
   * visualize the convolution function.
   * @param args String[]
   */
  public static void main(String[] args) {
    double ll = Double.parseDouble(args[0]);
    double ul = Double.parseDouble(args[1]);
    double a = Double.parseDouble(args[2]);
    double b = Double.parseDouble(args[3]);
    double step = Double.parseDouble(args[4]);
    String jplotfile = args[5];
    double[] x = new double[1];
    HashMap params = new HashMap();
    params.put("convolutionapproximator.integrationlowerlimit", new Double(ll));
    params.put("convolutionapproximator.integrationupperlimit", new Double(ul));
    params.put("convolutionapproximator.varindex", new Integer(0));
    params.put("integralapproximator.integrandvarindex", new Integer(0));
    ConvolutionApproximator conv = new ConvolutionApproximator(new MyFunction(), new NormpdfFunction(), params);
    Vector xs = new Vector();
    Vector ys = new Vector();
    double x0 = a;
    double ymin = Double.MAX_VALUE;
    double ymax = Double.NEGATIVE_INFINITY;
    while (x0<=b) {
      x[0] = x0;
      try {
        double y = conv.eval(x, params);
        System.err.println("x="+x0+" conv(f,g)(x)="+y);  // itc: HERE rm asap
        if (y < ymin) ymin = y;
        if (y > ymax) ymax = y;
        xs.addElement(new Double(x0));
        ys.addElement(new Double(y));
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      x0+=step;
    }
    // now create the jplotfile
    try {
      PrintWriter pw = new PrintWriter(new FileOutputStream(jplotfile));
      pw.println("double double");
      pw.println("invisible "+ a +" "+ ymin); pw.println("invisible "+ b +" "+ ymax);
      Double xold = (Double) xs.elementAt(0);
      Double yold = (Double) ys.elementAt(0);
      for (int i=1; i<xs.size(); i++) {
        Double xi = (Double) xs.elementAt(i);
        Double yi = (Double) ys.elementAt(i);
        pw.println("line "+xold.doubleValue()+" "+yold.doubleValue()+" "+
                   xi.doubleValue()+" "+yi.doubleValue());
        xold = xi;
        yold = yi;
      }
      pw.println("go");
      pw.flush();
      pw.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    System.err.println("Total simpson() calls="+conv.getIntegratorTotalCalls());
  }
}


class MyFunction implements FunctionIntf {
  public MyFunction() {
  }
  public double eval(Object x, HashMap p) throws IllegalArgumentException {
    double[] t;
    if (x==null) throw new IllegalArgumentException("null arg");
    else if (x instanceof VectorIntf) t = ((VectorIntf) x).getDblArray1();
    else if (x instanceof double[]) t = (double[]) x;
    else throw new IllegalArgumentException("arg cannot be converted to double[]");
    //return Math.exp(-Math.abs(t[0]));
    if (t[0]<=0) return t[0]*t[0]; else return 0;
    //return Math.sin(t[0]);
    //if (t[0]>=-1 && t[0]<=0) return 1.0+t[0];
    //else if (t[0]>0 && t[0]<=1) return 1.0-t[0];
    //else return 0.0;
  }
}

