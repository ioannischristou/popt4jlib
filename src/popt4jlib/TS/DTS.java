package popt4jlib.TS;

import utils.*;
import popt4jlib.*;
import parallel.*;
import java.util.*;
import java.io.*;


/**
 * The DTS class implements a parallel Tabu Search algorithm for the
 * minimization of a function to be passed in as argument object implementing
 * the FunctionIntf argument of the <CODE>minimize(f)</CODE> method.
 * The parallelization of the TS method consists of spawning a number of threads
 * that will be searching the search space utilizing a shared-short-term 
 * memory structure of tabu moves (those being small spheres around the points
 * already visited most recently.) The size of the tabu list will be a multiple
 * of the number of threads available.
 * Normally, the non-deterministic nature of the shared-memory tabu-list means 
 * that for more than 1 thread, the entire run of the algorithm is 
 * non-deterministic and we cannot guarantee that two different runs with the
 * same parameters will yield the same results. To avoid this, we force each 
 * thread in every generation to update its own copy of the shared-memory tabu
 * list, and in the end of a generation to update the global shared tabu list in
 * order, using the <CODE>parallel.OrderedBarrier</CODE> class!
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DTS implements OptimizerIntf {
  private static int _nextId=0;
  final private int _id;
  private HashMap _params=null;
  double _incValue=Double.MAX_VALUE;
  Object _inc=null;  // incumbent chromosome
	private BoundedBufferArray _tabuList;
  FunctionIntf _f=null;

  private DTSThread[] _threads=null;


  /**
   * public constructor assigns a unique id among all DTS objects it constructs.
   */
  public DTS() {
    _id = incrID();
  }


  /**
   * public constructor taking as input the parameters of the DTS process to be
   * used later on when the minimize(f) method is called. The parameters will be
   * copied so modifying the HashMap that is passed as argument later on, will
   * not affect the parameters of the DTS object.
   * Unique id is assigned to this object among all DTS objects.
   * @param params HashMap
   */
  public DTS(HashMap params) {
    this();
    try {
      setParams(params);
    } catch (Exception e) {
      e.printStackTrace();  // no-op: cannot reach this point
    }
  }


  /**
   * return a copy of the parameters. Modifications to the returned object
   * do not affect the data member.
   * @return HashMap
   */
  synchronized HashMap getParams() {
    return new HashMap(_params);
  }


  /**
   * the optimization params are set to p. Later modifying the HashMap that is
   * passed as argument, will not affect the parameters of the DTS object.
   * @param p HashMap
   * @throws OptimizerException if this method is called while another thread
   * is running the <CODE>minimize(f)</CODE> method on this object.
   */
  public synchronized void setParams(HashMap p) throws OptimizerException {
    if (_f!=null) 
			throw new OptimizerException("cannot modify parameters while running");
    _params = null;
    _params = new HashMap(p);  // own the params
  }


  /**
   * reset fields that need resetting so that another thread (or current one)
   * can re-use the object's <CODE>minimize(f)</CODE> method.
   */
  synchronized void reset() {
    try {
      OrderedBarrier.removeInstance("dts." + _id);
    }
    catch (parallel.ParallelException e) {
      e.printStackTrace();
    }
    _f = null;
    _inc = null;
    _incValue = Double.MAX_VALUE;
  }


  /**
   * introduced to keep FindBugs happy...
   * @return FunctionIntf
   */
  synchronized FunctionIntf getFunction() {
    return _f;
  }
	
	
	/**
	 * get the shared tabu-list.
	 * @return BoundedBufferArray
	 */
	synchronized BoundedBufferArray getTabuList() {
		return _tabuList;
	}


  /**
   * The most important method of the class, that runs a parallel implementation
   * of the TS method. Prior to calling this method, a number of parameters must
   * have been passed in the DTS object, either in construction time, or via a
   * call to setParams(p). These are:
   * <ul>
   * <li> &lt;"dts.randomchromosomemaker", RandomChromosomeMakerIntf r&gt; 
	 * mandatory, the object responsible for implementing the interface that 
	 * allows creating random initial chromosome objects.
   * <li> &lt;"dts.movemaker", NewChromosomeMakerIntf movemaker&gt; mandatory, 
	 * the object responsible for implementing the interface that allows creating 
	 * new chromosome Objects from an existing one (makes a move).
	 * <li> &lt;"dts.nhoodmindist", Double d&gt; mandatory, the minimum distance
	 * between two points to be considered as belonging to the same neighborhood.
	 * <li> &lt;"dts.nhooddistcalc", ArgDistanceCalcIntf&gt; mandatory, the object
	 * that allows calculation of distances between arguments.
   * <li> &lt;"dts.c2amaker", Chromosome2ArgMakerIntf c2amaker&gt; optional, an 
	 * object implementing the Chromosome2ArgMakerIntf that transforms chromosome 
	 * Objects used in the TS process -and manipulated by the Object implementing
   * the NewChromosomeMakerIntf interface- into argument Objects that can be
   * passed into the FunctionIntf object that the process minimizes. Default is
   * null, which results in the chromosome objects being passed "as-is" to the
   * FunctionIntf object being minimized.
   * <li> &lt;"dts.numthreads", Integer nt&gt; optional, number of threads to 
	 * be run by the process, default is 1.
   * <li> &lt;"dts.numiters", Integer nglobaliters&gt; optional, the total 
	 * number of "generations" ie total iterations to make per thread, 
	 * default is 100.
	 * <li> &lt;"dts.nhoodsize", Integer nhoodSize&gt; optional, the total number
	 * of moves that define a neighborhood, default is 10.
	 * <li> &lt;"dts.tabulistmaxsize", Integer maxsz&gt; optional, maximum size
	 * of the shared tabu-list. Default is 10 &times; $nt.
   * </ul>
   * The result is a PairObjDouble object that contains the best function arg.
   * along with the minimum function value obtained by this argument (or null
   * if the process fails to find any valid function argument).
   * @param f FunctionIntf
   * @throws OptimizerException if another thread is currently running the
   * <CODE>minimize(f)</CODE> method on this object, and also if anything goes
   * wrong in the optimization process.
   * @return PairObjDouble an object that holds both the best value found by the
   * DTS process run as well as the argmin -the argument that produced this best
   * value.
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("DTS.minimize(f): null f");
    try {
      int nt = 1;
      synchronized (this) {
        if (_f != null)
          throw new OptimizerException("DTS.minimize(): another thread is "+
                                       "concurrently executing the method on"+
                                       " the same object");
        _f = f;
        Integer ntI = (Integer) _params.get("dts.numthreads");
        if (ntI != null) nt = ntI.intValue();
        if (nt < 1)
					throw new OptimizerException("DTS.minimize(): invalid number of "+
						                           "threads specified");
				int tlsize = nt*10;  // default value
				Integer tlsizeI = (Integer) _params.get("dts.tabulistmaxsize");
				if (tlsizeI!=null) tlsize = tlsizeI.intValue();
        RndUtil.addExtraInstances(nt);  // not needed
				_tabuList = new BoundedBufferArray(tlsize);
        _threads = new DTSThread[nt];
      }

      for (int i = 0; i < nt; i++) {
        _threads[i] = new DTSThread(this, i);
      }
			
      for (int i=0; i<nt; i++) {
				OrderedBarrier.addThread(_threads[i], "dts." + _id); // init Barrier
			}
						
      for (int i = 0; i < nt; i++) {
        _threads[i].start();
      }

      for (int i = 0; i < nt; i++) {
        DTSThreadAux rti = _threads[i].getDTSThreadAux();
        rti.waitForTask();  // equivalent to _threads[i].join();
      }

      synchronized(this) {
        Chromosome2ArgMakerIntf c2amaker = (Chromosome2ArgMakerIntf) _params.
            get("dts.c2amaker");
        Object arg = _inc;
        if (c2amaker != null)
          arg = c2amaker.getArg(_inc, _params);
        return new PairObjDouble(arg, _incValue);
      }
    }
    finally {
      reset();
    }
  }


  /**
   * update if we have an incumbent.
   * @param ind DTSIndividual
   */
  synchronized void setIncumbent(DTSIndividual ind) {
    if (ind==null) return;
    if (_incValue > ind.getValue()) {  // minimization problems only
      System.err.println("Updating Incumbent w/ val="+ind.getValue());
      _incValue = ind.getValue();
      _inc = ind.getChromosome();
    }
  }


  /**
   * get the DTSThread with the given id for this object.
   * @param id int
   * @return DTSThread
   */
  synchronized DTSThread getDTSThread(int id) {
    return _threads[id];
  }


  /**
   * return the unique id assigned to this DSA object among all DSA objects.
   * @return int
   */
  synchronized int getId() { return _id; }


  /**
   * auxiliary method generating unique ids among this class's objects
   * @return int
   */
  synchronized private static int incrID() {
    return ++_nextId;
  }

}


