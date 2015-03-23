package tests;

import popt4jlib.*;
import popt4jlib.GA.*;
import popt4jlib.GradientDescent.*;
import java.util.*;
import utils.RndUtil;

/**
 * Test-driver program for Distributed Genetic Algorithm meta-heuristic optimizer.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DGATest {

  /**
   * public no-arg (no-op) constructor
   */
  public DGATest() {

  }


  /**
   * run as <CODE> java -cp &ltclasspath&gt tests.DGATest &ltparams_file&gt [random_seed] [maxfuncevalslimit]</CODE>
   * where the params_file must contain lines of the following form:
   * <li> class,dga.function, &ltfullclassnameoffunction&gt mandatory, the name
   * of the java class defining the function to be optimized
   * <li> function.maxevaluationtime, $num$ optional, the maximum wall-clock time
   * allowed for any function evaluation to complete (in milliseconds).
   * <li> function.reentrantMT, $bool_value$ optional, if present and $bool_value$
   * evaluates to true, indicates that the function to be optimized is not
   * thread-safe, but does not contain any static data members that can destroy
   * the evaluations of two different function objects, so that different threads
   * evaluating the function on different arguments using different function
   * objects are safe.
   * <li> dga.numthreads, $num$ optional, indicates how many islands (each on
   * its own thread) will be created for this DGA run. Default is 1.
   * <li> rndgen,$num$,$num2$ mandatory, specifies the starting random
   * seed to use for each of the $num2$ threads to use (the value num2 must
   * equal the number given for the dde.numthreads, or 1 if no such line is
   * present). The value of num should be a positive integer.
   * <li> class,dga.observerlocaloptimizer, &ltfullclassname&gt optional, the
   * full class name of the java class implementing the <CODE>ObserverIntf</CODE>
   * in the popt4jlib pacakge as well as the <CODE>LocalOptimizerIntf</CODE> in
   * popt4jlib.GradientDescent package. Used for constructing ensembles of
   * optimizers exchanging (sub-)optimal incumbent solutions as they are found.
   * Can only be used with functions accepting as arguments <CODE>double[]</CODE>
   * or <CODE>VectorIntf</CODE>.
   * <li> class,dga.localoptimizer, &ltfullclassname&gt optional, the full class
   * name of the java class implementing the <CODE>LocalOptimizerIntf</CODE> in
   * popt4jlib.GradientDescent package. Used as a final post-optimization step.
   * Can only be used with functions accepting as arguments <CODE>double[]</CODE>
   * or <CODE>VectorIntf</CODE>.
   * <li> class,dga.randomchromosomemaker,&ltfullclassname&gt mandatory, the
   * full class name of the java class implementing the RandomChromosomeMakerIntf
   * interface in the popt4jlib package, responsible for creating valid random
   * chromosome Objects to populate the islands.
   * <li> class,dga.xoverop, &ltfullclassname&gt mandatory, the full class name
   * of the java class implementing the XoverOpIntf interface in the popt4jlib.GA
   * package that produces two new chromosome Objects from two old chromosome
   * Objects. It is the responsibility of the operator to always return NEW Objects.
   * <li> class,dga.mutationop, &ltfullclassname&gt optional, the full class name
   * of the class implementing the MutationOpIntf interface in the popt4jlib.GA
   * pavkage. If present, the operator will always be applied to the resulting
   * Objects that the XoverOpIntf will produce.
   * <li> class,dga.c2amaker, &ltfullclassname&gt optional, the full class name
   * of the class implementing the Chromosome2ArgMakerIntf interface in package
   * popt4jlib that is responsible for tranforming a chromosome Object to a
   * function argument Object. If not present, the default identity
   * transformation is assumed.
   * <li> class,dga.a2cmaker, &ltfullclassname&gt optional, the full class name
   * of the class implementing the Arg2ChromosomeMakerIntf in the popt4jlib package
   * responsible for transforming a FunctionIntf argument Object to a chromosome
   * Object. If not present, the default identity transformation is assumed. The
   * a2c object is only useful when other ObserverIntf objects register for this
   * SubjectIntf object and also add back solutions to it (as FunctionIntf args)
   * <li> dga.numgens, $num$ optional, the number of generations to run the
   * GA, default is 1.
   * <li> dga.numinitpop, $num$ optional, the initial population number for
   * each island, default is 10.
   * <li> dga.poplimit, $num$ optional, the maximum population for each
   * island, default is 100.
   * <li> dga.xoverprob, $num$ optional, the square of the expectation
   * of the number of times cross-over will occur divided by island population
   * size, default is 0.7.
   * <li> dga.cutoffage, $num$ optional, the number of generations an
   * individual is expected to live before being removed from the population,
   * default is 5.
   * <li> dga.varage, $num$ optional, the variance in the number of
   * generations an individual will remain in the population before being
   * removed, default is 0.9.
   * <li> maxfuncevalslimit, $num$ optional, the max. number of function
   * evaluations allowed in the process.
   *
   * <li> In addition, the various operators (xover, mutation, a2cmaker,
   * c2amaker, randomchromosomemaker etc.) may require additional parameters as
   * they are defined and documented in their respective class file definitions.
   *
   * <p> The optional second argument, if present, overrides the initial random
   * seed specified in the params_file.
   *
   * <p> The optional third argument, if present, overrides any max. limit set
   * on the number of function evaluations allowed. After this limit, the
   * function will always return Double.MAX_VALUE instead, and won't increase
   * the evaluation count.
   * @param args String[]
   */
  public static void main(String[] args) {
    try {
      long start_time = System.currentTimeMillis();
      Hashtable params = utils.DataMgr.readPropsFromFile(args[0]);
      FunctionIntf func = (FunctionIntf) params.get("dga.function");
      Integer maxfuncevalI = (Integer) params.get("function.maxevaluationtime");
      Boolean doreentrantMT = (Boolean) params.get("function.reentrantMT");
      int nt = 1;
      Integer ntI = (Integer) params.get("dga.numthreads");
      if (ntI!=null) nt = ntI.intValue();
      if (args.length>1) {
        long seed = Long.parseLong(args[1]);
        RndUtil.getInstance().setSeed(seed);  // updates all extra instances too!
      }
      if (args.length>2) {
        long num = Long.parseLong(args[2]);
        params.put("maxfuncevalslimit",new Long(num));
      }
      FunctionIntf wrapper_func = null;
      if (maxfuncevalI!=null && maxfuncevalI.intValue()>0)
        wrapper_func = new LimitedTimeEvalFunction(func, maxfuncevalI.longValue());
      else if (doreentrantMT!=null && doreentrantMT.booleanValue()==true) {
        wrapper_func = new ReentrantFunctionBaseMT(func, nt);
      }
      else wrapper_func = new FunctionBase(func);
      params.put("dga.function",wrapper_func);
      DGA opter = new DGA(params);
      // check for an ObserverIntf
      ObserverIntf obs = (ObserverIntf) params.get("dga.observerlocaloptimizer");
      if (obs!=null) {
        opter.registerObserver(obs);
        ((LocalOptimizerIntf) obs).setParams(params);
      }
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      double[] arg = null;
      try {
        arg = (double[]) p.getArg();
      }
      catch (ClassCastException e) {
	try {
          DblArray1Vector y = (DblArray1Vector) p.getArg();
          arg = y.getDblArray1();
	}
	catch (ClassCastException e2) {
	  // no-op
	}
      }
			utils.PairObjDouble p2 = null;
      if (arg!=null) {
        System.out.print("best soln found:[");
        for (int i = 0; i < arg.length; i++) System.out.print(arg[i] + " ");
        System.out.println("] VAL=" + p.getDouble());
/*
        // finally see how mutation went
        MutationOpIntf mop = (MutationOpIntf) params.get("dga.mutationop");
        if (mop!=null && mop instanceof DblArray1GradientDescentMutationOp) {
          int s = ((DblArray1GradientDescentMutationOp) mop).getSuccessfulMutations();
          int f = ((DblArray1GradientDescentMutationOp) mop).getFailedMutations();
          System.err.println("successful mutations="+s+" failed mutations="+f);
        }
*/
      // final local optimization
        LocalOptimizerIntf lasdst = (LocalOptimizerIntf) params.get("dga.localoptimizer");
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
      } else System.err.println("DGA did not find any solution.");
      double val = (p2==null || p2.getDouble()>=Double.MAX_VALUE) ? p.getDouble() : p2.getDouble();
			if (wrapper_func instanceof FunctionBase)
        System.out.println("total function evaluations="+((FunctionBase) wrapper_func).getEvalCount());
      else if (wrapper_func instanceof ReentrantFunctionBase)
        System.out.println("total function evaluations="+((ReentrantFunctionBase) wrapper_func).getEvalCount());
      else if (wrapper_func instanceof ReentrantFunctionBaseMT)
        System.out.println("total function evaluations="+((ReentrantFunctionBaseMT) wrapper_func).getEvalCount());
      else {
        LimitedTimeEvalFunction f = (LimitedTimeEvalFunction) wrapper_func;
        System.out.println("total function evaluations="+f.getEvalCount()+
                           " total SUCCESSFUL function evaluations="+f.getSucEvalCount());
      }
      long end_time = System.currentTimeMillis();
      long dur = end_time-start_time;
      /*
      System.err.println("Total #DblArray1Vector objects created="+DblArray1Vector.getTotalNumObjs());
      */
     System.out.println("total time (msecs): "+dur);
     if (wrapper_func instanceof FunctionBase)
       System.out.println("VVV,"+val+",TTT,"+dur+",NNN,"+((FunctionBase)wrapper_func).getEvalCount()+",PPP,DGA,FFF,"+args[0]);  // for parser program to extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
