package tests;

import popt4jlib.*;
import popt4jlib.DE.*;
import popt4jlib.GradientDescent.*;
import java.util.*;
import utils.RndUtil;

/**
 * Test-driver class for the (Distributed) Differential Evolution algorithm
 * implemented in class popt4jlib.DE.DDE.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DDETest {
  /**
   * single public no-arg constructor
   */
  public DDETest() {
  }


  /**
   * run as <CODE> java -cp &lt;classpath&gt; tests.DDETest &lt;params_file&gt; [random_seed] [maxfuncevals]</CODE>
   * where the params_file must contain lines of the following form:
	 * <ul>
   * <li> class,dde.function, &lt;fullclasspathname&gt;  mandatory, the java class
   * name defining the function to be optimized, which must accept
   * as arguments either <CODE>double[]</CODE> or <CODE>VectorIntf</CODE>
   * objects.
   * <li> class,dde.localoptimizer, &lt;fullclasspathname&gt; optional the java
   * class name of an object implementing the LocalOptimizerIntf defined in
   * the popt4jlib.GradientDescent package, to be used as further optimizer of
   * the best solution found by the DE process.
   * <li> dde.numdimensions, $num$ mandatory, the dimension of the domain of
   * the function to be minimized.
   * <li> dde.numtries, $num$ optional, the total number of "tries", default
   * is 100.
   * <li> dde.numthreads, $num$ optional, the number of threads to use,
   * default is 1.
   * <li> rndgen,$num$,$num2$ mandatory, specifies the starting random
   * seed to use for each of the $num2$ threads to use (the value num2 must
   * equal the number given in the line for dde.numthreads).
   * <li> dde.popsize, $num$ optional, the total population size in each
   * iteration, default is 10.
   * <li> dde.w, $num$ optional, the "weight" of the DE process, a double
   * number in [0,2], default is 1.0
   * <li> dde.px, $num$ optional, the "crossover rate" of the DE process, a
   * double number in [0,1], default is 0.9
   * <li> dde.minargval, $num$ optional, a double number that is a lower
   * bound for all variables of the optimization process, i.e. all variables
   * must satisfy x_i &gte; $num$, default is -infinity
   * <li> dde.maxargval, $num$ optional, a double number that is an upper
   * bound for all variables of the optimization process, i.e. all variables
   * must satisfy x_i &lte; $num$, default is +infinity
   * <li> dde.minargval$i$, $num$ optional, a double number that is a lower
   * bound for the i-th variable of the optimization process, i.e. variable
   * must satisfy x_i >= $num$, default is -infinity
   * <li> dde.maxargval$i$, $num$ optional, a double number that is an upper
   * bound for the i-th variable of the optimization process, i.e. variable
   * must satisfy x_i &lte; $num$, default is +infinity
	 * <li> dde.de/best/1/binstrategy, $val$ optional, a boolean value
	 * that if present and true, indicates that the DE/best/1/bin strategy should
	 * be used in evolving the population instead of the DE/rand/1/bin strategy,
	 * default is false
	 * <li> dde.nondeterminismok, $val$ optional, a boolean value 
	 * indicating whether the method should return always the same value given 
	 * the same parameters and same random seed(s). The method can be made to run
	 * much faster in a multi-core setting if this flag is set to true (at the 
	 * expense of deterministic results) getting the CPU utilization to reach 
	 * almost 100% as opposed to around 60% otherwise, default is false
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
      Hashtable params = utils.DataMgr.readPropsFromFile(args[0]);
      if (args.length>1) {
        long seed = Long.parseLong(args[1]);
        RndUtil.getInstance().setSeed(seed);  // updates all extra instances too!
      }
      if (args.length>2) {
        long num = Long.parseLong(args[2]);
        params.put("maxfuncevalslimit",new Long(num));
      }
      FunctionIntf func = (FunctionIntf) params.get("dde.function");
      FunctionBase wrapper_func = new FunctionBase(func);
      params.put("dde.function",wrapper_func);
      DDE opter = new DDE(params);
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      VectorIntf arg = (VectorIntf) p.getArg();
      System.out.print("best soln found:[");
      for (int i=0;i<arg.getNumCoords();i++) System.out.print(arg.getCoord(i)+" ");
      System.out.println("] VAL="+p.getDouble());
      // final local optimization
			utils.PairObjDouble p2 = null;
      LocalOptimizerIntf lasdst = (LocalOptimizerIntf) params.get("dde.localoptimizer");
      if (lasdst!=null) {
        VectorIntf x0 = arg.newInstance();  // arg.newCopy();
        params.put("gradientdescent.x0", x0);
        lasdst.setParams(params);
        p2 = lasdst.minimize(wrapper_func);
        VectorIntf xf = (VectorIntf) p2.getArg();
        System.out.print(
            "Optimized (via a GradientDescent local method) best soln found:[");
        for (int i = 0; i < xf.getNumCoords(); i++) System.out.print(xf.
            getCoord(i) + " ");
        System.out.println("] VAL=" + p2.getDouble());
      }
      System.err.println("total function evaluations="+wrapper_func.getEvalCount());
      long dur = System.currentTimeMillis()-start_time;
      double val = (p2==null || p2.getDouble()>=Double.MAX_VALUE) ? p.getDouble() : p2.getDouble();
			System.out.println("total time (msecs): "+dur);
      System.out.println("VVV,"+val+",TTT,"+dur+",NNN,"+wrapper_func.getEvalCount()+",PPP,DDE,FFF,"+args[0]);  // for parser program to extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