/**
 * auxiliary class not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DTSThread extends Thread {
  private DTSThreadAux _aux;

  public DTSThread(DTS master, int id) throws OptimizerException {
    _aux = new DTSThreadAux(master, id);
  }


  public DTSThreadAux getDTSThreadAux() {
    return _aux;
  }


  public void run() {
		try {
			_aux.runTask();
		}
		catch (ParallelException e) {  // should never get here
			e.printStackTrace();
			System.exit(-1);  
		}
  }
}


/**
 * auxiliary class not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DTSThreadAux {
  final private int _id;
  final private int _uid;
  final private DTS _master;
  private boolean _finish = false;
  private DTSIndividual _individual=null;
  private Chromosome2ArgMakerIntf _c2arg=null;
	private ArgDistanceCalcIntf _distCalc = null;
	private double _minDist4Nbhood = 0.0;
  private HashMap _p=null;
  private HashMap _fp=null;
  private FunctionIntf _f=null;

  public DTSThreadAux(DTS master, int id) throws OptimizerException {
		final Messenger mger = Messenger.getInstance();
    _master = master;
    _id = id;
    _uid = (int) DataMgr.getUniqueId();
    _p = _master.getParams();  // returns a copy
    _p.put("thread.localid",new Integer(_id));
    _p.put("thread.id",new Integer(_uid));  // used to be _id
		// get _distCalc and _minDist4Nbhood
		_distCalc = (ArgDistanceCalcIntf) _p.get("dts.nhooddistcalc");
		_minDist4Nbhood = ((Double) _p.get("dts.nhoodmindist")).doubleValue();
    _f = _master.getFunction();
    // create the _funcParams
    _fp = new HashMap();
    Iterator it = _p.keySet().iterator();
    while (it.hasNext()) {
      String key = (String) it.next();
      Object val = _p.get(key);
      if (val!=null) {
        Class valclass = val.getClass();
        Package pack = valclass.getPackage();
        if (pack!=null) {
          String packname = pack.getName();
          if (packname.startsWith("popt4jlib") ||
              packname.startsWith("parallel"))
						continue; // don't include such objects
        }
        else {
          mger.msg("no package info for object with key "+key, 2);
        }
        _fp.put(key,val);
      }
    }
    // end creating _funcParams

    _c2arg = (Chromosome2ArgMakerIntf) _p.get("dts.c2amaker");
    if (_c2arg==null) {  // use default choice
      _c2arg = new IdentityC2ArgMaker();  // the chromosome IS the function arg.
    }
  }


  public void runTask() throws ParallelException {
		final OrderedBarrier ob = 
			OrderedBarrier.getInstance("dts."+_master.getId());
    // start: do the DTS
    try {
      getInitSolution();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    int numiters = 100;
    Integer niI = (Integer) _p.get("dts.numiters");
    if (niI!=null) numiters = niI.intValue();
    for (int gen = 0; gen < numiters; gen++) {
      // System.err.println("Island-Thread id=" + _id + " running iter=" + gen);
      ob.orderedBarrier(null);  
			// synchronize with other threads
			List ltl = null;
      try {
        ltl = runTS(gen);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      ob.orderedBarrier(new UpdateTLTask(ltl, _master.getTabuList()));
      // synchronize with other threads and update the global shared tabu list
    }
    // end: declare finish
    setFinish();
  }


  public synchronized boolean getFinish() {
    return _finish;
  }


  public synchronized void setFinish() {
    _finish = true;
    notify();
  }


  public synchronized void waitForTask() {
    while (_finish==false) {
      try {
        wait();  // wait as other operation is still running
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // recommended action
      }
    }
    // System.err.println("Thread-"+_id+" done.");
  }


  /* TS methods */


	/**
	 * the method defines the neighborhood of a point in two ways: when checking
	 * for tabu-list membership, a point belongs to a tabu-list if its distance
	 * from any member of the list is shorter than a user-defined threshold; and
	 * when creating the neighbors of a point, we simply call the move-maker a 
	 * specified number of times to create the neighborhood.
	 * @param gen
	 * @return List a list of the new solutions that entered the local tabu search
	 * @throws OptimizerException 
	 */
  private List runTS(int gen) throws OptimizerException {
		final Messenger mger = Messenger.getInstance();
    mger.msg("thread: "+_id+" gen="+gen, 1);
    final BoundedBufferArray tabuList = _master.getTabuList();
		final BoundedBufferArray localTabuList = 
			new BoundedBufferArray(tabuList.getMaxSize());
		final List newSolsList = new ArrayList();
		// populate localTabuList
		for (int i=0; i<tabuList.size(); i++) {
			try {
				localTabuList.addElement(tabuList.elementAt(i));
			}
			catch (ParallelException e) { // never gets here
				e.printStackTrace();
			}
		}
		final NewChromosomeMakerIntf mover = 
			(NewChromosomeMakerIntf) _p.get("dts.movemaker");
		Integer nbhoodNumMovesI = (Integer) _p.get("dts.nhoodsize");
		int nhoodsize = 10;  // default
		if (nbhoodNumMovesI!=null) nhoodsize = nbhoodNumMovesI.intValue();
    int accepted=0;
    int rejected=0;
		for (int i=0; i<nhoodsize; i++) {
	    Object newsol = mover.createNewChromosome(_individual.getChromosome(), 
				                                        _p);
			Object newarg = _c2arg.getArg(newsol, _p);
			// ensure newarg is not "tabu"
			if (isTabu(newarg, localTabuList)) {
				++rejected;
				continue;
			}
			double newval = Double.MAX_VALUE;
			try {
				newval = _f.eval(newarg, _fp);  // used to be _p, _master._f
			}  
			catch (Exception e) {
				mger.msg("DTSThreadAux.runTS(): _f.eval() threw "+e.toString()+
						     ", ignoring and continuing",0);
			}
			double df = newval - _individual.getValue();
      if (df < 0) {
        // accept
        accepted++;
        _individual = new DTSIndividual(newsol, _master, _fp);
        _master.setIncumbent(_individual);
				// update local-tabu-list
				updateTabuList(newarg, localTabuList);
				newSolsList.add(newarg);
      }
			else ++rejected;
    }
    System.err.println("thread: "+_id+" gen="+gen+" acc="+accepted+
			                 " rej="+rejected);
		return newSolsList;
  }


  private void getInitSolution() throws OptimizerException, 
		                                    InterruptedException {
    RandomChromosomeMakerIntf amaker = 
			(RandomChromosomeMakerIntf) _p.get("dts.randomchromosomemaker");
    // what to do if no such maker is provided? must have a default one
    if (amaker==null) 
			throw new OptimizerException("no RandomChromosomeMakerIntf "+
                                   "provided in the params HashMap");
    Object chromosome = amaker.createRandomChromosome(_p);
    _individual = new DTSIndividual(chromosome, _master, _fp);
    _master.setIncumbent(_individual);  // update master's best soln found if 
		                                    // needed
  }
	
	
	private boolean isTabu(Object arg, BoundedBufferArray ltl) {
		for (int i=0; i<ltl.size(); i++) {
			try {
				Object si = ltl.elementAt(i);
				if (_distCalc.dist(arg, si) <= _minDist4Nbhood) return true;
			}
			catch (ParallelException e) {  // can never get here
				e.printStackTrace();
			}
		}
		return false;
	}
	
	
	private void updateTabuList(Object arg, BoundedBufferArray ltl) {
		try {
			if (ltl.size()==ltl.getMaxSize()) ltl.remove();
			ltl.addElement(arg);
		}
		catch (ParallelException e) {  // can never get here
			e.printStackTrace();
		}
	}

}


