package tests;

import popt4jlib.*;
import utils.*;
import popt4jlib.GradientDescent.*;
import java.util.*;

/**
 * A test driver class for testing the AcceleratedGradientDescent class for
 * local unconstrained optimization implementing the
 * Accelerated Gradient Descent method.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AGDTest {
  /**
   * public no-arg no-op constructor
   */
  public AGDTest() {
    // no-op
  }


  /**
   * invoke from the command-line as:
   * <CODE>java -cp &lt;classpath&gt; tests.AGDTest &lt;params_file&gt; 
	 * [random_seed] [maxfuncevals]
	 * </CODE>.
   * The params_file must contain lines of the following form:
	 * <ul>
   * <li> agd.numdimensions, $num$ mandatory, number of dimensions
   * of the function to be optimized; the function to be optimized must accept
   * as arguments either <CODE>double[]</CODE> or <CODE>VectorIntf</CODE>
   * objects.
   * <li> agd.numtries, $num$ mandatory, the number of "tries" to
   * attempt, i.e. the number of initial random points.
   * <li> agd.maxiters, $num$ optional, the maximum number of 
	 * major iterations of the AGD search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
	 * <li> agd.L, $val$ optional, the L constant that is supposed to 
	 * be the L-smoothness factor of the objective function. Default is 1.0.
   * <li> class,agd.function, &lt;fullclassnameoffunction&gt; mandatory, the
   * classname of the java class defining the function to be optimized.
   * <li> agd.numthreads, $num$ optional, the number of threads to use in
   * the optimization process. Default is 1.
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
        RndUtil.getInstance().setSeed(seed);  // update all extra instances too!
      }
      if (args.length>2) {
        long num = Long.parseLong(args[2]);
        params.put("maxfuncevalslimit",new Long(num));
      }
      int n = ((Integer) params.get("agd.numdimensions")).intValue();
			int numtries = 1;
			try {
				numtries = ((Integer) params.get("agd.numtries")).intValue();
			}
			catch (Exception e) {
				System.err.println("agd.numtries not specified, will use 1");
			}
      // add the initial points
			final Random r = RndUtil.getInstance().getRandom();
			for (int i=0; i<numtries; i++) {
				VectorIntf x0 = new DblArray1Vector(new double[n]);
	      for (int j=0; j<n; j++) {
		      double val = r.nextGaussian();
			    x0.setCoord(j, val);
				}
				params.put("agd.x"+i, x0);
			}
      FunctionIntf func = (FunctionIntf) params.get("agd.function");
      FunctionBase wrapper_func = new FunctionBase(func);
      AcceleratedGradientDescent opter = 
				new AcceleratedGradientDescent(params);
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      VectorIntf arg = (VectorIntf) p.getArg();
      System.out.print("best soln found: ");
      System.out.print(arg);
      System.out.println(" VAL="+p.getDouble()+" #function calls="+
				                 wrapper_func.getEvalCount());
      long dur = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+dur);
      System.out.println("VVV,"+p.getDouble()+",TTT,"+dur+",NNN,"+
				                 wrapper_func.getEvalCount()+",PPP,AGD,FFF,"+args[0]);  
      // above is for parser program to extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

