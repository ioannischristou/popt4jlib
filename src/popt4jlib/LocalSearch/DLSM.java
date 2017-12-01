package popt4jlib.LocalSearch;

import utils.*;
import popt4jlib.*;
import parallel.*;
import java.util.*;


/**
 * class implements a parallel LocalSearch method for combinatorial optimization
 * problems, obeying the <CODE>popt4jlib.LocalOptimizerIntf</CODE> contract. The 
 * class uses multi-threading to evaluate in parallel the available moves from 
 * any point, and is itself thread-safe. The local search, is essentially a 
 * best-first search with backtracking: the moves-maker object is responsible
 * for generating all possible new positions from a given position; these 
 * positions are then evaluated, and all those that are better than the current
 * position, are stored as possible points to move from. These moves continue
 * until the search cannot find a better solution. The best found is returned.
 * The search may also be cut-short if the maximum number of allowed moves is 
 * reached.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DLSM implements LocalOptimizerIntf {
	// private static final long serialVersionUID=...
  private static int _nextId=0;
  private int _id;
  private Params _params=null;
  private boolean _setParamsFromSameThreadOnly=false;
  transient private Thread _originatingThread=null;  // threads don't serialize
  private Chromosome2ArgMakerIntf _c2amaker=null;
  private Arg2ChromosomeMakerIntf _a2cmaker=null;
  private AllChromosomeMakerIntf _allmovesmaker=null;
  double _incValue=Double.MAX_VALUE;
  Object _inc=null;  // incumbent chromosome
  FunctionIntf _f=null;

	private ParallelBatchTaskExecutor _executor = null;  // executor to use when
	                                                     // 1-int-arg ctor is used

  /**
   * public constructor of a DLSM OptimizerIntf. Assigns unique id among all 
	 * DLSM objects (in the same JVM).
   */
  public DLSM() {
    _id = incrID();
  }
	
	
	/**
	 * public 1-arg constructor allows the one-time creation of an executor 
	 * object, so that thread creation/destruction doesn't occur every time the  
	 * method is called from the same object.
	 * @param num_threads int the number of threads this optimizer will use in its
	 * unique executor.
	 */
	public DLSM(int num_threads) {
		this();
		try {
			_executor = 
				ParallelBatchTaskExecutor.newParallelBatchTaskExecutor(num_threads);
		}
		catch (ParallelException e) {
			throw new IllegalArgumentException("DLSM object failed creation");
		}
	}


  /**
   * public constructor of a DLSM OptimizerIntf object. Assigns unique id among
   * all DLS objects, and sets the appropriate parameters for the optimization
   * process via the setParams(params) process. The parameters are discussed in
   * the javadoc for the <CODE>minimize(f)</CODE> method.
   * @param params HashMap
   */
  public DLSM(HashMap params) {
    this();
    try {
      setParams(params);
    }
    catch (Exception e) {
      // no-op: cannot reach this point
    }
  }


  /**
   * constructor of a DLSM object, that assigns a unique id plus the parameters
   * passed into the argument. Also, it prevents other threads from modifying
   * the parameters passed into this object if the second argument is true.
   * @param params HashMap
   * @param setParamsOnlyFromSameThread boolean
   */
  public DLSM(HashMap params, boolean setParamsOnlyFromSameThread) {
    this();
    try {
      setParams(params);
    }
    catch (Exception e) {
      // no-op: cannot reach this point
    }
    _setParamsFromSameThreadOnly = setParamsOnlyFromSameThread;
    if (setParamsOnlyFromSameThread) _originatingThread=Thread.currentThread();
  }


  /**
   * return a copy of the parameters. Modifications to the returned object
   * do not affect the original data member.
   * @return HashMap
   */
  public synchronized HashMap getParams() {
    return new HashMap(_params.getParamsMap());
  }


  /**
   * the optimization params are set to p.
   * @param p HashMap
   * @throws OptimizerException if another thread is concurrently running the
   * <CODE>minimize(f)</CODE> method of this object. Note that unless the 2-arg
   * constructor DLSM(params, use_from_same_thread_only=true) is used to create
   * this object, it is perfectly possible for one thread to call 
	 * <CODE>setParams(p)</CODE>, then another to <CODE>setParams(p2)</CODE> to 
	 * some other param-set, and then the first thread to call 
	 * <CODE>minimize(f)</CODE>.
   */
  public synchronized void setParams(HashMap p) throws OptimizerException {
    if (_f!=null) 
			throw new OptimizerException("cannot modify parameters while running");
    if (_setParamsFromSameThreadOnly) {
      if (Thread.currentThread()!=_originatingThread)
        throw new OptimizerException("Current Thread is not allowed to call "+
					                           "setParams() on this DLS.");
    }
    _params = new Params(p);  // own the params
    try {
      _c2amaker = (Chromosome2ArgMakerIntf) _params.getObject("dls.c2amaker");
    }
    catch (ClassCastException e) {
      throw new OptimizerException("dls.c2amaker must be a "+
				                           "Chromosome2ArgMakerIntf object");
    }
    try {
      _a2cmaker = (Arg2ChromosomeMakerIntf) _params.getObject("dls.a2cmaker");
    }
    catch (ClassCastException e) {
      throw new OptimizerException("dls.a2cmaker must be a "+
				                           "Arg2ChromosomeMakerIntf object");
    }
    try {
      _allmovesmaker = 
				(AllChromosomeMakerIntf) _params.getObject("dls.movesmaker");
    }
    catch (ClassCastException e) {
      throw new OptimizerException("dls.movesmaker must be a "+
				                           "AllChromosomeMakerIntf object");
    }
  }

	
  /**
   * return a new empty <CODE>DLS</CODE> optimizer object (that must be
   * configured via a call to setParams(p) before it is used).
   * @return LocalOptimizerIntf
   */
  public LocalOptimizerIntf newInstance() {
    return new DLS();
  }


  /**
   * The most important method of the class is essentially a best-first search
	 * with backtracking method.
   * Prior to calling this method, some parameters must have been set, either
   * during construction of the object, or via a call to setParams(p).
   * The parameters are as follows:
   * <ul>
   * <li> &lt;"dls.x0", Object arg&gt; mandatory, the initial point from which 
	 * to start the local search.
   * <li> &lt;"dls.movesmaker", AllChromosomeMakerIntf movesmaker&gt; mandatory 
	 * the object responsible for implementing the interface that allows creating 
	 * ALL chromosome Objects from an existing one (produces -by definition- the
   * entire neighborhood of the object).
   * <li> &lt;"dls.maxiters", Integer niters&gt; optional, the max number of 
	 * iterations the process will go through, default is 
	 * <CODE>Integer.MAX_VALUE</CODE>.
   * <li> &lt;"dls.numthreads", Integer nt&gt; optional, the number of threads 
	 * in the threadpool to be used for exploring each possible move in the 
	 * neighborhood. Default is 1. Only used if this object was not constructed
	 * via the 1-int-arg constructor.
	 * <li> &lt;dls.maxsize", Integer num&gt; optional, the size of the queue
	 * holding up positions to move from. Default is 10.
   * <li> &lt;"dls.a2cmaker", Arg2ChromosomeMakerIntf a2cmaker&gt; optional, an 
	 * object implementing the Arg2ChromosomeMakerIntf that transforms objects 
	 * that can be passed directly to the FunctionIntf being minimized to 
	 * Chromosome objects that can be used in the local-search process -and 
	 * manipulated by Object implementing the AllChromosomeMakerIntf interface. 
	 * Default is null, which results in the arg objects being passed "as-is" to 
	 * the AllChromosomeMakerIntf object.
   * <li> &lt;"dls.c2amaker", Chromosome2ArgMakerIntf c2amaker&gt; optional, an 
	 * object implementing the Chromosome2ArgMakerIntf that transforms chromosome 
	 * Objects used in the local-search process -and manipulated by the Object 
	 * implementing the AllChromosomeMakerIntf interface- into argument Objects 
	 * that can be passed into the FunctionIntf object that the process minimizes. 
	 * Default is null, which results in the chromosome objects being passed 
	 * "as-is" to the FunctionIntf object being minimized.
   * </ul>
	 * Notice that for all keys above if the exact key of the form "dls.X" is not 
	 * found in the params, then the params will be searched for key "X" alone, so
	 * that if key "X" exists in the params, it will be assumed that its value
	 * corresponds to the value of "dls.X" originally sought.
   * <p>The result is a PairObjDouble object that contains the best function arg
   * along with the minimum function value obtained by this argument (or null
   * if the process fails to find any valid function argument).</p>
   * @param f FunctionIntf the function to optimize locally
   * @throws OptimizerException if another thread is concurrently running the
   * same method of this object or if the argument function object is null or if
	 * the optimization process fails
   * @return PairObjDouble an object that holds both the best value found by the
   * DLS process run as well as the argmin -the argument that produced this best
   * value.
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("DLSM.minimize(f): null f");
    HashMap p = null;
    try {
      int nt = 1;
      int max_iters = Integer.MAX_VALUE;
      synchronized (this) {
        if (_f != null)
					throw new OptimizerException("DLSM.minimize(): another thread is "+
						                           "concurrently executing the method on "+
					                             "the same object");
        _f = f;
        reset();
        Integer ntI = (Integer) _params.getInteger("dls.numthreads");
        if (ntI != null) nt = ntI.intValue();
        if (nt < 1)throw new OptimizerException(
            "DLSM.minimize(): invalid number of threads specified");
      }
			int maxsize = 10;
			Integer mxszI = (Integer) _params.getInteger("dls.maxsize");
			if (mxszI!=null) maxsize = mxszI.intValue();
      Integer niI = (Integer) _params.getInteger("dls.maxiters");
      if (niI != null) max_iters = niI.intValue();
      Object x0 = _params.getObject("dls.x0");
      if (x0==null) {
        throw new OptimizerException("no initial point (with key 'dls.x0') "+
					                           "specified in params");
      }
      // do the work
			BoundedMinHeapUnsynchronized buffer = 
				new BoundedMinHeapUnsynchronized(maxsize);
			BoundedMinHeapUnsynchronized max_buffer = 
				new BoundedMinHeapUnsynchronized(maxsize);  // max-heap buffer!
			// buffer contains argument objects for the f.eval() function
      try {
        ParallelBatchTaskExecutor executor = null;
        try {
          if (_executor==null)
						executor = ParallelBatchTaskExecutor.
										   newParallelBatchTaskExecutor(Math.max(nt,1));
					else executor = _executor;
        }
        catch (ParallelException e) {
          // no-op: never happens
        }
				Messenger mger = Messenger.getInstance();
				p = getParams();
				int cnt = 0;				
				try {
					_incValue = _f.eval(x0, p);
					if (_a2cmaker!=null) _inc = _a2cmaker.getChromosome(x0, p);
					else _inc = x0;
				}
				catch (IllegalArgumentException e) {
					e.printStackTrace();
				}				
				buffer.addElement(new Pair(x0, new Double(_incValue)));
				max_buffer.addElement(new Pair(x0, new Double(-_incValue)));
				double cur_best=Double.MAX_VALUE;
				while (buffer.size()>0 && cnt++ < max_iters) {
					//Object x = buffer.remove();
					Pair xv = (Pair) buffer.remove();
					// remove from max_buffer as well
					Pair obj2rm = new Pair(xv.getFirst(), 
						               new Double(-((Double)xv.getSecond()).doubleValue()));
					max_buffer.decreaseKey(obj2rm, new Pair(xv.getFirst(),
						                             new Double(Double.NEGATIVE_INFINITY)));
					max_buffer.remove();
					// done removing from max_buffer
					Object x = xv.getFirst();
					cur_best = ((Double)xv.getSecond()).doubleValue();
					if (_a2cmaker!=null) x = _a2cmaker.getChromosome(x, p);
					mger.msg("DLSM.minimize(): executing iteration "+cnt,1);
					// createAllChromosomes(x,p) can be, and usually is, the hardest 
					// computing task therefore, should be parallelized whenever possible
					Vector newchromosomes = _allmovesmaker.createAllChromosomes(x, p);
					if (newchromosomes==null || newchromosomes.size()==0) break;
					Vector newtasks = new Vector();
					for (int j=0; j<newchromosomes.size(); j++) {
						Object arg=null;
						if (_c2amaker == null || _c2amaker instanceof IdentityC2ArgMaker)
							arg = newchromosomes.elementAt(j);
						else arg = _c2amaker.getArg(newchromosomes.get(j),p);
						FunctionEvaluationTask ftj = 
							new FunctionEvaluationTask(getFunction(),arg,p);
						newtasks.add(ftj);
					}
					executor.executeBatch(newtasks);  // blocking
					for (int j=0; j<newtasks.size(); j++) {
						FunctionEvaluationTask ftj = 
							(FunctionEvaluationTask) newtasks.elementAt(j);
						if (ftj.isDone()==false) {  // this task failed to evaluate
							mger.msg("DLSM.minimize(): Task "+ftj+" failed to evaluate???",
											 2);
							continue;
						}  
						double ftjval = ftj.getObjValue();
						if (ftjval<cur_best) {
							x = _c2amaker==null || _c2amaker instanceof IdentityC2ArgMaker ?
									newchromosomes.get(j) : 
								  _c2amaker.getArg(newchromosomes.get(j), p);							
							try {
								buffer.addElement(new Pair(x, new Double(ftjval)));
								max_buffer.addElement(new Pair(x, new Double(-ftjval)));
							}
							catch (IllegalArgumentException e) {
								// mger.msg("soln found already exists in heap", 2);
								// continue
							}
							catch (IndexOutOfBoundsException e) {
								//if (ftjval<_incValue) {
									// make space for the new guy
									Pair max_el = (Pair) max_buffer.remove();
									// if it's worth entering the heap
									double mx_v = ((Double) max_el.getSecond()).doubleValue();
									if (ftjval < -mx_v) {
										buffer.decreaseKey(new Pair(max_el.getFirst(),
											new Double(-((Double)max_el.getSecond()).doubleValue())),
																			 new Pair(max_el.getFirst(), 
																				new Double(Double.NEGATIVE_INFINITY)));
										buffer.remove();
										buffer.addElement(new Pair(x, new Double(ftjval)));
										max_buffer.addElement(new Pair(x, new Double(-ftjval)));
									} else {
										max_buffer.addElement(max_el);  // re-insert
									}
								//} else {
								//	mger.msg("DLSM.minimize(f): no space in buffer to add "+
								//					 "improving solution", 2);							
								//}
							}
							if (ftjval<_incValue) {
								mger.msg("DLSM.minimize(f): found better solution="+ftjval,0);
								_incValue = ftjval;
								_inc = newchromosomes.elementAt(j);
							}
							//x = _c2amaker==null || _c2amaker instanceof IdentityC2ArgMaker ?
							//		_inc : _c2amaker.getArg(_inc, p);
						}  // if ftjval best
					}  // for j in newtasks
				}  // while buffer!=empty
				
        // shutdown the executor if it was created within this method call
        if (_executor==null) executor.shutDown();
        mger.msg("LocalSearch: completed after "+cnt+" iterations.",1);
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new OptimizerException("DLSM.minimize(f): failed");
      }

      synchronized (this) {
        if (_inc==null)
          throw new OptimizerException("DLSM.minimize(f): "+
						                           "failed to find solution");
        Object arg = _inc;
        if (_c2amaker != null) arg = _c2amaker.getArg(_inc, p);
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
	 * shuts down the <CODE>_executor</CODE> data member, if it's not null.
	 * @throws OptimizerException 
	 */
	public synchronized void shutDownExecutor() throws OptimizerException {
		if (_f!=null) 
			throw new OptimizerException("minimize() is currently running.");
		if (_executor!=null) {
			try {
				_executor.shutDown();
			}
			catch (ParallelException e) {
				e.printStackTrace();
				throw new OptimizerException("_executor.shutDown() failed;"+
					                           " probably due to another thread having "+
					                           "terminated it already");
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

