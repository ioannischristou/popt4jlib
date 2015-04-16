package tests;

import popt4jlib.*;
import popt4jlib.SA.*;
import popt4jlib.GradientDescent.*;
import java.util.*;
import utils.RndUtil;

/**
 * Test-driver class for executing the SA algorithm implemented in the
 * <CODE>popt4jlib.SA.DSA</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DSATest {
  /**
   * public no-arg constructor
   */
  public DSATest() {
  }


  /**
   * run as <CODE> java -cp &lt;classpath&gt; tests.DSATest &lt;params_file&gt; [random_seed] [maxfuncevals]</CODE>
   * where the params_file must contain lines of the following form:
	 * <ul>
   * <li> class,dsa.function, &lt;fullclasspathname&gt;  mandatory, the java class
   * name defining the function to be optimized, which must accept
   * as arguments either <CODE>double[]</CODE> or <CODE>VectorIntf</CODE>
   * objects.
   * <li> class,dsa.randomchromosomemaker, &lt;fullclassname&gt; mandatory the class
   * name of a RandomChromosomeMakerIntf object responsible for implementing the
   * interface that allows creating random initial chromosome objects.
   * <li> class,dsa.movemaker, &lt;fullclassname&gt; mandatory, the  object responsible
   * for implementing the interface NewChromosomeMakerIntf that allows creating
   * new chromosome Objects from an existing one (makes a move).
   * <li> dsa.numouteriters, $num$ optional, the total number of "generations"
   * i.e. outer iterations to run, default is 100.
   * <li> dsa.numthreads, $num$ optional, the number of threads representing
   * islands for this evolutionary process run. Default is 1.
   * <li> rndgen,$num$,$num2$ mandatory, specifies the starting random
   * seed to use for each of the $num2$ threads to use (the value num2 must
   * equal the number given for the dsa.numthreads, or 1 if no such line is
   * present). The value of num should be a positive integer.
   * <li> class,dsa.c2amaker, &lt;fullclassname&gt; optional, the object that
   * implements the Chromosome2ArgMakerIntf interface that transforms chromosome
   * Objects used in the evolutionary process -and manipulated by the Object
   * implementing the NewChromosomeMakerIntf interface- into argument Objects
   * that can be passed into the FunctionIntf object that the process minimizes.
   * Default is null, which results in the chromosome objects being passed "as-is"
   * to the FunctionIntf object being minimized.
   * <li> dsa.numtriesperiter, $num$ optional, the number of
   * iterations each thread will run before communicating its incumbent solution
   * to all others, and get the best overall incumbent -among running threads-
   * to continue with, default is 100.
   * <li> class,dsa.schedule, &lt;fullclasspathname&gt; optional, class name of the
   * object implementing the SAScheduleIntf that determines the temperature at
   * each "generation" i.e. outer iteration. Default is LinScaleSchedule, that
   * can be configured by the following two optional parameters:
   * <li> dsa.T0, $num$ optional, initial temperature, default is 1000.0
   * <li> dsa.K, $num$ optional, the "Boltzman constant", default is 20.0
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
      Hashtable params = utils.DataMgr.readPropsFromFile(args[0]);
      if (args.length>1) {
        long seed = Long.parseLong(args[1]);
        RndUtil.getInstance().setSeed(seed);  // updates all extra instances too!
      }
      if (args.length>2) {
        long num = Long.parseLong(args[2]);
        params.put("maxfuncevalslimit",new Long(num));
      }
      FunctionIntf func = (FunctionIntf) params.get("dsa.function");
      FunctionBase wrapper_func = new FunctionBase(func);
      params.put("dsa.function",wrapper_func);
      DSA opter = new DSA(params);
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      double[] arg = (double[]) p.getArg();
      System.out.print("best soln found:[");
      for (int i=0;i<arg.length;i++) System.out.print(arg[i]+" ");
      System.out.println("] VAL="+p.getDouble());
      long dur = System.currentTimeMillis()-start_time;
      System.out.println("total time (msecs): "+dur);
      // final local optimization
      LocalOptimizerIntf lasdst = (LocalOptimizerIntf) params.get("dsa.localoptimizer");
      utils.PairObjDouble p2 = null;
			if (lasdst!=null) {
        VectorIntf x0 = new popt4jlib.DblArray1Vector(arg);
        params.put("gradientdescent.x0", x0);
        lasdst.setParams(params);
        p2 = lasdst.minimize(wrapper_func);
        VectorIntf xf = (VectorIntf) p2.getArg();
        System.out.print(
            "Optimized (via LocalOptimizerIntf) best soln found:[");
        for (int i = 0; i < xf.getNumCoords(); i++) System.out.print(xf.
            getCoord(i) + " ");
        System.out.println("] VAL=" + p2.getDouble());
      }
      System.err.println("total function evaluations="+wrapper_func.getEvalCount());
      dur = System.currentTimeMillis()-start_time;
      double val = (p2==null || p2.getDouble()>=Double.MAX_VALUE) ? p.getDouble() : p2.getDouble();
      System.out.println("total time (msecs): "+dur);
      System.out.println("VVV,"+val+",TTT,"+dur+",NNN,"+wrapper_func.getEvalCount()+",PPP,DSA,FFF,"+args[0]);  // for parser program to extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
