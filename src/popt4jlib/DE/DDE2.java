package popt4jlib.DE;

import java.util.*;
import parallel.*;
import parallel.distributed.*;
import utils.*;
import popt4jlib.*;
import popt4jlib.GradientDescent.VecUtil;

/**
 * A parallel/distributed implementation of the Differential Evolution 
 * algorithm. Almost the same as <CODE>DDE</CODE> class, but much faster 
 * when running with tens of threads on many-core CPUs, because of less 
 * synchronization required on the _solXXX data members. On the other hand,
 * this approach has the trade-off that the _solXXX elements are only updated
 * for threads to see at the end of each generation. While threads execute a 
 * generation, they don't see each other's updates. In fact, if non-determinism
 * is OK (via the appropriate flag set in the parameters), then the threads may
 * never see each other's updates (with the exception of the incumbent).
 * Notice: This class is the only implementation among the optimization meta-
 * heuristics in this library where if the "dde.nondeterminismok" flag is false,
 * it guarantees the same answer for the same random seed regardless of the 
 * number of threads used in the process, as long as all other parameters remain
 * the same for all runs. In such a case, the run-times of the program improve 
 * only when there is significant work to be done for each thread in each 
 * generation; for example, when minimizing the Rosenbrock function in 100 
 * dimensions, when the population size is 5000, running with 8 threads on 
 * a Lenovo ThinkPad T530 is about 2.5 times faster than running on 1 thread.
 * <p>Notes:
 * <ul>
 * <li>2021-05-08: ensure function evaluations can only throw 
 * <CODE>IllegalArgumentException</CODE> exceptions.
 * <li>2020-04-25: method seParams() became public because it was moved up from
 * LocalOptimizerIntf to the root OptimizerIntf interface class.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DDE2 implements OptimizerIntf {
  private static int _nextId = 0;
  private int _id;
  private HashMap _params;
  private boolean _setParamsFromSameThreadOnly=false;
  private Thread _originatingThread=null;
  private double _incValue=Double.MAX_VALUE;
	private int _incIndex;  // index to the current generation's best individual
  private VectorIntf _inc=null;  // incumbent vector
  private DDE2Thread[] _threads=null;
  FunctionIntf _f=null;
  VectorIntf[] _sols;  // the population of solutions
  double[] _solVals;
  int _maxthreadwork;
	// array needed when repeatable results must occur even when running on 
	// different number of threads
	Random[] _rands = null;
	// data related to inter-process communication (individuals' migration)
	String _dmpCoordinator=null;  // the coordinator location for distributed msg 
	                              // passing if any exists
	int _dmpPort=-1;  // the coordinator port for distributed msg passing if any 
	                  // exists
	int _thisProcessId=-1;  // the id of this process used in migration things if 
	                        // any exists
	int _nextProcessId=-1;  // the id of the process to whom this process should 
	                        // be sending "migrants" if any exists
	int _numGensBetweenMigrations = 10;
	int _numMigrants = 10;
	String _reducerHost=null;  // the location of the reduce-srv if any exists
	int _reducerPort=-1;  // the port of the reduce-srv if any exists
	

  /**
   * default constructor. Assigns to the object a unique id.
   */
  public DDE2() {
    _id = incrID();
  }


  /**
   * Constructor of a DDE2 object, that assigns a unique id plus the parameters
   * passed into the argument.
   * @param params HashMap
   */
  public DDE2(HashMap params) {
    this();
    try {
      setParams(params);
    }
    catch (Exception e) {
      // no-op: cannot reach this point
    }
  }


  /**
   * Constructor of a DDE2 object, that assigns a unique id plus the parameters
   * passed into the argument. Also, it prevents other threads from modifying
   * the parameters passed into this object if the second argument is true.
   * @param params HashMap
   * @param setParamsOnlyFromSameThread boolean
   */
  public DDE2(HashMap params, boolean setParamsOnlyFromSameThread) {
    this();
    try {
      setParams(params);
    }
    catch (Exception e) {
      // no-op: cannot reach this point
    }
    _setParamsFromSameThreadOnly = setParamsOnlyFromSameThread;
    if (setParamsOnlyFromSameThread) 
			_originatingThread = Thread.currentThread();
  }


  /**
   * return a copy of the parameters. Modifications to the returned object
   * do not affect the data member.
   * @return HashMap
   */
  synchronized HashMap getParams() {  
    // modifications of the returned object do not matter
    return new HashMap(_params);
  }


  /**
   * the optimization params are set to a copy of p.
   * @param p HashMap
   * @throws OptimizerException if another thread is concurrently running the
   * <CODE>minimize(f)</CODE> of this object. Notice that unless the 2-arg
   * constructor DDE2(params, use_from_same_thread_only=true) is used to create
   * this object, it is perfectly possible for one thread to call setParams(p),
   * then another to setParams(p2) to some other param-set, and then the
   * first thread to call minimize(f).
   */
  public synchronized void setParams(HashMap p) throws OptimizerException {
    if (_f!=null) 
			throw new OptimizerException("cannot modify parameters while running");
    if (_setParamsFromSameThreadOnly) {
      if (Thread.currentThread()!=_originatingThread)
        throw new OptimizerException("Current Thread is not allowed to call "+
					                           "setParams() on this DDE.");
    }
    _params = null;
    _params = new HashMap(p);  // own the params
  }


  /**
   * the most important method of the class, that implements a Distributed
   * Differential Evolution process. A number of threads work on different
   * solutions in the solution array holding each iteration's population.
   * For a given number of "tries", the process should run faster when given
   * more threads assuming there are as many cores in the CPU running the
   * process.
   * Prior to calling this method, a few parameters must have been passed in
   * either during object construction, or afterwards, by a call to setParams(p)
   * These parameters are:
	 * <ul>
   * <li> &lt;"dde.numdimensions", Integer nd&gt; mandatory, the dimension of 
	 * the domain of the function to be minimized.
   * <li> &lt;"dde.numtries", Integer ni&gt; optional, the total number of 
	 * "tries", default is 100.
   * <li> &lt;"dde.numthreads", Integer nt&gt; optional, the number of threads 
	 * to use, default is 1.
   * <li> &lt;"dde.popsize", Integer ps&gt; optional, the total population size 
	 * in each iteration, default is 10.
   * <li> &lt;"dde.w", Double w&gt; optional, the "weight" of the DE process, a 
	 * double number in [0,2], default is 1.0
   * <li> &lt;"dde.px", Double px&gt; optional, the "crossover rate" of the DE 
	 * process, a double number in [0,1], default is 0.9
   * <li> &lt;"dde.minargval", Double val&gt; optional, a double number that is 
	 * a lower bound for all variables of the optimization process, i.e. all 
	 * variables must satisfy x_i &ge; val.doubleValue(), default is -infinity
   * <li> &lt;"dde.maxargval", Double val&gt; optional, a double number that is 
	 * an upper bound for all variables of the optimization process, i.e. all 
	 * variables must satisfy x_i &le; val.doubleValue(), default is +infinity
   * <li> &lt;"dde.minargval$i$", Double val&gt; optional, a double number that 
	 * is a lower bound for the i-th variable of the optimization process, i.e. 
	 * variable must satisfy x_i &ge; val.doubleValue(), default is -infinity
   * <li> &lt;"dde.maxargval$i$", Double val&gt; optional, a double number that 
	 * is an upper bound for the i-th variable of the optimization process, i.e. 
	 * variable must satisfy x_i &le; val.doubleValue(), default is +infinity
	 * <li> &lt;"dde.de/best/1/binstrategy", Boolean val&gt; optional, a boolean 
	 * value that if present and true, indicates that the DE/best/1/bin strategy 
	 * should be used in evolving the population instead of the DE/rand/1/bin 
	 * strategy, default is false
	 * <li> &lt;"dde.nondeterminismok", Boolean val&gt; optional, a boolean value 
	 * indicating whether the method should return always the same value given 
	 * the same parameters and same random seed(s), regardless of the number of 
	 * threads used! The method can be made to run
	 * much faster in a multi-core setting if this flag is set to true (at the 
	 * expense of deterministic results) getting the CPU utilization to reach 
	 * almost 100% as opposed to around 60% otherwise, default is false
	 * <li> &lt;"dde2.numgensbetweenbarrier", Integer val&gt; optional, an integer
	 * specifying the number of generations between two successive barrier calls
	 * among the threads participating in the DDE2 process, default is 
	 * <CODE>Integer.MAX_VALUE</CODE>; also notice that this parameter is 
	 * meaningless when the "dde.nondeterminismok" flag is false.
	 * <li> &lt;"dde.dmpaddress", String location&gt; optional, if existing, 
	 * specifies the location of a distributed Msg-Passing server that implements
	 * the basic send/recv operations as specified in 
	 * <CODE>
	 * parallel.distributed.DActiveMsgPassingCoordinatorLongLivedConnSrv[Clt]
	 * </CODE>
	 * default is null
	 * <li> &lt;"dde.dmpport", Integer port&gt; optional, if existing, 
	 * specifies the port number of a distributed Msg-Passing server implementing
	 * the basic send/recv operations as specified in 
	 * <CODE>
	 * parallel.distributed.DActiveMsgPassingCoordinatorLongLivedConnSrv[Clt]
	 * </CODE>
	 * default is null
	 * <li> &lt;"dde.dmpthisprocessid", Integer myid&gt; optional, if existing, it 
	 * indicates the id of this process (this is the number to use in a 
	 * <CODE>recvData(myid)</CODE> call on the 
	 * <CODE>
	 * parallel.distributed.DActiveMsgPassingCoordinatorLongLivedConnClt
	 * </CODE>
	 * object), default is null
	 * <li> &lt;"dde.dmpnextprocessid", Integer id&gt; optional, if existing, it
	 * indicates the id of the process to which this process should be 
	 * sending "migrants" to; (this is the number to use as the "send address" in 
	 * a sendData(myid, id, data) DActiveblahblah call); default is null
	 * <li> &lt;"dde.numgensbetweenmigrations", Integer num&gt; optional, if 
	 * existing, it indicates the number of generations that must pass between two
	 * successive "migrations" between DDE island-processes; default is 10
	 * <li> &lt;"dde.nummigrants",Integer num&gt; optional, if it exists indicates
	 * how many migrants will be sent and received from each dde process; this 
	 * number must be less than or equal to the ratio dde.popsize/dde.numthreads.
	 * If the constraint does not hold for any of the processes participating in 
	 * the entire distributed DE process, the process may fail; default is 10
	 * <li> &lt;"dde.reducerhost", String host&gt; optional, if existing, it 
	 * indicates the address in which the reducer server resides; default is null
	 * <li> &lt;"dde.reducerport", Integer port&gt; optional, if existing, it
	 * indicates the address in which the reducer server process listens at; 
	 * default is -1
   * </ul>
	 * <p> Notice that in case of running DDE in a distributed manner, there are
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
   * @param f FunctionIntf the function to be minimized
   * @throws OptimizerException if another thread is concurrently running the
   * <CODE>minimize(f)</CODE> method of this object or if the optimization
   * process fails
   * @return PairObjDouble an object containing both the best value found by
   * the DE optimization process, as well as the best argument that produced it.
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("DDE.minimize(f): null f");
		DReducer red = null;
    try {
      synchronized (this) {
        if (_f != null)throw new OptimizerException("DDE.minimize(): "+
          "another thread is concurrently executing the method on this object");
        _f = new FunctionExceptionsWrapper(f);  // ensure _f.eval() only throws
				                                        // IllegalArgumentException
        reset();
      }
      int numthreads = 1;
      try {
        Integer ntI = (Integer) _params.get("dde.numthreads");
        if (ntI != null && ntI.intValue() > 1) numthreads = ntI.intValue();
      }
      catch (Exception e) {
        e.printStackTrace();  // no-op
      }
      int popsize = 10;
      try {
        Integer psI = (Integer) _params.get("dde.popsize");
        if (psI != null && psI.intValue() > 1) popsize = psI.intValue();
      }
      catch (Exception e) {
        e.printStackTrace();  // no-op
      }
      _sols = new VectorIntf[popsize]; // create array of solutions
      _solVals = new double[popsize];  // no need for these ctors to be synched
                                       // as the worker threads that will be
                                       // accessing it are created below and
                                       // thus the rules of synchronization are
                                       // obeyed (FindBugs complains unjustly)
                                       // same comment applies to the unsynched
                                       // use of _params, _threads field
			Boolean ndok = (Boolean) _params.get("dde.nondeterminismok");
			if (ndok==null || ndok.booleanValue()==false) {  // non-determinism NOT OK
				_rands = new Random[popsize];
				long seed = RndUtil.getInstance().getSeed();
				for (int i=0; i<popsize; i++) {
					_rands[i] = new Random(seed+i);
				}
			}
			_dmpCoordinator = (String) _params.get("dde.dmpaddress");
			_dmpPort = _params.containsKey("dde.dmpport") ? 
							     ((Integer) _params.get("dde.dmpport")).intValue() : -1;
			_reducerHost = (String) _params.get("dde.reducerhost");
			_reducerPort = _params.containsKey("dde.reducerport") ? 
							     ((Integer) _params.get("dde.reducerport")).intValue() : -1;
			try {
				if (_reducerPort>0)
					red = new DReducer(_reducerHost, _reducerPort, 
						                 "DDEReducer_"+_reducerHost+"_"+_reducerPort);
				// if running distributed, all will have to complete the above call
				// before being able to proceed with the migration of individuals
				// in the main DDE process and therefore when reducing, the server will
				// know exactly which ones participate in this reduction operation
			}
			catch (Exception e) {
				e.printStackTrace();
			}
      try {
        Barrier.setNumThreads("dde." + getId(), numthreads); // init. barrier
      }
      catch (ParallelException e) {
        e.printStackTrace();
        throw new OptimizerException("barrier init. failed");
      }

      _threads = new DDE2Thread[numthreads];
      if (ndok!=null && ndok.booleanValue()==true) 
				RndUtil.addExtraInstances(numthreads);  // not needed
      int ntries = 100;
      try {
        Integer ntriesI = (Integer) _params.get("dde.numtries");
        if (ntriesI != null && ntriesI.intValue() >= 1)
          ntries = ntriesI.intValue();
      }
      catch (Exception e) {
        e.printStackTrace();  // no-op
      }
      int vecsperthread = popsize / numthreads;
      int k = 0;
      int l = vecsperthread;
      for (int i = 0; i < numthreads - 1; i++) {
        _threads[i] = new DDE2Thread(i, k, l - 1, ntries, this);
        k = l;
        l += vecsperthread;
      }
      _threads[numthreads-1] = 
			    new DDE2Thread(numthreads - 1, k, popsize - 1, ntries, this);
      _maxthreadwork = popsize - k;
      for (int i = 0; i < numthreads; i++) {
        _threads[i].start();
      }
      // wait until all threads finish
      for (int i = 0; i < numthreads; i++) {
        try {
          _threads[i].join();
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt(); // recommended behavior
        }
      }

      synchronized (this) {
        if (_inc == null) // didn't find a solution
          throw new OptimizerException("failed to find solution");
        // ok, we're done
        PairObjDouble pr = new PairObjDouble(_inc.newInstance(), _incValue);
        return pr;
      }
    }
    finally {
      try {
				// release resources
        Barrier.removeInstance("dde."+getId());
				if (_threads!=null) {
					for (int i=0; i<_threads.length; i++) {
						_threads[i] = null;  // release thread resources
					}
					_threads = null;
				}
				// if running distributed, find the min of all DDE processes to report
				if (red!=null) {
					Double val = (Double) red.reduce(new Double(_incValue), 
						                               ReduceOperatorMinDbl.getInstance());
					Messenger.getInstance().msg("Best Value Found Overall Processes="+
						                          val.doubleValue(), 0);
					// clean-up with reduce-server
					red.removeCurrentThread();
					// done.
				}
      }
      catch (Exception e) {  // cannot get here
       e.printStackTrace();
       throw new OptimizerException("DDE.minimize(f): couldn't reset barrier "+
                                    "at end of optimization");
      }
      synchronized (this) {  // communicate changes to other threads
        _f = null;
      }
    }
  }


  synchronized void setIncumbent(VectorIntf arg, double val, int index) 
		throws OptimizerException {
    if (val<_incValue) {
      _incValue=val;
      _inc=arg;
			Messenger mger = Messenger.getInstance();
			_incIndex = index;
      if (Debug.debug(Constants.DDE)!=0) {
        // sanity check
				double incval = Double.MAX_VALUE;
				try {
					incval = _f.eval(arg, _params);
				}
				catch (Exception e) {
					throw new OptimizerException("DDE2.setIncumbent(): _f.eval() threw "+
						                           e.toString());
				}
        if (Math.abs(incval - _incValue) > 1.e-25) {
          mger.msg("DDE2.setIncumbent(): arg-val originally="+
                   _incValue + " fval=" + incval + " ???",0);
          throw new OptimizerException(
              "DDE2.setIncumbent(): insanity detected; " +
              "most likely evaluation function is " +
              "NOT reentrant... " +
              "Add the 'function.notreentrant,num'" +
              " pair (num=1 or 2) to run parameters");
        }
        // end sanity check
      }
      mger.msg("setIncumbent(): best sol value="+val,0);
    }
  }


  /**
   * this method will return an invalid incumbent if another thread has already
   * started running the minimize(f) method of this object.
   * @return VectorIntf
   */
  synchronized VectorIntf getIncumbent() {
    return _inc;
  }
	
	
	/**
   * this method will return an invalid incumbent index if another thread has 
	 * already started running the minimize(f) method of this object.
	 * @return int 
	 */
	synchronized int getIncIndex() {
		return _incIndex;
	}

	
  synchronized FunctionIntf getFunction() { return _f; }  // keep FindBugs happy

	
  int getId() { return _id; }


  void reset() {
    _inc = null;
    _incValue = Double.MAX_VALUE;
		_incIndex = -1;
    _sols = null;
    _solVals = null;
    // _maxthreadwork = 0;  // not needed
		_rands = null;
  }


  private synchronized static int incrID() {
    return ++_nextId;
  }

}


