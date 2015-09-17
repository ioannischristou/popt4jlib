package popt4jlib.LocalSearch;

import utils.*;
import popt4jlib.*;
import parallel.*;
import java.util.*;


/**
 * class implements a parallel LocalSearch method for combinatorial optimization
 * problems, obeying the general OptimizerIntf contract. The class uses
 * multi-threading to evaluate in parallel the available moves from any point,
 * and is itself thread-safe.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DLS implements OptimizerIntf {
  private static int _nextId=0;
  private int _id;
  private HashMap _params=null;
  private boolean _setParamsFromSameThreadOnly=false;
  private Thread _originatingThread=null;
  private Chromosome2ArgMakerIntf _c2amaker=null;
  private Arg2ChromosomeMakerIntf _a2cmaker=null;
  private AllChromosomeMakerIntf _allmovesmaker=null;
  double _incValue=Double.MAX_VALUE;
  Object _inc=null;  // incumbent chromosome
  FunctionIntf _f=null;


  /**
   * public constructor of a DLS OptimizerIntf. Assigns unique id among all DLS
   * objects (in the same JVM).
   */
  public DLS() {
    _id = incrID();
  }


  /**
   * public constructor of a DLS OptimizerIntf object. Assigns unique id among
   * all DLS objects, and sets the appropriate parameters for the optimization
   * process via the setParams(params) process. The parameters are discussed in
   * the javadoc for the <CODE>minimize(f)</CODE> method.
   * @param params HashMap
   */
  public DLS(HashMap params) {
    this();
    try {
      setParams(params);
    }
    catch (Exception e) {
      // no-op: cannot reach this point
    }
  }


  /**
   * Constructor of a DLS object, that assigns a unique id plus the parameters
   * passed into the argument. Also, it prevents other threads from modifying
   * the parameters passed into this object if the second argument is true.
   * @param params HashMap
   * @param setParamsOnlyFromSameThread boolean
   */
  public DLS(HashMap params, boolean setParamsOnlyFromSameThread) {
    this();
    try {
      setParams(params);
    }
    catch (Exception e) {
      // no-op: cannot reach this point
    }
    _setParamsFromSameThreadOnly = setParamsOnlyFromSameThread;
    if (setParamsOnlyFromSameThread) _originatingThread = Thread.currentThread();
  }


  /**
   * return a copy of the parameters. Modifications to the returned object
   * do not affect the original data member
   * @return HashMap
   */
  public synchronized HashMap getParams() {
    return new HashMap(_params);
  }


  /**
   * the optimization params are set to p
   * @param p HashMap
   * @throws OptimizerException if another thread is concurrently running the
   * <CODE>minimize(f)</CODE> method of this object. Note that unless the 2-arg
   * constructor DLS(params, use_from_same_thread_only=true) is used to create
   * this object, it is perfectly possible for one thread to call 
	 * <CODE>setParams(p)</CODE>, then another to <CODE>setParams(p2)</CODE> to 
	 * some other param-set, and then the first thread to call 
	 * <CODE>minimize(f)</CODE>.
   */
  public synchronized void setParams(HashMap p) throws OptimizerException {
    if (_f!=null) throw new OptimizerException("cannot modify parameters while running");
    if (_setParamsFromSameThreadOnly) {
      if (Thread.currentThread()!=_originatingThread)
        throw new OptimizerException("Current Thread is not allowed to call setParams() on this DLS.");
    }
    _params = null;
    _params = new HashMap(p);  // own the params
    try {
      _c2amaker = (Chromosome2ArgMakerIntf) _params.get("dls.c2amaker");
    }
    catch (ClassCastException e) {
      throw new OptimizerException("dls.c2amaker must be a Chromosome2ArgMakerIntf object");
    }
    try {
      _a2cmaker = (Arg2ChromosomeMakerIntf) _params.get("dls.a2cmaker");
    }
    catch (ClassCastException e) {
      throw new OptimizerException("dls.a2cmaker must be a Arg2ChromosomeMakerIntf object");
    }
    try {
      _allmovesmaker = (AllChromosomeMakerIntf) _params.get("dls.movesmaker");
    }
    catch (ClassCastException e) {
      throw new OptimizerException("dls.a2cmaker must be a AllChromosomeMakerIntf object");
    }
  }


  /**
   * The most important method of the class.
   * Prior to calling this method, some parameters must have been set, either
   * during construction of the object, or via a call to setParams(p).
   * The parameters are as follows:
   * <ul>
   * <li> &lt;"dls.x0", Object arg&gt; mandatory, the initial point from which to start
   * the local search.
   * <li> &lt;"dls.movesmaker", AllChromosomeMakerIntf movesmaker&gt; mandatory, the object
   * responsible for implementing the interface that allows creating ALL
   * chromosome Objects from an existing one (produces -by definition- the
   * entire neighborhood of the object).
   * <li> &lt;"dls.maxiters", Integer niters&gt; optional, the max number of iterations the
   * process will go through, default is <CODE>Integer.MAX_VALUE</CODE>.
   * <li> &lt;"dls.numthreads", Integer nt&gt; optional, the number of threads in the
   * threadpool to be used for exploring each possible move in the neighborhood.
   * Default is 1.
   * <li> &lt;"dls.a2cmaker", Arg2ChromosomeMakerIntf a2cmaker&gt; optional, an object
   * implementing the Arg2ChromosomeMakerIntf that transforms objects that can
   * be passed directly to the FunctionIntf being minimized to Chromomome objects
   * that can be used in the local-search process -and manipulated by the Object
   * implementing the AllChromosomeMakerIntf interface. Default is
   * null, which results in the arg objects being passed "as-is" to the
   * AllChromosomeMakerIntf object.
   * <li> &lt;"dls.c2amaker", Chromosome2ArgMakerIntf c2amaker&gt; optional, an object
   * implementing the Chromosome2ArgMakerIntf that transforms chromosome Objects
   * used in the localsearch process -and manipulated by the Object implementing
   * the AllChromosomeMakerIntf interface- into argument Objects that can be
   * passed into the FunctionIntf object that the process minimizes. Default is
   * null, which results in the chromosome objects being passed "as-is" to the
   * FunctionIntf object being minimized.
   * </ul>
   * <br>The result is a PairObjDouble object that contains the best function arg.
   * along with the minimum function value obtained by this argument (or null
   * if the process fails to find any valid function argument).</br>
   * @param f FunctionIntf the function to optimize locally
   * @throws OptimizerException if another thread is concurrently running the
   * same method of this object or if the optimization process fails
   * @return PairObjDouble an object that holds both the best value found by the
   * DLS process run as well as the argmin -the argument that produced this best
   * value.
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("DLS.minimize(f): null f");
    try {
      int nt = 1;
      int max_iters = Integer.MAX_VALUE;
      synchronized (this) {
        if (_f != null)throw new OptimizerException("DLS.minimize(): "+
          "another thread is concurrently executing the method on the same object");
        _f = f;
        reset();
        try {
          Integer ntI = (Integer) _params.get("dls.numthreads");
          if (ntI != null) nt = ntI.intValue();
        }
        catch (ClassCastException e) {
          e.printStackTrace();
        }
        if (nt < 1)throw new OptimizerException(
            "DLS.minimize(): invalid number of threads specified");
      }
      try {
        Integer niI = (Integer) _params.get("dls.maxiters");
        if (niI != null) max_iters = niI.intValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }

      Object x0 = _params.get("dls.x0");
      if (x0==null) {
        throw new OptimizerException("no initial point (with key dls.x0) specified in params");
      }
      // do the work
      try {
        ParallelBatchTaskExecutor executor = null;
        try {
          executor = ParallelBatchTaskExecutor.
										   newParallelBatchTaskExecutor(Math.max(nt,1));
        }
        catch (ParallelException e) {
          // no-op: never happens
        }
        HashMap p = getParams();
        Object x = x0;
        try {
          _incValue = _f.eval(x, p);
          if (_a2cmaker!=null) _inc = _a2cmaker.getChromosome(x0, p);
          else _inc = x0;
        }
        catch (IllegalArgumentException e) {
          e.printStackTrace();
        }
        if (_a2cmaker!=null) x = _a2cmaker.getChromosome(x0, p);
        int i=0;
        boolean cont = true;
				Messenger mger = Messenger.getInstance();
        while (i++ < max_iters && cont) {
          mger.msg("LocalSearch: executing iteration "+i,1);
          // createAllChromosomes(x,p) can be, and usually is, the hardest computing task
          // therefore, should be parallelized whenever possible
          Vector newchromosomes = _allmovesmaker.createAllChromosomes(x, p);
          if (newchromosomes==null || newchromosomes.size()==0) break;
          Vector newtasks = new Vector();
          for (int j=0; j<newchromosomes.size(); j++) {
            Object arg=null;
            if (_c2amaker == null || _c2amaker instanceof IdentityC2ArgMaker)
              arg = newchromosomes.elementAt(j);
            else arg = _c2amaker.getArg(newchromosomes.elementAt(j),p);
            FunctionEvaluationTask ftj = new FunctionEvaluationTask(getFunction(),
                                                                    arg,p);
            newtasks.add(ftj);
          }
          executor.executeBatch(newtasks);  // blocking
          cont = false;
          for (int j=0; j<newtasks.size(); j++) {
            FunctionEvaluationTask ftj = (FunctionEvaluationTask) newtasks.elementAt(j);
            if (ftj.isDone()==false) {  // this task failed to evaluate
							System.err.println("Task "+ftj+" failed to evaluate???");  // itc: HERE rm asap
							continue;
						}  
            double ftjval = ftj.getObjValue();
            if (ftjval<_incValue) {
              mger.msg("DLS.minimize(f): found better solution="+ftjval,0);
              _incValue = ftjval;
              _inc = newchromosomes.elementAt(j);
              x = _c2amaker==null || _c2amaker instanceof IdentityC2ArgMaker ?
                  _inc : _c2amaker.getArg(_inc, p);
              cont = true;
            }
          }
        }
        // shutdown the executor
        executor.shutDown();
        mger.msg("LocalSearch: completed after "+i+" iterations.",1);
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new OptimizerException("DLS.minimize(f): failed");
      }

      synchronized (this) {
        if (_inc==null)
          throw new OptimizerException("DLS.minimize(f): failed to find a solution");
        Object arg = _inc;
        if (_c2amaker != null)
          arg = _c2amaker.getArg(_inc, _params);
        return new PairObjDouble(arg, _incValue);
      }
    }
    finally {
      synchronized (this) {
        _f = null;
      }
    }
  }


  /**
   * reset values.
   */
  void reset() {
    _inc = null;
    _incValue = Double.MAX_VALUE;
  }


  synchronized Chromosome2ArgMakerIntf getC2AMaker() { return _c2amaker; }


  synchronized FunctionIntf getFunction() { return _f; }  // keep FindBugs happy


  synchronized int getId() { return _id; }


  synchronized static int incrID() {
    return ++_nextId;
  }

}

