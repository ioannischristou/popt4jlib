package tests;

import popt4jlib.*;
import popt4jlib.GA.*;
import popt4jlib.GradientDescent.*;
import java.util.*;

/**
 * Test-driver program for Distributed Genetic Algorithm meta-heuristic optimizer
 * using the socket-eval function.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class SimpleDGASocketEvalTest {

  /**
   * public no-arg (no-op) constructor
   */
  public SimpleDGASocketEvalTest() {

  }


  /**
   * run as <CODE> java -cp &lt;classpath&gt; tests.SimpleDGASocketEvalTest &lt;params_file&gt; </CODE>
   * where the params_file must contain lines of the following form:
   * <li> dga.function.hostname, &lt;hostname&gt; optional, the name
   * of the host where the process computing the function to be minimized runs.
   * Default is localhost
   * <li> dga.function.port, $num$ optional, the port where the process computing
   * the function to be minimized listens for arguments. Default is 4444.
   * <li> function.maxevaluationtime, $num$ optional, the maximum wall-clock time
   * allowed for any function evaluation to complete (in milliseconds).
   * <li> dga.numthreads, $num$ optional, indicates how many islands (each on
   * its own thread) will be created for this DGA run. Default is 1.
   * <li> rndgen,$num$,$num2$ mandatory, specifies the starting random
   * seed to use for each of the $num2$ threads to use (the value num2 must
   * equal the number given for the dde.numthreads, or 1 if no such line is
   * present). The value of num should be a positive integer.
   * <li> class,dga.randomchromosomemaker,&lt;fullclassname&gt; mandatory, the
   * full class name of the java class implementing the RandomChromosomeMakerIntf
   * interface in the popt4jlib package, responsible for creating valid random
   * chromosome Objects to populate the islands.
   * <li> class,dga.xoverop, &lt;fullclassname&gt; mandatory, the full class name
   * of the java class implementing the XoverOpIntf interface in the popt4jlib.GA
   * package that produces two new chromosome Objects from two old chromosome
   * Objects. It is the responsibility of the operator to always return NEW Objects.
   * <li> class,dga.mutationop, &lt;fullclassname&gt; optional, the full class name
   * of the class implementing the MutationOpIntf interface in the popt4jlib.GA
   * pavkage. If present, the operator will always be applied to the resulting
   * Objects that the XoverOpIntf will produce.
   * <li> class,dga.c2amaker, &lt;fullclassname&gt; optional, the full class name
   * of the class implementing the Chromosome2ArgMakerIntf interface in package
   * popt4jlib that is responsible for tranforming a chromosome Object to a
   * function argument Object. If not present, the default identity
   * transformation is assumed.
   * <li> class,dga.a2cmaker, &lt;fullclassname&gt; optional, the full class name
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
   *
   * <li> In addition, the various operators (xover, mutation, a2cmaker,
   * c2amaker, randomchromosomemaker etc.) may require additional parameters as
   * they are defined and documented in their respective class file definitions.
   * @param args String[]
   */
  public static void main(String[] args) {
    DGA opter=null;
    FunctionIntf wrapper_func=null;
    try {
      long start_time = System.currentTimeMillis();
      HashMap params = utils.DataMgr.readPropsFromFile(args[0]);
      String hostname = (String) params.get("dga.function.hostname");
      int port = ((Integer) params.get("dga.function.port")).intValue();
      ReentrantSocketEvalFunctionBase func = null;
      Boolean send_paramsB = (Boolean) params.get("function.sendparams");
      if (send_paramsB!=null && send_paramsB.booleanValue())
        func = new ReentrantSocketEvalFunctionBase(hostname, port, true);
      else func = new ReentrantSocketEvalFunctionBase(hostname, port);
      Integer maxfuncevalI = (Integer) params.get("function.maxevaluationtime");
      if (maxfuncevalI!=null && maxfuncevalI.intValue()>0)
        wrapper_func = new LimitedTimeEvalFunction(func, maxfuncevalI.longValue());
      else wrapper_func = func;
      params.put("dga.function",wrapper_func);
      opter = new DGA(params);
      utils.PairObjDouble p = opter.minimize(wrapper_func);
      double[] arg = null;
      try {
        arg = (double[]) p.getArg();
      }
      catch (ClassCastException e) {
        // no-op
      }
      if (arg!=null) {
        System.out.print("best soln found:[");
        for (int i = 0; i < arg.length; i++) System.out.print(arg[i] + " ");
        System.out.println("] VAL=" + p.getDouble());
      }
      long end_time = System.currentTimeMillis();
      System.out.println("total time (msecs): "+(end_time-start_time));
    }
    catch (Exception e) {
      e.printStackTrace();
      if (opter!=null) {
        double[] cursol = (double[]) opter.getIncumbent();
        if (cursol!=null) {
          System.err.print("best soln found up to exception: [ ");
          for (int i = 0; i < cursol.length; i++) {
            System.err.print(cursol[i]+" ");
          }
          System.err.println("]");
        }
      }
      System.exit(-1);
    }
  }
}
