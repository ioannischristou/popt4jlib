package tests;

import popt4jlib.*;
import utils.*;
import popt4jlib.GradientDescent.*;
import java.util.*;

/**
 * A test driver class for testing the AlternatingVariablesDescent class for
 * local unconstrained (or box-constrained) optimization implementing the
 * "trivial" Alternating Variables Descent method (valid for non-smooth
 * functions also).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AVDTest {
  /**
   * public no-arg no-op constructor
   */
  public AVDTest() {
    // no-op
  }


  /**
   * invoke from the command-line as:
   * <CODE>java -cp &lt;classpath&gt; tests.AVDTest &lt;params_file&gt; [random_seed] [maxfuncevals]</CODE>.
   * The params_file must contain lines of the following form:
	 * <ul>
   * <li> avd.numdimensions, $num$ mandatory, number of dimensions
   * of the function to be optimized; the function to be optimized must accept
   * as arguments either <CODE>double[]</CODE> or <CODE>VectorIntf</CODE>
   * objects.
   * <li> avd.numtries, $num$ mandatory, the number of "tries" to
   * attempt from a single initial random point.
   * <li> class,avd.function, &lt;fullclassnameoffunction&gt; mandatory, the
   * classname of the java class defining the function to be optimized.
   * <li> avd.minargval, $val$ mandatory, a double number that is a lower
   * bound for all variables of the optimization process, i.e. all variables
   * must satisfy x_i &ge; val.
   * <li> avd.maxargval, $val$ mandatory, a double number that is an upper
   * bound for all variables of the optimization process, i.e. all variables
   * must satisfy x_i &le; val.
   * <li> avd.tryorder$i$, $n$ optional, an integer value specifying which
   * variable to be optimized in the i-th inner iteration. If no such line
   * exists, and also, the pair ("avd.tryallparallel",true) exists in params,
	 * all variables will be optimized in parallel, and the best one kept as the 
	 * changing value variable.
   * <p>Plus in the file, the parameters for the method
   * <CODE>AlternatingVariablesDescent.minimize(f)</CODE> must be specified:
   * <li> avd.numthreads, $num$ optional, the number of threads to use in
   * the optimization process. Default is 1.
   * <li> avd.ftol, $num$ optional, the minimum abs. value for two function
   * values to be considered different. Default is 1.e-8.
   * <li> avd.stepsize, $num$ optional, the minimum quantum step size for
   * each variable. Special smaller values for specific variables may be set
   * by a pair avd.stepsize$i$, $num$. Default is 1.e-6.
   * <li> avd.nitercnt, $n$ optional, the number of inner-iterations
   * in the OneDStepQuantumOptimizer process before changing the length of the
   * step-size. Default is 5.
   * <li> avd.multfactor, $n$ optional, the multiplication factor when
   * changing the inner step-size length. Default is 2.
	 * <li> class,avd.pdbtexecinitedwrkcmd,&lt;fullclassname&gt;[args] optional, 
	 * if present, the full class name of the initialization command to send to 
	 * the network of workers to run function evaluation tasks, followed by the 
	 * constructor's arguments if any; default is null, indicating no distributed 
	 * computation.
	 * <li> avd.pdbthost, &lt;hostname&gt; optional, the name
	 * of the server to send function evaluation requests, default is localhost.
	 * <li> avd.pdbtport, $num$ optional, the port the above server listens to 
	 * for client requests, default is 7891.
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
      int n = ((Integer) params.get("avd.numdimensions")).intValue();
      double maxargval = ((Double) params.get("avd.maxargval")).doubleValue();
      double minargval = ((Double) params.get("avd.minargval")).doubleValue();
      // add the initial point
      VectorIntf x0 = new DblArray1Vector(new double[n]);
      for (int j=0; j<n; j++) {
        double val = minargval+RndUtil.getInstance().getRandom().nextDouble()*(maxargval-minargval);
        x0.setCoord(j, val);
      }
      // check out any tryorder points
      int[] tryorder = new int[n];
      boolean toexists = false;
      for (int i=0; i<n; i++) {
        Integer toi = (Integer) params.get("avd.tryorder"+i);
        if (toi!=null) {
          toexists = true;
          tryorder[i] = toi.intValue();
        }
        else tryorder[i] = -1;
      }
      if (toexists) params.put("avd.tryorder",tryorder);
			if (!toexists) {  // check if "tryallparallel" keyword is there
				Boolean tapB = (Boolean) params.get("avd.tryallparallel");
				if (tapB==null || tapB.booleanValue()==false) {
					for (int i=0; i<n; i++) {
						tryorder[i]=i;
					}
					params.put("avd.tryorder",tryorder);
				}
			}
      params.put("avd.x0", x0);
      FunctionIntf func = (FunctionIntf) params.get("avd.function");
      FunctionBase wrapper_func = new FunctionBase(func);
      AlternatingVariablesDescent opter = new AlternatingVariablesDescent(params);
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      VectorIntf arg = (VectorIntf) p.getArg();
      System.out.print("best soln found: ");
      System.out.print(arg);
      System.out.println(" VAL="+p.getDouble()+" #function calls="+wrapper_func.getEvalCount());
      long dur = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+dur);
      System.out.println("VVV,"+p.getDouble()+",TTT,"+dur+",NNN,"+wrapper_func.getEvalCount()+",PPP,AVD,FFF,"+args[0]);  // for parser program to extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

