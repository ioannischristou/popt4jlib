package tests;

import popt4jlib.*;
import utils.*;
import popt4jlib.GradientDescent.*;
import java.util.*;

/**
 * test-driver class for the RandomDirections optimization algorithm.
 * The algorithm is implemented in the
 * <CODE>popt4jlib.GradientDescent.RandomDescents</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RDTest {
  /**
   * public no-arg constructor
   */
  public RDTest() {
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt;  tests.RDTest &lt;params_file&gt;</CODE>
   * where the params_file is a text file. File must contain the following lines:
	 * <ul>
   * <li> class,rd.function, &lt;fullclassname&gt; mandatory, the full class name
   * of the function to be minimized. The function must accept <CODE>double[]</CODE>
   * or <CODE>popt4jlib.VectorIntf</CODE> objects as arguments.
   * <li> rd.numdimensions, $num$ mandatory, the number of dimensions of the
   * function being minimized.
   * <li> rd.functionargmaxval, $num$ mandatory, an upper bound on the value of
   * any variable.
   * <li> rd.functionargmaxval$j$, $num$ optional, if it exists, it indicates
   * a more strict upper bound on the ($j$+1)-st variable (j ranges in
   * [0, rd.numdimensions-1])
   * <li> rd.functionargminval, $num$ mandatory, a lower bound on the value of
   * any variable.
   * <li> rd.functionargminval$j$, $num$ optional, if it exists, it indicates
   * a more strict lower bound on the ($j$+1)-st variable (j ranges in
   * [0, rd.numdimensions-1])
   * <li> rd.numtries", $num$ optional, the number of initial starting points
   * to use (must either exist then ntries &lt;"x$i$",VectorIntf v&gt; pairs in the
   * parameters or a pair &lt;"gradientdescent.x0",VectorIntf v&gt; pair in params).
   * Default is 1.
   * <li> rd.numthreads, $num$ optional, the number of threads to use.
   * Default is 1.
   * <li> rndgen,$num$,$num2$ mandatory, specifies the starting random
   * seed to use for each of the $num2$ threads to use (the value num2 must
   * equal the number given for the rd.numthreads, or 1 if no such line is
   * present). The value of num should be a positive integer.
   * <li> class,rd.gradient, &lt;fullclasspathname&gt; optional, the class name
   * of the <CODE>popt4jlib.VecFunctionIntf</CODE> the implements the gradient
   * of f, the function to be minimized. If this param-value pair does not exist,
   * the gradient will be computed using Richardson finite differences extrapolation
   * <li> rd.gtol, $num$ optional, the minimum abs. value for each of the
   * gradient's coordinates, below which if all coordinates of the gradient
   * happen to be, the search stops assuming it has reached a stationary point.
   * Default is 1.e-8.
   * <li> rd.maxiters, $num$ optional, the maximum number of major
   * iterations of the RD search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
   * <li> rd.rho, $num$ optional, the value of the parameter � in approximate
   * line search step-size determination obeying the two Wolfe-Powell conditions
   * Default is 0.1.
   * <li> rd.beta, $num$ optional, the value of the parameter � in the
   * approximate line search step-size determination obeying the Armijo rule
   * conditions. Default is 0.9.
   * <li> rd.gamma, $num$ optional, the value of the parameter � in the
   * approximate line search step-size determination obeying the Armijo rule
   * conditions. Default is 1.0.
   * <li> rd.looptol, $num$ optional, the minimum step-size allowed. Default
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
      int n = ((Integer) params.get("rd.numdimensions")).intValue();
      double maxargval = ((Double) params.get("rd.functionargmaxval")).doubleValue();
      double minargval = ((Double) params.get("rd.functionargminval")).doubleValue();
      // add the initial point
      VectorIntf x0 = new DblArray1Vector(new double[n]);
      for (int j=0; j<n; j++) {
        double val = minargval+RndUtil.getInstance().getRandom().nextDouble()*(maxargval-minargval);
        x0.setCoord(j, val);
      }
      params.put("rd.x0", x0);
      FunctionIntf func = (FunctionIntf) params.get("rd.function");
      RandomDescents opter = new RandomDescents(params);
      utils.PairObjDouble p = opter.minimize(func);
      VectorIntf arg = (VectorIntf) p.getArg();
      System.out.print("best soln found:[");
      for (int i=0;i<arg.getNumCoords();i++) System.out.print(arg.getCoord(i)+" ");
      System.out.println("] VAL="+p.getDouble());
      long dur = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+dur);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
