package tests;

import popt4jlib.*;
import popt4jlib.GA.*;
import popt4jlib.GradientDescent.*;
import java.util.*;
import utils.RndUtil;

/**
 * Test-driver class for optimizing the Fletcher-Powell Function (defined and
 * documented in <CODE>tests.FletcherPowellFunction</CODE>) using the DGA process.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DGAFPTest {

  /**
   * public no-arg constructor
   */
  public DGAFPTest() {
  }


  /**
   * invoke as <CODE>java -&lt;classpath&gt; tests.DGAFPTest &lt;params_file&gt;</CODE>
   * where the params_file must have the lines described in the documentation of
   * the class popt4jlib.GA.DGA.
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long start_time = System.currentTimeMillis();
      HashMap params = utils.DataMgr.readPropsFromFile(args[0]);
      // simply add the parameters A, B and alpha
      int n = ((Integer) params.get("dga.chromosomelength")).intValue();
      Vector[] A = new Vector[n];
      Vector[] B = new Vector[n];
      double[] alpha = new double[n];
      for (int i=0; i<n; i++) {
        A[i] = new Vector();
        B[i] = new Vector();
        alpha[i] = Math.PI*Math.sin(Math.PI/(i+1));
        for (int j=0; j<n; j++) {
          A[i].addElement(new Integer(RndUtil.getInstance().getRandom().nextInt(200)-100));
          B[i].addElement(new Integer(RndUtil.getInstance().getRandom().nextInt(200)-100));
        }
      }
      params.put("A",A);
      params.put("B",B);
      params.put("alpha",alpha);
      FunctionIntf func = (FunctionIntf) params.get("dga.function");
      FunctionBase wrapper_func = new FunctionBase(func);
      params.put("dga.function",wrapper_func);
      DGA opter = new DGA(params);
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      double[] arg = (double[]) p.getArg();
      System.out.print("best soln found:[");
      for (int i=0;i<arg.length;i++) System.out.print(arg[i]+" ");
      System.out.println("] VAL="+p.getDouble());
/*
      // finally see how mutation went
      MutationOpIntf mop = (MutationOpIntf) params.get("dga.mutationop");
      if (mop!=null && mop instanceof DblArray1GradientDescentMutationOp) {
        int s = ((DblArray1GradientDescentMutationOp) mop).getSuccessfulMutations();
        int f = ((DblArray1GradientDescentMutationOp) mop).getFailedMutations();
        System.err.println("successful mutations="+s+" failed mutations="+f);
      }
*/
      // final local optimization
      LocalOptimizerIntf lasdst = (LocalOptimizerIntf) params.get("dga.localoptimizer");
      if (lasdst!=null) {
        VectorIntf x0 = new popt4jlib.DblArray1Vector(arg);
        params.put("gradientdescent.x0", x0);
        lasdst.setParams(params);
        utils.PairObjDouble p2 = lasdst.minimize(wrapper_func);
        VectorIntf xf = (VectorIntf) p2.getArg();
        System.out.print(
            "Optimized (via LocalOptimizer) best soln found:[");
        for (int i = 0; i < xf.getNumCoords(); i++) System.out.print(xf.
            getCoord(i) + " ");
        System.out.println("] VAL=" + p2.getDouble());
      }
      System.out.println("total function evaluations="+wrapper_func.getEvalCount());
      long end_time = System.currentTimeMillis();
      System.out.println("total time (msecs): "+(end_time-start_time));
      System.out.print("true optimal solution is: alpha=[ ");
      for (int i=0; i<n; i++) System.out.print(alpha[i]+" ");
      System.out.println("] FP(alpha)=0");
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
