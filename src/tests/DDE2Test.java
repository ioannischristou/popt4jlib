package tests;

import popt4jlib.*;
import popt4jlib.DE.*;
import java.util.*;
import utils.RndUtil;

/**
 * Test-driver class for the (Distributed) Differential Evolution algorithm
 * implemented in class <CODE>popt4jlib.DE.DDE2</CODE>. Almost the same as 
 * <CODE>DDETest</CODE>, but runs the <CODE>DDE2</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DDE2Test {
  /**
   * single public no-arg constructor
   */
  public DDE2Test() {
  }


  /**
   * run as <CODE> java -cp &lt;classpath&gt; tests.DDE2Test &lt;params_file&gt; [random_seed] [maxfuncevals]</CODE>.
   * The params_file must contain lines of the following form:
	 * <ul>
   * <li> class,dde.function, &lt;fullclasspathname&gt;  mandatory, the java class
   * name defining the function to be optimized, which must accept
   * as arguments either <CODE>double[]</CODE> or <CODE>VectorIntf</CODE>
   * objects.
   * <li> class,dde.localoptimizer, &lt;fullclasspathname&gt; optional the java
   * class name of an object implementing the LocalOptimizerIntf defined in
   * the popt4jlib package, to be used as further optimizer of the best solution 
	 * found by the DE process.
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
   * must satisfy x_i &ge; $num$, default is -infinity
   * <li> dde.maxargval, $num$ optional, a double number that is an upper
   * bound for all variables of the optimization process, i.e. all variables
   * must satisfy x_i &le; $num$, default is +infinity
   * <li> dde.minargval$i$, $num$ optional, a double number that is a lower
   * bound for the i-th variable of the optimization process, i.e. variable
   * must satisfy x_i &ge; $num$, default is -infinity
   * <li> dde.maxargval$i$, $num$ optional, a double number that is an upper
   * bound for the i-th variable of the optimization process, i.e. variable
   * must satisfy x_i &le; $num$, default is +infinity
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
	 * <li> dde2.numgensbetweenbarrier, $val$ optional, an integer
	 * specifying the number of generations between two successive barrier calls
	 * among the threads participating in the DDE2 process, default is 
	 * <CODE>Integer.MAX_VALUE</CODE>; also notice that this parameter is 
	 * meaningless when the "dde.nondeterminismok" flag is false.
	 * <li> dde.countfuncevals, $bool_val$ optional, boolean value indicating 
	 * whether or not the process should be counting the number of function 
	 * evaluations it performs, default is false
	 * <li> dde.dmpaddress, $str_val$ optional, if existing, 
	 * specifies the location of a distributed Msg-Passing server that implements
	 * the basic send/recv operations as specified in 
	 * <CODE>parallel.distributed.DActiveMsgPassingCoordinatorLongLivedConnSrv[Clt]</CODE>
	 * default is null
	 * <li> dde.dmpport, $num$ optional, if existing, 
	 * specifies the port number of a distributed Msg-Passing server implementing
	 * the basic send/recv operations as specified in 
	 * <CODE>parallel.distributed.DActiveMsgPassingCoordinatorLongLivedConnSrv[Clt]</CODE>
	 * default is null
	 * <li> dde.dmpthisprocessid, $num$ optional, if existing, it 
	 * indicates the id of this process (this is the number to use in a 
	 * recvData(myid) call on the DActiveMsgPassingCoordinatorLongLivedConnClt
	 * object) default is null
	 * <li> dde.dmpnextprocessid, $num$ optional, if existing, it
	 * indicates the id of the process to which this process should be 
	 * sending "migrants" to; (this is the number to use as the "send address" in 
	 * a sendData(myid, id, data) DActiveblahblah call); default is null
	 * <li> dde.numgensbetweenmigrations, $num$ optional, if 
	 * existing, it indicates the number of generations that must pass between two
	 * successive "migrations" between DDE island-processes; default is 10
	 * <li> dde.nummigrants,$num$ optional, if it exists, indicates
	 * how many migrants will be sent and received from each DDE2 process; default
	 * is 10
	 * <li> dde.reducerhost, $str_val$ optional, if existing, it 
	 * indicates the address in which the reducer server resides; default is null
	 * <li> dde.reducerport, $num$ optional, if existing, it
	 * indicates the address in which the reducer server process listens at; 
	 * default is -1
   * </ul>
	 * <p> Notice that in case of running DDE2 in a distributed manner, there are
	 * two important constraints: 
	 * <ul>
	 * <li> First of all, the various processes participating in the distributed 
	 * DDE process must have their dde.dmpthisprocessid and dde.dmpnextprocessid
	 * parameters set up so that the flow of migration forms an exact ring, e.g.
	 * for a 3-process DDE we have that DDE_0 sends migrants to DDE_1 which sends 
	 * migrants to DDE_2 which sends migrants to DDE_0. 
	 * <li> The constraint dde.numgensbetweenmigrations &le; dde.numtries must 
	 * hold.
	 * </ul>
	 * Otherwise, there is no way for processes to block in at least one migration 
	 * cycle (and thereby have the DReduceSrv know the total number of processes 
	 * before the final reduce operation), and therefore the distributed reduce 
	 * operation afterwards is not guaranteed to work properly.
   * <p> if the second optional argument is passed in, it overrides the random
   * seed specified in the params_file (specified as the 1st arg in cmd-line).
	 * </p>
   * <p> The optional third argument, if present, overrides any max. limit set
   * on the number of function evaluations allowed. After this limit, the
   * function will always return Double.MAX_VALUE instead, and won't increase
   * the evaluation count. Obviously, for this limit to be enforced, the 
	 * "dde.countfuncevals" flag must be set to true in the params_file (1st arg
	 * in the command line)</p>
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
			Integer popsizeI = (Integer) params.get("dde.popsize");
			int popsize = popsizeI==null ? 10 : popsizeI.intValue();
			Integer ntI = (Integer) params.get("dde.numthreads");
			int nt = ntI==null ? 1 : ntI.intValue();
			int poolsize = popsize/nt + 5;
			popt4jlib.DblArray1VectorThreadLocalPools.setPoolSize(poolsize);
      FunctionIntf func = (FunctionIntf) params.get("dde.function");
      if (params.containsKey("dde.countfuncevals") && 
					((Boolean) params.get("dde.countfuncevals")).booleanValue()==true) {
				FunctionBase wrapper_func = new FunctionBase(func);
				params.put("dde.function",wrapper_func);
				func = wrapper_func;
			}
      DDE2 opter = new DDE2(params);
      utils.PairObjDouble p = opter.minimize(func);
      VectorIntf arg = (VectorIntf) p.getArg();
      System.out.print("best soln found:[");
      for (int i=0;i<arg.getNumCoords();i++) 
				System.out.print(arg.getCoord(i)+" ");
      System.out.println("] VAL="+p.getDouble());
      // final local optimization
			utils.PairObjDouble p2 = null;
      LocalOptimizerIntf lasdst = 
				(LocalOptimizerIntf) params.get("dde.localoptimizer");
      if (lasdst!=null) {
        VectorIntf x0 = arg.newInstance();  // arg.newCopy();
        params.put("gradientdescent.x0", x0);
        lasdst.setParams(params);
        p2 = lasdst.minimize(func);
        VectorIntf xf = (VectorIntf) p2.getArg();
        System.out.print(
            "Optimized (via a GradientDescent local method) best soln found:[");
        for (int i = 0; i < xf.getNumCoords(); i++) System.out.print(xf.
            getCoord(i) + " ");
        System.out.println("] VAL=" + p2.getDouble());
      }
      if (func instanceof FunctionBase) 
				System.err.println("total function evaluations="+
					                 ((FunctionBase)func).getEvalCount());
      long dur = System.currentTimeMillis()-start_time;
      double val = (p2==null || p2.getDouble()>=Double.MAX_VALUE) ? 
				             p.getDouble() : p2.getDouble();
			System.out.println("total time (msecs): "+dur);
      if (func instanceof FunctionBase)
				System.out.println("VVV,"+val+",TTT,"+dur+
					                 ",NNN,"+((FunctionBase)func).getEvalCount()+
					                 ",PPP,DDE2,FFF,"+args[0]);  // for parser program to 
			                                                 // extract from output
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
