package tests;

import popt4jlib.*;
import popt4jlib.MonteCarlo.*;
import java.util.*;
import utils.RndUtil;

/**
 * test-driver class for the Monte-Carlo Search method (random sampling of the
 * search space).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MonteCarloTest {
  /**
   * public no-arg constructor
   */
  public MonteCarloTest() {
  }


  /**
   * invoke as <CODE>java -cp &lt;classpath&gt; tests.MonteCarloTest &lt;params_file&gt; [random_seed] [maxfuncevals]</CODE>
   * where params_file must contain the following lines:
   * <li> class,mcs.function, &lt;fullclassname&gt; mandatory, the full class name
   * of the function to be minimized.
   * <li> mcs.numtries, $num$ mandatory, the number of random attempts
   * to perform in total (these attempts will be distributed among the number
   * of threads that will be created.)
   * <li> class,mcs.randomargmaker, &lt;fullclassname&gt; mandatory, the full class
   * name of an object that implements the <CODE>popt4jlib.RandomArgMakerIntf</CODE>
   * interface so that it can produce function arguments for the function f to
   * be minimized.
   * <li> mcs.numthreads, $num$ optional, the number of threads to use,
   * default is 1.
   * <li> rndgen,$num$,$num2$ mandatory, specifies the starting random
   * seed to use for each of the $num2$ threads to use (the value num2 must
   * equal the number given for the mcs.numthreads, or 1 if no such line is
   * present). The value of num should be a positive integer.
   * <li> any other parameters required for the evaluation of the function, or
   * by the objects passed in above (e.g. the RandomArgMakerIntf object etc.)
   *
   * <p> if the second optional argument is passed in, it overrides the random
   * seed specified in the params_file.
   * <p> The optional third argument, if present, overrides any max. limit set
   * on the number of function evaluations allowed specified in the params_file.
   *
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long start_time = System.currentTimeMillis();
			DblArray1VectorThreadLocalPools.setPoolSize(1);  // 1 vector/thread needed
      Hashtable params = utils.DataMgr.readPropsFromFile(args[0]);
      if (args.length>1) {
        long seed = Long.parseLong(args[1]);
        RndUtil.getInstance().setSeed(seed);  // updates all extra instances too!
      }
      if (args.length>2) {
        long num = Long.parseLong(args[2]);
        params.put("maxfuncevalslimit",new Long(num));  // this only "cuts" any 
				// larger limit set; also it is useless without the wrapper_func
				// the below method correctly sets the limit on function evaluations
				params.put("mcs.numtries", new Integer(Integer.parseInt(args[2])));
      }
      FunctionIntf func = (FunctionIntf) params.get("mcs.function");
      FunctionBase wrapper_func = new FunctionBase(func);
      params.put("mcs.function",func);  // used to be wrapper_func
      // FunctionIntf func = (FunctionIntf) params.get("mcs.function");
      MCS opter = new MCS(params);
      utils.PairObjDouble p = opter.minimize(func);  // used to be wrapper_func  
			if (p.getArg() instanceof double[]) {
				double[] arg = (double[]) p.getArg();
				System.out.print("best soln found:[");
				for (int i=0;i<arg.length;i++) System.out.print(arg[i]+" ");
			} else if (p.getArg() instanceof DblArray1Vector) {
				DblArray1Vector arg = (DblArray1Vector) p.getArg();
				System.out.print("best soln found:[");
				for (int i=0;i<arg.getNumCoords();i++) System.out.print(arg.getCoord(i)+" ");
			}
      System.out.println("] VAL="+p.getDouble());
      long dur = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+dur);
      System.out.println("VVV,"+p.getDouble()+",TTT,"+dur+",NNN,"+wrapper_func.getEvalCount()+",PPP,MCS,FFF,"+args[0]);  // for parser program to extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