/**
 * auxiliary class, not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DTSIndividual {
  private Object _chromosome;
  private double _val=Double.MAX_VALUE;  // raw objective value
  private DTS _master;  // ref. back to master DTS object
  private FunctionIntf _f;  // ref. back to master's _f function

  public DTSIndividual(Object chromosome, DTS master, HashMap p) {
    _chromosome = chromosome;
    _master = master;
    _f = _master.getFunction();
		try {
			computeValue(p);  // may throw OptimizerException
		}
		catch (OptimizerException e) {
			final Messenger mger = Messenger.getInstance();
			mger.msg("DTSIndividual: chromosome "+chromosome.toString()+
				       " computeValue() threw "+e.toString()+
				       "; _val will remain Double.MAX_VALUE and continue", 0);
		}
  }


  public DTSIndividual(Object chromosome, double val, DTS master) {
    _chromosome = chromosome;
    _val = val;
    _master = master;
  }


  public String toString() {
    String r = "Chromosome=[";
    r += _chromosome.toString();
    r += "] val="+_val;
    return r;
  }
  public Object getChromosome() { return _chromosome; }
  public double getValue() { return _val; }  // enhance the density value 
	                                           // differences
  public void computeValue(HashMap p) throws OptimizerException {
    if (_val==Double.MAX_VALUE) {  // don't do the computation if already done
      Chromosome2ArgMakerIntf c2amaker =
          (Chromosome2ArgMakerIntf) p.get("dts.c2amaker");
      Object arg = null;
      if (c2amaker == null) arg = _chromosome;
      else arg = c2amaker.getArg(_chromosome, p);
			try {
				_val = _f.eval(arg, p);  // was _master._f which is also safe
			}
			catch (Exception e) {
				throw new OptimizerException("DTSIndividual.computeValue(): _f.eval() "+
					                           " threw "+e.toString());
			}
    }
  }
}


/**
 * auxiliary class not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class UpdateTLTask implements TaskObject {
	private List _newSols;
	private BoundedBufferArray _tabuList;

	
	/**
	 * sole constructor.
	 * @param newSols List  // List&lt;Object arg&gt;
	 * @param tabuList BoundedBufferArray
	 */
	UpdateTLTask(List newSols, BoundedBufferArray tabuList) {
		_newSols = newSols;
		_tabuList = tabuList;
	}
	
	
	/**
	 * the main computation simply updates the tabu-list with the objects in the
	 * list.
	 * @return null 
	 */
	public Serializable run() {
		if (_newSols==null) return null;
		try {
			for (int i=0; i<_newSols.size(); i++) {
				if (_tabuList.size()==_tabuList.getMaxSize()) _tabuList.remove();
				_tabuList.addElement(_newSols.get(i));
			}
		}
		catch (ParallelException e) {  // can never get here
			e.printStackTrace();
		}
		return null;
	}
	
	
  /**
   * returns true only if the computation carried out by the run() method has
   * been completed.
   * @return boolean always returns true
   */
  public boolean isDone() {
		return true;
	}


  /**
   * always throws.
   * @param other TaskObject the object whose state must be copied onto this.
   * @throws IllegalArgumentException if other can't be copied onto this object.
   */
  public void copyFrom(TaskObject other) throws IllegalArgumentException {
		throw new IllegalArgumentException("not implemented");
	}
}