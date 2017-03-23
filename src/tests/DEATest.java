package tests;

import popt4jlib.LocalOptimizerIntf;
import popt4jlib.*;
import popt4jlib.EA.*;
import popt4jlib.GradientDescent.*;
import java.util.*;
import utils.RndUtil;

/**
 * test-driver for the (Distributed) Evolutionary Algorithm implemented in the
 * <CODE>popt4jlib.EA.DEA</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DEATest {
  /**
   * solo public no-arg constructor
   */
  public DEATest() {
  }

  /**
   * run as <CODE> java -cp &lt;classpath&gt; tests.DEATest &lt;params_file&gt; [random_seed] [maxfuncevals]</CODE>.
   * The params_file must contain lines of the following form:
	 * <ul>
   * <li> class,dea.function, &lt;fullclasspathname&gt;  mandatory, the java class
   * name defining the function to be optimized, which must accept
   * as arguments either <CODE>double[]</CODE> or <CODE>VectorIntf</CODE>
   * objects.
   * <li> function.maxevaluationtime, $num$ optional, the number of milliseconds
   * of wall-clock time that each function evaluation will be allowed to execute
   * (so that if the function takes more to execute, it will be considered that
   * the function at this point returns Double.MAX_VALUE)
   * <li> class,dea.localoptimizer, &lt;fullclasspathname&gt;[,args] optional 
	 * the java class name of an object implementing the LocalOptimizerIntf 
	 * defined in the popt4jlib package, to be used as further optimizer of
   * the best solution found by the DE process.
   * <li> class,dea.randomchromosomemaker, &lt;fullclassname&gt;[,args] 
	 * mandatory, the class name of the object responsible for implementing the
	 * <CODE>popt4jlib.RandomChromosomeMakerIntf</CODE> interface that allows 
	 * creating random initial chromosome objects.
   * <li> class,dea.movemaker, &lt;fullclassname&gt;[,args] mandatory, the 
	 * object responsible for implementing the interface NewChromosomeMakerIntf 
	 * that allows creating new chromosome Objects from an existing one (makes a 
	 * move).
   * <li> dea.numiters, $num$ optional, the number of iterations each
   * thread will go through in this evolutionary process run, default is 100.
   * <li> dea.sendrecvperiod, $num$ optional, the number of generations
   * before the threads communicate their best solution to the master DEA
   * process and subsequent receipt of the same best solution by all threads.
   * Default is 1.
   * <li> dea.numthreads, $num$ optional, the number of threads representing
   * islands for this evolutionary process run. Default is 1.
   * <li> rndgen,$num$,$num2$ mandatory, specifies the starting random
   * seed to use for each of the $num2$ threads to use (the value num2 must
   * equal the number given in the line for dde.numthreads). The value of num
   * should be a positive integer.
   * <li> class,dea.c2amaker, &lt;fullclassname&gt;[,args] optional, the object 
	 * that implements the Chromosome2ArgMakerIntf interface that transforms 
	 * chromosome Objects used in the evolutionary process -and manipulated by the 
	 * Object implementing the NewChromosomeMakerIntf interface- into argument 
	 * Objects that can be passed into the FunctionIntf object that the process 
	 * minimizes. Default is null, which results in the chromosome objects being 
	 * passed "as-is" to the FunctionIntf object being minimized.
   * </ul>
   * <p> if the optional second argument is also passed in, it overrides the 
	 * random seed specified in the params_file.</p>
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
      FunctionIntf func = (FunctionIntf) params.get("dea.function");
      Integer maxfuncevalI = (Integer) params.get("function.maxevaluationtime");
      FunctionIntf wrapper_func = null;
      if (maxfuncevalI!=null && maxfuncevalI.intValue()>0)
        wrapper_func = new LimitedTimeEvalFunction(func, maxfuncevalI.longValue());
      else wrapper_func = new FunctionBase(func);
      params.put("dea.function",wrapper_func);
      DEA opter = new DEA(params);
      /*
      // check for an ObserverIntf
      ObserverIntf obs = (ObserverIntf) params.get("dga.observerlocaloptimizer");
      if (obs!=null) {
        opter.registerObserver(obs);
        ((LocalOptimizerIntf) obs).setParams(params);
      }
      */
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      double[] arg = (double[]) p.getArg();
      if (wrapper_func instanceof FunctionBase)
        System.out.println("total function evaluations when DEA finishes="+((FunctionBase) wrapper_func).getEvalCount());
      else {
        LimitedTimeEvalFunction f = (LimitedTimeEvalFunction) wrapper_func;
        System.out.println("total function evaluations when DEA finishes="+f.getEvalCount()+
                           " total SUCCESSFUL function evaluations="+f.getSucEvalCount());
      }
			utils.PairObjDouble p2 = null;
      if (arg!=null) {
        System.out.print("best soln found:[");
        for (int i = 0; i < arg.length; i++) System.out.print(arg[i] + " ");
        System.out.println("] VAL=" + p.getDouble());
        // final local optimization
        LocalOptimizerIntf lasdst = (LocalOptimizerIntf) params.get("dea.localoptimizer");
        if (lasdst!=null) {
          VectorIntf x0 = new popt4jlib.DblArray1Vector(arg);
          params.put("gradientdescent.x0", x0);
          lasdst.setParams(params);
          p2 = lasdst.minimize(wrapper_func);
          VectorIntf xf = (VectorIntf) p2.getArg();
          System.out.print("Optimized (via LocalOptimizer) best soln found:[");
          for (int i = 0; i < xf.getNumCoords(); i++)
            System.out.print(xf.getCoord(i) + " ");
          System.out.println("] VAL=" + p2.getDouble());
        }
      }  else System.err.println("DEA did not find any solution.");
      if (wrapper_func instanceof FunctionBase)
        System.out.println("total function evaluations="+((FunctionBase) wrapper_func).getEvalCount());
      else {
        LimitedTimeEvalFunction f = (LimitedTimeEvalFunction) wrapper_func;
        System.out.println("total function evaluations="+f.getEvalCount()+
                           " total SUCCESSFUL function evaluations="+f.getSucEvalCount());
      }
      long end_time = System.currentTimeMillis();
      long dur = end_time-start_time;
      double val = (p2==null || p2.getDouble()>=Double.MAX_VALUE) ? p.getDouble() : p2.getDouble();
      System.out.println("total time (msecs): "+dur);
      if (wrapper_func instanceof FunctionBase)
        System.out.println("VVV,"+val+",TTT,"+dur+",NNN,"+((FunctionBase)wrapper_func).getEvalCount()+",PPP,DEA,FFF,"+args[0]);  // for parser program to extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
