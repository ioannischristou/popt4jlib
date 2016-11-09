package tests;

import popt4jlib.*;
import utils.*;
import popt4jlib.GradientDescent.*;
import analysis.*;
import java.util.*;

/**
 * Test driver class for the Conjugate-Gradient algorithm using the
 * Polak-Ribiere formula for updating the b parameter, and the Armijo rule
 * method for step-size determination.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PRCGTest {
  /**
   * public no-arg constructor
   */
  public PRCGTest() {
  }


  /**
   * run as <CODE> java -cp &lt;classpath&gt; tests.PRCGTest &lt;params_file&gt; </CODE>
   * where the params_file must contain lines of the following form:
	 * <ul>
   * <li> class,prcg.function, &lt;fullclassname&gt; mandatory, the full class name
   * of the function to be minimized. The function must accept <CODE>double[]</CODE>
   * or <CODE>popt4jlib.VectorIntf</CODE> objects as arguments.
   * <li> prcg.numdimensions, $num$ mandatory, the number of dimensions of the
   * function being minimized.
   * <li> prcg.functionargmaxval, $num$ mandatory, an upper bound on the value of
   * any variable.
   * <li> prcg.functionargmaxval$j$, $num$ optional, if it exists, it indicates
   * a more strict upper bound on the ($j$+1)-st variable (j ranges in
   * [0, prcg.numdimensions-1])
   * <li> prcg.functionargminval, $num$ mandatory, a lower bound on the value of
   * any variable.
   * <li> prcg.functionargminval$j$, $num$ optional, if it exists, it indicates
   * a more strict lower bound on the ($j$+1)-st variable (j ranges in
   * [0, prcg.numdimensions-1])
   * <li> prcg.numtries", $num$ optional, the number of initial starting points
   * to use (must either exist then ntries &lt;"x$i$",VectorIntf v&gt; pairs in the
   * parameters or a pair &lt;"gradientdescent.x0",VectorIntf v&gt; pair in params).
   * Default is 1.
   * <li> prcg.numthreads, $num$ optional, the number of threads to use.
   * Default is 1.
   * <li> class,prcg.gradient, &lt;fullclasspathname&gt; optional, the class name
   * of the <CODE>popt4jlib.VecFunctionIntf</CODE> the implements the gradient
   * of f, the function to be minimized. If this param-value pair does not exist,
   * the gradient will be computed using Richardson finite differences extrapolation
   * <li> prcg.gtol, $num$ optional, the minimum abs. value for each of the
   * gradient's coordinates, below which if all coordinates of the gradient
   * happen to be, the search stops assuming it has reached a stationary point.
   * Default is 1.e-8.
   * <li> prcg.maxiters, $num$ optional, the maximum number of major
   * iterations of the CG search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
   * <li> prcg.rho, $num$ optional, the value of the parameter ñ in approximate
   * line search step-size determination obeying the two Wolfe-Powell conditions
   * Default is 0.1.
   * <li> prcg.sigma, $num$ optional, the value of the parameter ó in the
   * approximate line search step-size determination obeying the Wolfe-Powell
   * conditions. Default is 0.9
   * <li> prcg.beta, $num$ optional, the value of the parameter â in the
   * approximate line search step-size determination obeying the Armijo rule
   * conditions. Default is 0.9.
   * <li> prcg.gamma, $num$ optional, the value of the parameter ã in the
   * approximate line search step-size determination obeying the Armijo rule
   * conditions. Default is 1.0.
   * <li> prcg.looptol, $num$ optional, the minimum step-size allowed. Default
   * is 1.e-21.
   * <li> Also, any other parameters e.g. needed by the function to be evaluated
   * must be provided as separate lines in the params_file
   * </ul>
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long start_time = System.currentTimeMillis();
      HashMap params = utils.DataMgr.readPropsFromFile(args[0]);
      final int n = ((Integer) params.get("prcg.numdimensions")).intValue();
      final double maxargval = ((Double) params.get("prcg.functionargmaxval")).doubleValue();
      final double minargval = ((Double) params.get("prcg.functionargminval")).doubleValue();
      // add the initial points
      final int numtries = ((Integer) params.get("prcg.numtries")).intValue();
      for (int i=0; i<numtries; i++) {
        VectorIntf x0 = new DblArray1Vector(new double[n]);
        for (int j=0; j<n; j++) {
          double maxargvalj = maxargval;
          Double maxargvaljD = (Double) params.get("prcg.functionargmaxval"+j);
          if (maxargvaljD!=null && maxargvaljD.doubleValue()<maxargval)
            maxargvalj = maxargvaljD.doubleValue();
          double minargvalj = minargval;
          Double minargvaljD = (Double) params.get("prcg.functionargminval"+j);
          if (minargvaljD!=null && minargvaljD.doubleValue()>minargval)
            minargvalj = minargvaljD.doubleValue();
          double val = minargvalj+RndUtil.getInstance().getRandom().nextDouble()*(maxargvalj-minargvalj);
          x0.setCoord(j, val);
        }
        params.put("prcg.x"+i, x0);
      }
      FunctionIntf func = (FunctionIntf) params.get("prcg.function");
      PolakRibiereConjugateGradient opter = new PolakRibiereConjugateGradient(params);
      utils.PairObjDouble p = opter.minimize(func);
      VectorIntf arg = (VectorIntf) p.getArg();
      System.out.print("best soln found:[");
      for (int i=0;i<arg.getNumCoords();i++) System.out.print(arg.getCoord(i)+" ");
      System.out.println("] VAL="+p.getDouble());
      System.err.println("numSolutionsFound="+opter.getNumOK()+" numFailed="+opter.getNumFailed());
      VecFunctionIntf grad = (VecFunctionIntf) params.get("prcg.gradient");
      if (grad==null) grad = new GradApproximator(func);
      VectorIntf g = grad.eval(arg,params);
      double norm = VecUtil.norm(g,2);
      System.err.println("checking: ||g(best)||="+norm);
      long dur = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+dur);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