/**
 * auxiliary class not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DDE2Thread extends Thread {

  private final DDE2 _master;
  private int _numtries;
	private int _numthreads;
  private int _id;
  private int _uid;
  private int _from;
  private int _to;
  private double _px=0.9;
  private double _w=1.0;
  private HashMap _fp;
	private boolean _nonDeterminismOK=false;
	private boolean _doDEBestStrategy=false;
	private int _bestInd=-1;
	// data related to inter-process communication (individuals' migration)
	private DActiveMsgPassingCoordinatorLongLivedConnClt _dmsgpassClient=null;
	// caches
	private boolean _cacheOn=false;
	private List _minargvali;
	private List _maxargvali;
	// temp storage area for copySols and copyVals arrays, needed only when
	// the dde.nondeterminismOK is false (which is the default also)
	VectorIntf[] _tmpSols;
	double[] _tmpVals;
	BoolVector _tmpInds;
	

	/**
	 * sole public constructor.
	 * @param id int
	 * @param from int
	 * @param to int
	 * @param numtries int
	 * @param master DDE2  // redundant if class were to become inner-class of DDE
	 */
  public DDE2Thread(int id, int from, int to, int numtries, DDE2 master) {
    _id = id;
    _uid = (int) DataMgr.getUniqueId();
    _master=master;
    _numtries=numtries;
    _from = from;
    _to = to;
		_numthreads = 1;
		try {
			HashMap p = _master.getParams();
			if (p.containsKey("dde.numthreads"))
				_numthreads = ((Integer) p.get("dde.numthreads")).intValue();
			Boolean ndok = (Boolean) p.get("dde.nondeterminismok");
			if (ndok!=null && ndok.booleanValue()==true) _nonDeterminismOK = true;
			Boolean debeststr = (Boolean) p.get("dde.de/best/1/binstrategy");
			if (debeststr!=null && debeststr.booleanValue()==true) {
				_doDEBestStrategy = true;
			}
			if (_id==0) {
				if (_master._dmpCoordinator!=null) {
					String coordname = "DDE.DMsgPassingCoord_"+_master._dmpPort;
					_dmsgpassClient = 
						new DActiveMsgPassingCoordinatorLongLivedConnClt(
							_master._dmpCoordinator, _master._dmpPort, coordname);
					Integer ngbmI = (Integer) p.get("dde.numgensbetweenmigrations");
					if (ngbmI!=null) _master._numGensBetweenMigrations = ngbmI.intValue();
					if (_master._numGensBetweenMigrations>=numtries) {  
            // force at least one migration to ensure reduce op correctness
						_master._numGensBetweenMigrations=numtries-1;
					}
					Integer nmI = (Integer) p.get("dde.nummigrants");
					if (nmI!=null) _master._numMigrants = nmI.intValue();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
  }

	
	/**
	 * the main method of the thread class. Initializes the part of the population
	 * that is this thread's responsibility, and runs for a number of generations 
	 * the DE algorithm, and handles migrations among populations living in 
	 * different DDE processes (in different JVMs).
	 */
  public void run() {
    HashMap p = _master.getParams();  // returns a copy
    p.put("thread.localid", new Integer(_id));
    p.put("thread.id", new Integer(_uid));  // used to be _id
    // create the _funcParams
    _fp = new HashMap();
    Iterator it = p.keySet().iterator();
    while (it.hasNext()) {
      String key = (String) it.next();
      Object val = p.get(key);
      if (val!=null) {
        Class valclass = val.getClass();
        Package pack = valclass.getPackage();
        if (pack!=null) {
          String packname = pack.getName();
          if (packname.startsWith("popt4jlib") ||
              packname.startsWith("parallel"))continue;  // don't include 
					                                               // such objects
        }
        else {
          Messenger.getInstance().msg("no package info "+
						                          "for object with key "+key, 2);
        }
        _fp.put(key, val);
      }
    }
    // end creating _funcParams
		int num_gens_between_barrier = Integer.MAX_VALUE;
		try {
      Double wD = (Double) p.get("dde.w");
      if (wD != null && wD.doubleValue() >= 0 && wD.doubleValue() <= 2)
        _w = wD.doubleValue();
      Double pxD = (Double) p.get("dde.px");
      if (pxD != null && pxD.doubleValue() >= 0 && wD.doubleValue() <= 1)
        _px = pxD.doubleValue();
			if (_id==0) {
				Integer tpId = (Integer) p.get("dde.dmpthisprocessid");
				if (tpId!=null) _master._thisProcessId = tpId.intValue();
				Integer npId = (Integer) p.get("dde.dmpnextprocessid");
				if (npId!=null) _master._nextProcessId = npId.intValue();
			}
			Integer ngbbI = (Integer) p.get("dde2.numgensbetweenbarrier");
			if (ngbbI!=null && ngbbI.intValue()>0) 
				num_gens_between_barrier = ngbbI.intValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();  // no-op
    }
    VectorIntf best = null;
    FunctionIntf f = _master.getFunction();  // was _master._f
		double[] copy_vals = new double[_master._solVals.length];
    VectorIntf[] copy_sols = new VectorIntf[_master._sols.length];  // vis. OK
		if (!_nonDeterminismOK) {
			_tmpSols = new VectorIntf[_to-_from+1];  // used to be copy_sols.length
			_tmpVals = new double[_to-_from+1];  // used to be copy_vals.length
			_tmpInds = new BoolVector(copy_vals.length);
		}
    // initialize the [from,to] part of the population
    //DblArray1VectorRndMaker rvmaker = new DblArray1VectorRndMaker(p);
    FastDblArray1VectorURndMaker rvmaker = new FastDblArray1VectorURndMaker(p);
		double cur_best = Double.MAX_VALUE;
		for (int i=_from; i<=_to; i++) {
      try {
				VectorIntf rv = null;
				if (_master._rands!=null)
					rv = rvmaker.createNewRandomVector(_master._rands[i]);
        else rv = rvmaker.createNewRandomVector();
				_master._sols[i] = rv;
				copy_sols[i] = rv;
				if (!_nonDeterminismOK) {
					_tmpSols[i-_from] = null;
					_tmpVals[i-_from] = Double.NaN;
					_tmpInds.unset(i-_from);
				}
				double ival = f.eval(rv, _fp);
        _master._solVals[i] = ival;
				copy_vals[i] = ival;
				if (ival < cur_best) {
					_bestInd = i;
					_master.setIncumbent(rv.newInstance(), ival, _bestInd);
					cur_best = ival;
				}
      }
      catch (Exception e) {
        e.printStackTrace();  // no-op
      }
    }
    double bestval = cur_best;
    // main computation
		Barrier b = Barrier.getInstance("dde."+_master.getId());
		for (int i=0; i<_numtries; i++) {
      try {
        if (i==0 || i % num_gens_between_barrier == 0 ||
						(_numthreads>1 && _master._dmpCoordinator!=null && 
					   _nonDeterminismOK)) 
					b.barrier();  
        // if _nonDeterminismOK==false, then the barriers in min(f,p) make the
				// above barrier redundant: when i==0 though, the barrier is needed for
				// population initialization correctness. Here, in DDE2, without 
				// migration, we don't need the barrier as the updates don't get seen
				// by the other threads very often any way
				// handle migration stuff now
				boolean is_migration_gen = i % _master._numGensBetweenMigrations == 0;
				if (_dmsgpassClient!=null && is_migration_gen) { 
					b.barrier();
					doMigration(i, copy_sols, copy_vals);
					b.barrier();
				}
				else if (_master._dmpCoordinator!=null && is_migration_gen) {
					b.barrier();
					// no-op
					b.barrier();
				}
				// done with migration handling
				{  // DDE2 is a different version of DDE: updates of the population 
				   // are not instantly seen by the threads
				   // 2nd update: as the barriers are too slow, use _master synch only
				   // which results in different threads seeing different versions of
				   // of the population, unless of course non-determinism is NOT ok.
				   // 3rd update: _master synch is not needed when non-determinism NOTOK
				   if (!_nonDeterminismOK && _numthreads>1) {
						 b.barrier();
						 for (int j=_from; j<=_to; j++) {  // write own updates
							 _master._sols[j] = copy_sols[j];
							 _master._solVals[j] = copy_vals[j];
						 }
					 } else {
						 synchronized (_master) {
							 if (_nonDeterminismOK || _numthreads==1) {
								 for (int j=0; j<_from; j++) {  // read others' updates
									 copy_sols[j] = _master._sols[j];
									 // risk no-copy, if nondeterminimsm is not OK, copy will be 
									 // enforced below
									 copy_vals[j] = _master._solVals[j];
								 }
							 }
							 for (int j=_from; j<=_to; j++) {  // write own updates
								 _master._sols[j] = copy_sols[j];
								 _master._solVals[j] = copy_vals[j];
							 }
							 if (_nonDeterminismOK || _numthreads==1) {
								 for (int j=_to+1; j<copy_sols.length; j++) {  // read others'
									 copy_sols[j] = _master._sols[j];
									 // risk no-copy, if nondeterminimsm is not OK, copy will be 
									 // enforced below
									 copy_vals[j] = _master._solVals[j];
								 }
							 }
						 }
					 }
					 if (!_nonDeterminismOK && _numthreads>1) {
						 b.barrier();
						 // read again the others' updates
						 // 3rd update: no _master synch needed
						 //synchronized (_master) {
						   for (int j=0; j<_from; j++) {
							   if (copy_sols[j]!=null && 
									   copy_sols[j] instanceof PoolableObjectIntf)
									 ((PoolableObjectIntf) copy_sols[j]).release();  // avoid leak
								 copy_sols[j] = _master._sols[j].newCopy();  // copy needed
								 copy_vals[j] = _master._solVals[j];
							 }
							 for (int j=_to+1; j<copy_sols.length; j++) {
							   if (copy_sols[j]!=null && 
									   copy_sols[j] instanceof PoolableObjectIntf)
									 ((PoolableObjectIntf) copy_sols[j]).release();  // avoid leak								 
								 copy_sols[j] = _master._sols[j].newCopy();  // copy needed
								 copy_vals[j] = _master._solVals[j];
							 }
						 //}
					 }
			  }
        PairObjDouble pair = min(f, p, copy_sols, copy_vals, b);
        if (pair==null) {
          continue;
        }
        double val = pair.getDouble();
        if (val<bestval) {
          bestval=val;
          best=(VectorIntf) pair.getArg();
          _master.setIncumbent(best, bestval, _bestInd);
        }
      }
      catch (Exception e) {
        e.printStackTrace();
        // no-op
      }
    }  // for i=0..._numtries-1 
  }


	/**
	 * implements the DE algorithm on the part of the population managed by this 
	 * thread.
	 * @param f FunctionIntf
	 * @param p HashMap
	 * @param copySols VectorIntf[]
	 * @param copyVals double[]
	 * @param b Barrier
	 * @return PairObjDouble  // Pair&lt;VectorIntf, Double&gt;
	 * @throws OptimizerException 
	 */
  private PairObjDouble min(FunctionIntf f, HashMap p, 
		                        VectorIntf[] copySols, double[] copyVals, Barrier b) 
		throws OptimizerException {
    final int popsize = _master._sols.length;  // no problem with unsync. access
                                               // as the _master._sols array
                                               // has been created before the
                                               // worker thread creation
                                               // (FindBugs unjustly complains)
    Random rnd = RndUtil.getInstance(_uid).getRandom();  // used to be _id
		int cur_inc_ind = -1;
		if (_doDEBestStrategy) {
			if (_nonDeterminismOK==false && _numthreads > 1) 
				b.barrier();  // ensure the previous generation's incumbent is used:
			                // without this barrier, it is possible that after the 
			                // barrier in the for-loop in run(), some other thread
			                // was quick enough to find a new incumbent and update
			                // the master with it; this barrier protects against 
			                // this possibility.
			cur_inc_ind = _master.getIncIndex();
		}
		double bestval = Double.MAX_VALUE;
		VectorIntf best = null;
    int count=0;
		List indices = new ArrayList(popsize);  // indices used to be Vector
    for (int i=_from; i<=_to; i++) {
      ++count;
      VectorIntf x = copySols[i];  // VectorIntf x = _master._sols[i];
      // select random vectors a,b,c from _sols
      indices.clear();
      for (int j=0; j<popsize; j++) {
        if (j!=i) indices.add(new Integer(j));
      }
			if (_master._rands!=null) rnd = _master._rands[i];
      Collections.shuffle(indices, rnd);
      boolean found=false;
      VectorIntf xia=null; VectorIntf xib=null; VectorIntf xic=null;
      if (_doDEBestStrategy==false) {  // select xia in random
				for (int k=0; k<indices.size(); k++) {
					int ia = ((Integer) indices.get(k)).intValue();
					xia = copySols[ia];  // xia = _master._sols[ia];
					if (VecUtil.equal(x,xia)==false) {
						found=true;
						indices.remove(k);
						break;
					}
				}
			} else {  // do DE/best/... strategy: xia is always the cur. best indiv.
				found = true;
				// xia = _master._sols[cur_inc_ind];  // _master.getSol(cur_inc_ind);
				xia = copySols[cur_inc_ind];
				indices.remove(new Integer(cur_inc_ind));
			}
      if (!found) 
				throw new OptimizerException("couldn't find a vector xa != x");
      found=false;
      for (int k=0; k<indices.size(); k++) {
        int ib = ((Integer) indices.get(k)).intValue();
        xib = copySols[ib];  // xib = _master._sols[ib];
        if (VecUtil.equal(x,xib)==false && VecUtil.equal(xia,xib)==false) {
          found=true;
          indices.remove(k);
          break;
        }
      }
      if (!found) 
				throw new OptimizerException("couldn't find a vector xb != x,xa");
      found=false;
      for (int k=0; k<indices.size(); k++) {
        int ic = ((Integer) indices.get(k)).intValue();
        xic = copySols[ic];  // xic = _master._sols[ic];
        if (VecUtil.equal(x,xic)==false && VecUtil.equal(xia,xic)==false && 
					  VecUtil.equal(xib,xic)==false) {
          found=true;
          indices.remove(k);
          break;
        }
      }
      if (!found) 
				throw new OptimizerException("couldn't find a vector xc != x,xa,xb");
      int n = x.getNumCoords();
      int r = rnd.nextInt(n);
      VectorIntf xtry = x.newCopy();
      for (int j=0; j<n; j++) {
        double rj = rnd.nextDouble();
        if (j==r || rj<_px) {
          double valj = bound(xia.getCoord(j) + 
						                  _w*(xib.getCoord(j)-xic.getCoord(j)), j, p);
          try {
            xtry.setCoord(j, valj);
          }
          catch (ParallelException e) {  // can never get here
            e.printStackTrace();
          }
        }
      }
      if (!_nonDeterminismOK && _numthreads>1) b.barrier();  // Barrier #1  
      // next call may throw IllegalArgumentException
      double ftry = Double.MAX_VALUE;
			try {
				ftry = f.eval(xtry, _fp);  // used to be p
			}
			catch (IllegalArgumentException e) {
				e.printStackTrace();  // ignore non-quietly
			}
			final double cur_val_i = copyVals[i];  // _master._solVals[i];
			
			// don't do the update to copy_XXX within the loop, as this will create
			// differences when running with different numbers of threads
      if (ftry < cur_val_i && _nonDeterminismOK) {  
        // update master-copy population
				if (copySols[i] instanceof PoolableObjectIntf) {
					// release previous population member
					((PoolableObjectIntf) copySols[i]).release();
				}
				copySols[i] = xtry.newCopy();
				// _master._solVals[i] = ftry;
				copyVals[i] = ftry;
      }
			else {
				// just copy the new best value to the temp-storage area, and do the
				// update of the copy_XXX arrays outside of the loop
				if (ftry < cur_val_i) {
					_tmpSols[i-_from] = xtry.newCopy();
					_tmpVals[i-_from] = ftry;
					_tmpInds.set(i);
				} else if (!_nonDeterminismOK) {
					_tmpSols[i-_from] = null;
					_tmpVals[i-_from] = Double.NaN;
					_tmpInds.unset(i);
				}
				// end copy to temp storage area
			}
      if (ftry < bestval) {
        best = xtry.newInstance();
        bestval = ftry;
				if (ftry < cur_val_i) _bestInd = i;  // otherwise, no update happened
      }
			if (xtry instanceof PoolableObjectIntf) {
				((PoolableObjectIntf) xtry).release();
			}
      if (!_nonDeterminismOK && _numthreads>1) b.barrier();  // Barrier #2
      // barrier had to come here too to ensure all changes are visible in next 
			// iteration of the for-loop to all threads. Notice that even without the 
			// barriers, threads still have memory visibility regarding the vectors in
			// the solution pool, but the order in which the updates in the pool occur
			// is undefined and therefore the results are non-deterministic.
    }  // end for i in [_from..._to]
		if (!_nonDeterminismOK) {
			for (int i=_tmpInds.nextSetBit(0); i>=0; i=_tmpInds.nextSetBit(i+1)) {
			//for (int i=_from; i<=_to; i++) {
				//if (_tmpSols[i-_from]!=null) {
	        // update master-copy population
					if (copySols[i] instanceof PoolableObjectIntf) {
						// release previous population member
						((PoolableObjectIntf) copySols[i]).release();
					}
					copySols[i] = _tmpSols[i-_from];
					copyVals[i] = _tmpVals[i-_from];
					// reset temp-storage area
					_tmpSols[i-_from] = null;
					_tmpVals[i-_from] = Double.NaN;
				//}
			}
			_tmpInds.clear();
			while (count<_master._maxthreadwork) {
				b.barrier();  // barrier for Barrier #1 above
				b.barrier();  // second barrier required for Barrier #2 above 
				++count;
			}
		}
    return new PairObjDouble(best, bestval);
  }


	/**
	 * projects the value val of the j-th variable according to any box 
	 * constraints described in the params passed in.
	 * @param val double
	 * @param j int
	 * @param params HashMap
	 * @return double
	 * @throws OptimizerException 
	 */
  private double bound(double val, int j, HashMap params) 
		throws OptimizerException {
    if (!_cacheOn) {
			_minargvali = new ArrayList();
			_maxargvali = new ArrayList();
			double mingval = Double.NEGATIVE_INFINITY;
			double maxgval = Double.MAX_VALUE;
			final int n = ((Integer) params.get("dde.numdimensions")).intValue();
			Double mingvD = (Double) params.get("dde.minargval");
			if (mingvD!=null) mingval = mingvD.doubleValue();
			Double maxgvD = (Double) params.get("dde.maxargval");
			if (maxgvD!=null) maxgval = maxgvD.doubleValue();
			if (maxgval < mingval)
				throw new OptimizerException("global minarg value>global maxarg value");
			for (int i=0; i<n; i++) {
				double minval = mingval;
				double maxval = maxgval;
				Double mvD = (Double) params.get("dde.minargval"+i);
				if (mvD!=null && mvD.doubleValue()>minval) minval = mvD.doubleValue();
				Double MvD = (Double) params.get("dde.maxargval"+i);
				if (MvD!=null && MvD.doubleValue()<maxval) maxval = MvD.doubleValue();
				if (minval>maxval)
					throw new OptimizerException("min arg value > global max arg value");
				_minargvali.add(new Double(minval));
				_maxargvali.add(new Double(maxval));
			}
			_cacheOn=true;
		}
    double val2 = val;
		double minval = ((Double) _minargvali.get(j)).doubleValue();
		double maxval = ((Double) _maxargvali.get(j)).doubleValue();
    if (val2 < minval) val2 = minval;
    else if (val2 > maxval) val2 = maxval;
    return val2;
  }
	
	
	/**
	 * implements a migration model based on the island migration topology 
	 * specified by the next-process-id's in the parameters passed in.
	 * NOTICE there is a very significant constraint that cannot be enforced
	 * but is assumed to hold for the given process, namely that the number of 
	 * individuals the first thread (_threads[0]) manages, is at least as large
	 * as the value <CODE>_master._numMigrants</CODE>, and that the same number of 
	 * migrants is used for each participating process in the distributed DE run.
	 * @param gen int
	 * @param copysols VectorIntf[] 
	 * @param copyvals double[] 
	 */
	private void doMigration(int gen, VectorIntf[] copysols, double[] copyvals) {
		// first send my data: choose randomly some vectors, put them in a vector
		// and ship them wherever they have to go
		final int ninds = _to+1;  // used to be _master._sols.length;
		final int nmigs = _master._numMigrants;  // must assume this is <= ninds
    final Random rnd = RndUtil.getInstance(_uid).getRandom();  // used to be _id
		final Messenger mger = Messenger.getInstance();
		ArrayList nums = new ArrayList(ninds);
		for (int i=0; i<ninds; i++) nums.add(new Integer(i));
		Collections.shuffle(nums, rnd);
		ArrayList imms = new ArrayList(nmigs);
		for (int i=0; i<nmigs; i++) 
			// imms.add(_master.getSol(((Integer)nums.get(i)).intValue()));
			imms.add(copysols[((Integer)nums.get(i)).intValue()]);
		try {
			// send to next process
			mger.msg("gen "+gen+": sending data to "+_master._nextProcessId, 0);
			_dmsgpassClient.sendData(_master._thisProcessId, _master._nextProcessId, 
				                       imms);
			// now receive new incomers and put them in the position of those who left
			mger.msg("gen "+gen+": receiving data from my previous", 0);
			ArrayList newcomers = 
				(ArrayList) _dmsgpassClient.recvData(_master._thisProcessId);
			if (newcomers!=null) {
				double bestval = Double.MAX_VALUE;
				VectorIntf bestmigrant = null;
				int bestpos=-1;
				for (int i=0; i<nmigs; i++) {
					VectorIntf nci = (VectorIntf) newcomers.get(i);
					final int pos = ((Integer)nums.get(i)).intValue();
					try {
						final double valpos = _master._f.eval(nci, _fp);  // signature says 
						                                                  // it may throw...
						if (copysols[pos] instanceof PoolableObjectIntf) {
							((PoolableObjectIntf) copysols[pos]).release();  // avoid leaks
						}
						copysols[pos] = nci;  // _master.setSol(pos, nci);
						copyvals[pos] = valpos;  // _master.setSolVal(pos, valpos);
						if (valpos<bestval) {
							bestval = valpos;
							bestmigrant = nci;
							bestpos = pos;
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				_master.setIncumbent(bestmigrant.newInstance(), bestval, bestpos);
			}
		}
		catch (Exception e) {  // in case of network failure, migration fails too.
			e.printStackTrace();
		}
	}
}

