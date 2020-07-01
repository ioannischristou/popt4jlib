package tests;

import popt4jlib.*;
import utils.*;
import analysis.*;
import popt4jlib.GradientDescent.*;
import java.util.*;

/**
 * Test driver class for the Conjugate-Gradient algorithm using the
 * Fletcher-Reeves formula for updating the b parameter, and the Al-Baali -
 * Fletcher bracketing &amp; sectioning method for step-size determination. The
 * above are described in:
 * Fletcher R.(1987) Practical Methods of Optimization 2nd ed. Wiley, Chichester
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FCGTest {

  /**
   * public no-arg constructor
   */
  public FCGTest() {
  }


  /**
   * run as 
	 * <CODE> java -cp &lt;classpath&gt; tests.FCGTest &lt;params_file&gt; 
	 * [random_seed] [maxfuncevalslimit]
	 * </CODE>
   * where the params_file must contain lines of the following form:
	 * <ul>
   * <li> class,fcg.function, &lt;fullclassname&gt; mandatory, full class name
   * of the function to be minimized. The function must accept 
	 * <CODE>double[]</CODE> or <CODE>popt4jlib.VectorIntf</CODE> objects as 
	 * arguments.
   * <li> fcg.numdimensions, $num$ mandatory, the number of dimensions of the
   * function being minimized.
   * <li> fcg.functionargmaxval, $num$ mandatory, an upper bound on the value of
   * any variable.
   * <li> fcg.functionargmaxval$j$, $num$ optional, if it exists, it indicates
   * a more strict upper bound on the ($j$+1)-st variable (j ranges in
   * [0, fcg.numdimensions-1])
   * <li> fcg.functionargminval, $num$ mandatory, a lower bound on the value of
   * any variable.
   * <li> fcg.functionargminval$j$, $num$ optional, if it exists, it indicates
   * a more strict lower bound on the ($j$+1)-st variable (j ranges in
   * [0, fcg.numdimensions-1])
   * <li> fcg.numtries", $num$ optional, the number of initial starting points
   * to use (must either exist then ntries &lt;"x$i$",VectorIntf v&gt; pairs in
   * the parameters or a pair &lt;"gradientdescent.x0",VectorIntf v&gt; pair in 
	 * params). Default is 1.
   * <li> fcg.numthreads, $num$ optional, the number of threads to use.
   * Default is 1.
   * <li> class,fcg.gradient, &lt;fullclasspathname&gt; optional, the class name
   * of the <CODE>popt4jlib.VecFunctionIntf</CODE> the implements the gradient
   * of f, the function to be minimized. If this param-value pair does not exist
   * the gradient will be computed using Richardson finite differences 
	 * extrapolation.
   * <li> fcg.gtol, $num$ optional, the minimum abs. value for each of the
   * gradient's coordinates, below which if all coordinates of the gradient
   * happen to be, the search stops assuming it has reached a stationary point.
   * Default is 1.e-8.
   * <li> fcg.maxiters, $num$ optional, the maximum number of major
   * iterations of the CG search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
   * <li> fcg.rho, $num$ optional, the value of the parameter ñ in approximate
   * line search step-size determination obeying the two Wolfe-Powell conditions
   * Default is 0.1.
   * <li> fcg.sigma, $num$ optional, the value of the parameter ó in the
   * approximate line search step-size determination obeying the Wolfe-Powell
   * conditions. Default is 0.9.
   * <li> fcg.t1, $num$ optional, the value of the parameter t_1 in the
   * Al-Baali - Fletcher bracketing-sectioning algorithm for step-size
   * determination. Default is 9.0.
   * <li> fcg.t2, $num$ optional, the value of the parameter t_2 in the
   * Al-Baali - Fletcher algorithm. Default is 0.1.
   * <li> fcg.t3, $num$ optional, the value of the parameter t_3 in the
   * Al-Baali - Fletcher algorithm. Default is 0.5.
   * <li> fcg.redrate, $num$ optional, a user acceptable reduction rate on the
   * function f for stopping the Al-Baali - Fletcher algorithm in the bracketing
   * phase. Default is 2.0.
   * <li> fcg.fbar, $num$ optional, a user-specified acceptable function value
   * to stop the Al-Baali - Fletcher algorithm in the bracketing phase. Default
   * is null (with the effect of utilizing the "fcg.redrate" value for stopping
   * criterion of the bracketing phase).
   * </ul>
   * <p> if the second optional argument is passed in, it overrides the random
   * seed specified in the params_file.
   * <p> The optional third argument, if present, overrides any max. limit set
   * on the number of function evaluations allowed. After this limit, the
   * function will always return Double.MAX_VALUE instead, and won't increase
   * the evaluation count.
   *
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long start_time = System.currentTimeMillis();
      HashMap params = utils.DataMgr.readPropsFromFile(args[0]);
      if (args.length>1) {
        long seed = Long.parseLong(args[1]);
        RndUtil.getInstance().setSeed(seed);  // updates all extra instances!
      }
      if (args.length>2) {
        long num = Long.parseLong(args[2]);
        params.put("maxfuncevalslimit",new Long(num));
      }
      FunctionIntf func = (FunctionIntf) params.get("fcg.function");
      FunctionBase wrapper_func = new FunctionBase(func);
      params.put("fcg.function",wrapper_func);
      final int n = ((Integer) params.get("fcg.numdimensions")).intValue();
      final double maxargval = 
				((Double) params.get("fcg.functionargmaxval")).doubleValue();
      final double minargval = 
				((Double) params.get("fcg.functionargminval")).doubleValue();
      // add the initial points
      final int numtries = ((Integer) params.get("fcg.numtries")).intValue();
      Random r = RndUtil.getInstance().getRandom();
			for (int i=0; i<numtries; i++) {
        VectorIntf x0 = new DblArray1Vector(new double[n]);
        for (int j=0; j<n; j++) {
          double maxargvalj = maxargval;
          Double maxargvaljD = (Double) params.get("fcg.functionargmaxval"+j);
          if (maxargvaljD!=null && maxargvaljD.doubleValue()<maxargval)
            maxargvalj = maxargvaljD.doubleValue();
          double minargvalj = minargval;
          Double minargvaljD = (Double) params.get("fcg.functionargminval"+j);
          if (minargvaljD!=null && minargvaljD.doubleValue()>minargval)
            minargvalj = minargvaljD.doubleValue();
          double val = minargvalj + r.nextDouble()*(maxargvalj-minargvalj);
          x0.setCoord(j, val);
        }
        params.put("fcg.x"+i, x0);
      }
      FletcherConjugateGradient opter = new FletcherConjugateGradient(params);
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      VectorIntf arg = (VectorIntf) p.getArg();
      System.out.print("best soln found:[");
      for (int i=0;i<arg.getNumCoords();i++) 
				System.out.print(arg.getCoord(i)+" ");
      System.out.println("] VAL="+p.getDouble());
      System.err.println("numSolutionsFound="+opter.getNumOK()+
				                 " numFailed="+opter.getNumFailed());
      VecFunctionIntf grad = (VecFunctionIntf) params.get("fcg.gradient");
      if (grad==null) grad = new GradApproximator(func);
      VectorIntf g = grad.eval(arg,params);
      double norm = VecUtil.norm(g,2);
      double norminf = VecUtil.normInfinity(g);
      System.err.println("checking: ||g(best)||="+norm+
				                 " ||g(best)||_inf="+norminf);
      System.err.println("total function evaluations="+
				                 wrapper_func.getEvalCount());
      long dur = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+dur);
      System.out.println("VVV,"+p.getDouble()+",TTT,"+dur+
				                 ",NNN,"+wrapper_func.getEvalCount()+
				                 ",PPP,FCG,FFF,"+args[0]);  // for parser program to 
			                                              // extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

