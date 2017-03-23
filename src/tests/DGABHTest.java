package tests;

import popt4jlib.*;
import utils.*;
import popt4jlib.BH.*;
import java.util.*;

/**
 * A test driver class for testing the <CODE>BH.DGABH</CODE> class for
 * mathematical optimization implementing the Generalized Adaptive Basin Hopping 
 * method in a parallel/distributed version.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DGABHTest {
	
  /**
   * public no-arg no-op constructor.
   */
  public DGABHTest() {
    // no-op
  }


  /**
   * invoke from the command-line as:
   * <CODE>java -cp &lt;classpath&gt; tests.DGABHTest &lt;params_file&gt; [random_seed] [maxfuncevals]</CODE>.
   * The params_file must contain lines of the following form:
	 * <ul>
   * <li> class,dgabh.function, &lt;fullclassname&gt;[,args] mandatory, 
	 * specifies the full java class name of the function to be optimized 
	 * (implementing the <CODE>popt4jlib.FunctionIntf</CODE> interface.) The 
	 * function must accept <CODE>popt4jlib.VectorIntf</CODE> objects as args.
   * <li> class,[dgabh.]randomparticlemaker,&lt;fullclassname&gt;[,args] 
	 * mandatory, the full class name of the 
	 * <CODE>popt4jlib.RandomChromosomeMakerIntf</CODE> object responsible for 
	 * creating valid random chromosome Objects to populate the islands.
   * <li> dgabh.numthreads,$num$ optional, how many threads will 
	 * be used, default is 1. Each thread corresponds to an island in the DGA 
	 * model.
   * <li> class,[dgabh.]c2amaker,&lt;fullclassname&gt; optional, the 
	 * object implementing <CODE>popt4jlib.Chromosome2ArgMakerIntf</CODE> that is 
	 * responsible for tranforming a chromosome Object to a function argument 
	 * Object. If not present, the default identity transformation is assumed.
   * <li> class,[dgabh.]a2cmaker,&lt;fullclassname&gt;[,args] optional, the 
	 * object implementing <CODE>popt4jlib.Arg2ChromosomeMakerIntf</CODE> that is 
	 * responsible for transforming a FunctionIntf argument Object to a chromosome 
	 * Object. If not present, the default identity transformation is assumed. 
	 * The a2c object is not only useful when other ObserverIntf 
	 * objects register for this SubjectIntf object and also add back solutions to 
	 * it (as FunctionIntf args), but also because the local-optimization process 
	 * that is part of the Basin-Hopping algorithm, may work on a different 
	 * representation space than the one the Basin-Hopping works (though there 
	 * does not seem to be any benefit from such a transformation).
   * <li> [dgabh.]numgens, $num$; optional, the number of iterations to run the 
	 * DGABH algorithm, default is 1.
   * <li> [dgabh.]immprob, $val$ optional, the probability with which a 
	 * sub-population will send some of its members to migrate to another 
	 * (island) sub-population, default is 0.01.
   * <li> [dgabh.]numinitpop,$num$ optional, the initial population number for 
	 * each island, default is 10.
	 * <li> class,[dgabh.]immigrationrouteselector,&lt;fullclassname&gt;[,args] 
	 * optional, the full class name of the 
	 * <CODE>popt4jlib.ImmigrationIslandOpIntf</CODE> object defining 
	 * the routes of immigration from island to island; default is null, which 
	 * forces the built-in unidirectional ring routing topology.
	 * <p>Notice that for all keys above if the exact key of the form "dgabh.X" is 
	 * not found in the params, then the params will be searched for key "X" alone
	 * so that if key "X" exists in the params, it will be assumed that its value
	 * corresponds to the value of "dgabh.X" originally sought.
	 * </p>
   * <li> rndgen,$num$,$num2$ mandatory, specifies the starting random
   * seed to use for each of the $num2$ threads to use (the value num2 must
   * equal the number given in the line for dgabh.numthreads). The value of num
   * should be a positive integer.
	 * <li>dbglvl,$num$ optional, sets the debugging level for this run. Debug
	 * level of 0 (zero) essentially prevents any debugging statements produced 
	 * via calls to <CODE>utils.Messenger</CODE> to be printed; higher numbers 
	 * produce more verbose output. Default is +Infinity which allows all 
	 * debugging calls <CODE>utils.Messenger.msg(stmt,lvl);</CODE> to be printed.
	 * <li>class,dgabh.chromosomeperturber,&lt;fullclassname&gt;[,args] mandatory,
	 * the object implementing <CODE>popt4jlib.BH.ChromosomePerturberIntf</CODE> 
	 * responsible for producing new individuals that are (presumably small) 
	 * perturbations of an original starting individual. Extra parameters that the 
	 * implementing perturber object requires must also be present.
	 * <li>class,dgabh.localoptimizer,&lt;fullclassname&gt;[,args] optional, the
	 * object implementing <CODE>popt4jlib.LocalOptimizerIntf</CODE> that is 
	 * responsible for performing a local-search around a starting point and 
	 * returning the best individual found by local-search. Missing such a line 
	 * implies no local-search process. As with above, any additional parameters 
	 * that the implementing locOpt object requires must also be present.
	 * <li> class,dgabh.pdbtexecinitedwrkcmd,&lt;fullclassname&gt;[args] optional, 
	 * if present, the full class name of the initialization command to send to 
	 * the network of workers to run function evaluation tasks, followed by the 
	 * constructor's arguments if any; default is null, indicating no distributed 
	 * computation.
	 * <li> dgabh.pdbthost, &lt;hostname&gt; optional, the name
	 * of the server to send function evaluation requests, default is localhost.
	 * <li> dgabh.pdbtport, $num$ optional, the port the above server listens to 
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
      FunctionIntf func = (FunctionIntf) params.get("dgabh.function");
      FunctionBase wrapper_func = new FunctionBase(func);
      DGABH opter = new DGABH(params);
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

