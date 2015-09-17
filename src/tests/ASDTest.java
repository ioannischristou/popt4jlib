package tests;

import popt4jlib.*;
import utils.*;
import popt4jlib.GradientDescent.*;
import java.util.*;

/**
 * A test driver class for testing the ArmijoSteepestDescent class for local
 * unconstrained optimization implementing the classical Steepest Descent with
 * Armijo rule for step-size determination algorithm.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ASDTest {
  /**
   * public no-arg constructor
   */
  public ASDTest() {
  }


  /**
   * invoke from the command-line as:
   * <CODE> java -cp &lt;classpath&gt; tests.ASDTest &lt;params_file&gt; [random_seed] [maxfuncevals]</CODE>.
   * The params_file must contain lines of the following form:
	 * <ul>
   * <li> asd.numdimensions, $num$ mandatory, number of dimensions
   * of the function to be optimized; the function to be optimized must accept
   * as arguments either <CODE>double[]</CODE> or <CODE>VectorIntf</CODE>
   * objects.
   * <li> asd.numtries, $num$ mandatory, the number of "tries" to
   * attempt. Different random points will be used as initial points for each
   * "try".
   * <li> class,asd.function, &lt;fullclassnameoffunction&gt; mandatory, the
   * classname of the java class defining the function to be optimized.
   * <li> asd.functionargmaxval, $num$ mandatory, the max. argument value for
   * any component of the initial vectors x0 to compute.
   * <li> asd.functionargminval, $num$ mandatory, the min. arg. value for any
   * component of the initial vectors x0 to compute.
   *
   * <p>Plus in the file, the parameters for the method
   * <CODE>ArmijoSteepestDescent.minimize(f)</CODE> must be specified:
   *
   * <li> asd.numthreads, $num$ optional, the number of threads to use in
   * the optimization process. Default is 1.
   * <li> asd.numtries, $num$ optional, the number of tries (starting
   * from different initial points). Default is 1.
   * <li> class,asd.gradient, &lt;fullclassnameofgradient&gt; optional, grad. of
   * f, the function to be minimized. If this param-value pair doesn't exist the
   * gradient will be computed using Richardson finite differences extrapolation
   * &lt;asd.gtol, $num$&gt; optional, the minimum absolute value for each of the
   * gradient's coordinates, below which if all coordinates of the gradient
   * happen to be, the search stops assuming it has reached a stationary point.
   * Default is 1.e-6.
   * <li> asd.maxiters, $num$ optional, the maximum number of major
   * iterations of the SD search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
   * <li> asd.rho, $num$ optional, the value for the parameter &rho; in the
   * Armijo rule implementation. Default is 0.1.
   * <li> asd.beta, $num$ optional, the value for the parameter &beta; in the
   * Armijo rule implementation. Default is 0.8.
   * <li> asd.gamma, $num$ optional, the value for the parameter &gamma; in the
   * Armijo rule implementation. Default is 1.
   * <li> asd.looptol, $num$ optional, the minimum step-size allowed. Default
   * is 1.e-21.
   * </ul>
   * <p> if the second optional argument is passed in, it overrides the random
   * seed specified in the params_file.</p>
   * <p> The optional third argument, if present, overrides any max. limit set
   * on the number of function evaluations allowed. After this limit, the
   * function will always return Double.MAX_VALUE instead, and won't increase
   * the evaluation count.</p>
   *
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long start_time = System.currentTimeMillis();
      HashMap params = utils.DataMgr.readPropsFromFile(args[0]);
      if (args.length>1) {
        long seed = Long.parseLong(args[1]);
        RndUtil.getInstance().setSeed(seed);  // updates all extra instances too!
      }
      if (args.length>2) {
        long num = Long.parseLong(args[2]);
        params.put("maxfuncevalslimit",new Long(num));
      }
      FunctionIntf func = (FunctionIntf) params.get("asd.function");
      FunctionBase wrapper_func = new FunctionBase(func);
      params.put("asd.function",wrapper_func);
      int n = ((Integer) params.get("asd.numdimensions")).intValue();
      double maxargval = ((Double) params.get("asd.functionargmaxval")).doubleValue();
      double minargval = ((Double) params.get("asd.functionargminval")).doubleValue();
      // add the initial points
      int numtries = ((Integer) params.get("asd.numtries")).intValue();
      for (int i=0; i<numtries; i++) {
        VectorIntf x0 = new DblArray1Vector(new double[n]);
        for (int j=0; j<n; j++) {
          double val = minargval+RndUtil.getInstance().getRandom().nextDouble()*(maxargval-minargval);
          x0.setCoord(j, val);
        }
        params.put("asd.x"+i, x0);
      }
      //FunctionIntf func = (FunctionIntf) params.get("asd.function");
      ArmijoSteepestDescent opter = new ArmijoSteepestDescent(params);
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      VectorIntf arg = (VectorIntf) p.getArg();
      System.out.print("best soln found:[");
      for (int i=0;i<arg.getNumCoords();i++) System.out.print(arg.getCoord(i)+" ");
      System.out.println("] VAL="+p.getDouble());
      System.err.println("numSolutionsFound="+opter.getNumOK()+" numFailed="+opter.getNumFailed());
      System.err.println("Total Num Function Evaluations="+wrapper_func.getEvalCount());
      long dur = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+dur);
      System.out.println("VVV,"+p.getDouble()+",TTT,"+dur+",NNN,"+wrapper_func.getEvalCount()+",PPP,ASD,FFF,"+args[0]);  // for parser program to extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
