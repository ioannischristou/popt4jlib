package tests;

import java.util.*;
import utils.*;
import analysis.*;
import popt4jlib.*;
import popt4jlib.GradientDescent.*;


/**
 * test-driver for the analysis.GradApproximator class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class GradApproximatorTest {
  /**
   * public no-arg constructor
   */
  public GradApproximatorTest() {
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.GradApproximatorTest &lt;params_file&gt; &lt;x_1&gt; ... &lt;x_n&gt;
   * where the params_file must contain the following:
   * <li> class,test.function, &lt;fullclassname&gt; the name of the function whose
   * gradient is to be approximated (implementing the <CODE>popt4jlib.FunctionIntf</CODE>)
   * interface.
   * <li> class,test.gradient, &lt;fullclassname&gt; the name of the class
   * implementing the <CODE>popt4jlib.VecFunctionIntf</CODE> that computes the
   * actual gradient of the test-function
   * <li> gradapproximator.numthreads, &lt;numthreads&gt; the number of threads
   * to use (optional). Default is 1. If this option is used, causes the
   * <CODE>GradApproximatorMT</CODE> class to be used instead of the single-
   * threaded version implemented in <CODE>GradApproximator</CODE>.
   * The rest of the parameters are the values for each of the variables at the
   * point in $$R^n$$ at which the gradient (and its approximation via the
   * <CODE>analysis.GradApproximator</CODE> class) will be computed.
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      HashMap p = DataMgr.readPropsFromFile(args[0]);
      FunctionIntf f = (FunctionIntf) p.get("test.function");
      VecFunctionIntf grad = (VecFunctionIntf) p.get("test.gradient");
      int n = args.length-1;
      double[] x = new double[n];
      for (int i=0; i<n; i++) x[i] = Double.parseDouble(args[i+1]);
      Integer ntI = (Integer) p.get("gradapproximator.numthreads");
      if (ntI==null) {
        GradApproximator ga = new GradApproximator(f);
        VectorIntf x0 = new DblArray1Vector(x);
        double f0 = f.eval(x0, p);
        System.out.println("f(" + x0 + ")=" + f0);
        VectorIntf g0 = grad.eval(x0, p);
        double normg0 = VecUtil.norm(g0, 2);
        System.out.println("g0(" + x0 + ")=" + g0 + " norm2=" + normg0);
        VectorIntf ga0 = ga.eval(x0, p);
        double normga0 = VecUtil.norm(ga0, 2);
        System.out.println("ga0(" + x0 + ")=" + ga0 + " norm2=" + normga0);
        VectorIntf g2 = ga0.newInstance();  // ga0.newCopy();
        for (int i = 0; i < n; i++) {
          g2.setCoord(i, ga0.getCoord(i) - g0.getCoord(i));
        }
        System.out.println("diff norm = " + VecUtil.norm(g2, 2));
      }
      else {
        GradApproximatorMT ga = new GradApproximatorMT(f);
        VectorIntf x0 = new DblArray1Vector(x);
        double f0 = f.eval(x0, p);
        System.out.println("f(" + x0 + ")=" + f0);
        VectorIntf g0 = grad.eval(x0, p);
        double normg0 = VecUtil.norm(g0, 2);
        System.out.println("g0(" + x0 + ")=" + g0 + " norm2=" + normg0);
        VectorIntf ga0 = ga.eval(x0, p);
        double normga0 = VecUtil.norm(ga0, 2);
        System.out.println("ga0(" + x0 + ")=" + ga0 + " norm2=" + normga0);
        VectorIntf g2 = ga0.newInstance();  // ga0.newCopy();
        for (int i = 0; i < n; i++) {
          g2.setCoord(i, ga0.getCoord(i) - g0.getCoord(i));
        }
        System.out.println("diff norm = " + VecUtil.norm(g2, 2));
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
