package popt4jlib.GA;

import parallel.*;
import utils.*;
import popt4jlib.*;
import java.util.*;

/**
 * The DGA class implements a thread-safe parallel Distributed Genetic Algorithm
 * that follows the island-model of GA computation, with an elitist selection
 * strategy that uses roulette-wheel selection of individuals based on their
 * fitness (that is computed every generation). The GA allows crossover and
 * mutation operators to act on the populations' indidivuals' chromosomes to
 * produce new individuals. It also features some less common properties, such
 * as:
 * <ul>
 * <li>1. a migration model that is based on "island starvation", i.e. whenever an
 * island (run by a dedicated thread, DGAThread) has very small population as
 * measured either on absolute numbers (0) or in relative numbers (less than
 * the population of another island divided by 2.5), then the "near-empty"
 * island becomes a host for migrating individuals (which shall be the "best"
 * from their respective islands).
 * <li>2. an aging mechanism via which individuals are removed from the population
 * once they reach their (randomly drawn from a Gaussian distribution) generation
 * limit.
 * </ul>
 * <p>
 * Both the above mechanisms are intended to reduce premature convergence effects
 * of the process that often plague genetic evolution optimization processes.
 * Finally, the class implements the Subject/ObserverIntf interfaces so that it
 * may be combined with other optimization processes and produce better results.
 * By implementing the SubjectIntf of the well-known Observer Design Pattern,
 * the class allows any observer objects implementing the ObserverIntf to be
 * notified whenever a new incumbent is found and then to enrich the class's
 * population (in island thread with id=0) by adding back into the first island's
 * population new (hopefully better) solutions. The solutions moved back and
 * forth between optimizers have to be moved as function argument objects and
 * not as chromosome objects.</p>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DGA implements OptimizerIntf, SubjectIntf, ObserverIntf {
  private static int _nextId=0;
  private int _id;
  private Hashtable _params;
  private boolean _setParamsFromSameThreadOnly=false;
  private Thread _originatingThread=null;
  private Chromosome2ArgMakerIntf _c2amaker;
  private Hashtable _observers;  // map<ObserverIntf o, Vector<Object> newSols>
  private Hashtable _subjects;  // map<ObserverIntf o, Vector<Object> newSols>
	private int _numIncUpdates=0;  // how many times soln was improved in process
  double _incValue=Double.MAX_VALUE;
  Object _inc;  // incumbent chromosome
  FunctionIntf _f;

  private DGAThread[] _threads=null;
  private int[] _islandsPop;
  private double _lastIncObserved=Double.MAX_VALUE;

  /**
   * public constructor. Provides a unique String id to the object that is
   * "DGA."+getId() which is used e.g. in the Barrier method calls, and
   * initializes the _observers and _subjects data member.
   */
  public DGA() {
    _id = incrID();
    _observers = new Hashtable();
    _subjects = new Hashtable();
  }


  /**
   * public constructor. The parameters to be used by the minimize(f) method are
   * passed in as the single argument to the method. The parameters are uniquely
   * owned by the DGA object.
   * @param params Hashtable
   */
  public DGA(Hashtable params) {
    this();
    try {
      setParams(params);
    }
    catch (Exception e) {
      // no-op: cannot reach this point
    }
  }


  /**
   * Constructor of a DGA object, that assigns a unique id plus the parameters
   * passed into the argument. Also, it prevents other threads from modifying
   * the parameters passed into this object if the second argument is true.
   * @param params Hashtable
   * @param setParamsOnlyFromSameThread boolean
   */
  public DGA(Hashtable params, boolean setParamsOnlyFromSameThread) {
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
   * return a copy of the parameters that were either passed in during
   * construction or via a call to setParams(p).
   * @return Hashtable
   */
  public synchronized Hashtable getParams() {
    return new Hashtable(_params);
  }


  /**
   * the optimization params are set to p
   * @param p Hashtable
   * @throws OptimizerException if another thread is concurrently running the
   * <CODE>minimize(f)</CODE> method of this object. Note that unless the 2-arg
   * constructor DGA(params, use_from_same_thread_only=true) is used to create
   * this object, it is perfectly possible for one thread to call setParams(p),
   * then another to setParams(p2) to some other param-set, and then the
   * first thread to call minimize(f).
   */
  public synchronized void setParams(Hashtable p) throws OptimizerException {
    if (_f!=null) throw new OptimizerException("cannot modify parameters while running");
    if (_setParamsFromSameThreadOnly) {
      if (Thread.currentThread()!=_originatingThread)
        throw new OptimizerException("Current Thread is not allowed to call setParams() on this DGA.");
    }
    _params = null;
    _params = new Hashtable(p);  // own the params
    _c2amaker = (Chromosome2ArgMakerIntf) _params.get("dga.c2amaker");
  }


  // SubjectIntf methods implementation
  /**
   * allows an Object that implements the ObserverIntf interface to register
   * with this DGA object and thus be notified whenever new incumbent solutions
   * are produced by the DGA process. The ObserverIntf objects may then
   * independently produce their own new solutions and add them back into the
   * DGA process via a call to addIncumbent(observer, functionarg).
   * The order of events cannot be uniquely defined and the experiment may not
   * always produce the same results.
   * @param observer ObserverIntf.
   * @return boolean returns always true.
   */
  public boolean registerObserver(ObserverIntf observer) {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      _observers.put(observer, new Vector());
      return true;
    }
    catch (ParallelException e) {
      e.printStackTrace();
      return false;
    }
    finally {
      try {
        DMCoordinator.getInstance("popt4jlib").releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
  }
  /**
   * removes an Object that implements the ObserverIntf that has been registered
   * to listen for new solutions. Returns true if the observer was registered,
   * false otherwise.
   * @param observer ObserverIntf
   * @return boolean
   */
  public boolean removeObserver(ObserverIntf observer) {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      int size = _observers.size();
      _observers.remove(observer);
      return (size == _observers.size() + 1);
    }
    catch (ParallelException e) {
      e.printStackTrace();
      return false;
    }
    finally {
      try {
        DMCoordinator.getInstance("popt4jlib").releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
  }
  /**
   * notifies every ObserverIntf object that was registered via a call to
   * registerObserver(obs) -and has not been removed since- by calling the
   * ObserverIntf object's method notifyChange(SubjectIntf this).
   */
  public void notifyObservers() {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      Iterator it = _observers.keySet().iterator();
      while (it.hasNext()) {
        ObserverIntf oi = (ObserverIntf) it.next();
        try {
          oi.notifyChange(this);
        }
        catch (OptimizerException e) {
          e.printStackTrace(); // no-op
        }
      }
    }
    catch (ParallelException e) {
      e.printStackTrace();
    }
    finally {
      try {
        DMCoordinator.getInstance("popt4jlib").releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
  }
  /**
   * returns the current function that is being minimized (may be null)
   * @return FunctionIntf
   */
  public synchronized FunctionIntf getFunction() {
    return _f;
  }
  /**
   * returns the currently best known function argument that  minimizes the _f
   * function. The ObserverIntf objects would need this method to get the
   * current incumbent (and use it as they please). Note: the method is not
   * synchronized, but it still executes atomically, and is in synch with
   * the DGAThreads executing setIncumbent().
   * @return Object
   */
  public Object getIncumbent() {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      Chromosome2ArgMakerIntf c2amaker =
          (Chromosome2ArgMakerIntf) _params.get("dga.c2amaker");
      Object arg = null;
      try {
        if (c2amaker == null) arg = _inc;  // atomic access to _XXX objects
                                           // (_params, _inc)
                                           // FindBugs complains unjustly here
        else arg = c2amaker.getArg(_inc, _params);
      }
      catch (OptimizerException e) {
        e.printStackTrace(); // no-op
      }
      return arg; // return the best found function argument
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    finally {
      try {
        DMCoordinator.getInstance("popt4jlib").releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
    // return _inc;  // return the best found chromosome.
  }
  /**
   * allows an ObserverIntf object to add back into the DGA process an
   * improvement to the incumbent solution it was given. The method should only
   * be called by the ObserverIntf object that has been registered to improve
   * the current incumbent.
   * @param obs ObserverIntf
   * @param soln Object
   */
  public void addIncumbent(ObserverIntf obs, Object soln) {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      // add new solution back
      Vector sols = (Vector) _observers.get(obs);
      if (sols == null) {
        return; // ObserverIntf was not registered or was removed
      }
      sols.addElement(soln);
    }
    catch (ParallelException e) {
      e.printStackTrace();
    }
    finally {
      try {
        DMCoordinator.getInstance("popt4jlib").releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
  }


  // ObserverIntf methods implementation
  /**
   * when a subject's thread calls the method notifyChange, in response, this
   * object will add the best solution found by the subject, in the _subjects'
   * solutions map, to be later picked up by the first DGAThread spawned by this
   * DGA object.
   * @param subject SubjectIntf
   * @throws OptimizerException
   */
  public void notifyChange(SubjectIntf subject) throws OptimizerException {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      Object arg = subject.getIncumbent();
      addSubjectIncumbent(subject, arg);  // add the solution found by the
      // subject to my solutions so that it will be picked up in the
      // next generation from _threads[0].
    }
    catch (ParallelException e) {
      e.printStackTrace();
    }
    finally {
      try {
        DMCoordinator.getInstance("popt4jlib").releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (passed in the ctor or via  prior call
   * to setParams(p) to do that).
   * These are:
   * <ul>
   * <li>&lt;"dga.randomchromosomemaker",RandomChromosomeMakerIntf maker&gt; mandatory,
   * the RandomChromosomeMakerIntf Object responsible for creating valid random
   * chromosome Objects to populate the islands.
   * <li>&lt;"dga.xoverop", XoverOpIntf xoverOp&gt; mandatory, the XoverOpIntf Object that
   * produces two new chromosome Objects from two old chromosome Objects. It is
   * the responsibility of the operator to always return NEW Objects.
   * <li>&lt;"dga.mutationop", MutationOpIntf mutationOp&gt; optional, if present, the
   * operator will always be applied to the resulting Objects that the
   * XoverOpIntf will produce, default is null.
   * <li>&lt;"dga.numthreads",Integer nt&gt; optional, how many threads will be used,
   * default is 1. Each thread corresponds to an island in the DGA model.
   * <li>&lt;"dga.c2amaker",Chromosome2ArgMakerIntf c2a&gt; optional, the object that is
   * responsible for transforming a chromosome Object to a function argument
   * Object. If not present, the default identity transformation is assumed.
   * <li>&lt;"dga.a2cmaker",Arg2ChromosomeMakerIntf a2c&gt; optional, the object that is
   * responsible for transforming a FunctionIntf argument Object to a chromosome
   * Object. If not present, the default identity transformation is assumed. The
   * a2c object is only useful when other ObserverIntf objects register for this
   * SubjectIntf object and also add back solutions to it (as FunctionIntf args)
   * <li>&lt;"dga.numgens",Integer ng&gt; optional, the number of generations to run the
   * GA, default is 1.
   * <li>&lt;"dga.numinitpop",Integer ip&gt; optional, the initial population number for
   * each island, default is 10.
   * <li>&lt;"dga.poplimit",Integer pl&gt; optional, the maximum population for each
   * island, default is 100.
   * <li>&lt;"dga.xoverprob",Double rate&gt; optional, the square root of the expectation
   * of the number of times cross-over will occur divided by island population
   * size, default is 0.7.
   * <li>&lt;"dga.cutoffage",Integer maxage&gt; optional, the number of generations an
   * individual is expected to live before being removed from the population,
   * default is 5.
   * <li>&lt;"dga.varage", Double varage&gt; optional, the variance in the number of
   * generations an individual will remain in the population before being
   * removed, default is 0.9.
   * <li>&lt;"ensemblename", String name&gt; optional, the name of the synchronized
   * optimization ensemble in which this DGA object will participate. In case
   * this value is non-null, then a higher-level ensemble optimizer must have
   * appropriately called the method
   * <CODE>Barrier.setNumThreads(name+"_master", numParticipantOptimizers)</CODE>
   * so as to properly synchronize the various participating optimizers' threads.
   * Also, if this object is to be re-used as a stand-alone optimizer,
   * this key-value pair must be removed from its parameter-set via a call to
   * <CODE>setParams(p)</CODE> prior to calling <CODE>minimize(f)</CODE> again.
   * If the optimizer ensemble is to call its <CODE>minimize(f)</CODE> method
   * again, then it must make sure to have reset the
   * <CODE>Barrier.getInstance(name+"_master")</CODE> object via a
   * <PRE>
	 * <CODE>
   * Barrier.removeInstance(name+"_master");
   * Barrier.setNumThreads(name+"_master", numOptimizers);
   * ComplexBarrier.removeInstance(name);
   * </CODE>
	 * </PRE>
   * series of calls.
   * <p> This implementation requires that the value of the key "dga.poplimit" 
	 * multiplied by the value of the key "dga.numthreads" times the value of the
	 * key "dga.numgens" is less than the value
	 * of the call <CODE>parallel.MsgPassingCoordinator.getMaxSize()</CODE> that 
	 * currently returns 10000 (otherwise, there is the risk of the method hanging
	 * up in pathological circumstances when all enough individuals in the 
	 * population are current incumbents). If this requirement is not met, an 
	 * OptimizerException will be thrown.
   * @param FunctionIntf f
   * @throws OptimizerException if the optimization process fails; also see above
	 * discussion
   * @return PairObjDouble Pair&lt;Object arg, Double val&gt;
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("DGA.minimize(f): null f");
    try {
      // atomic.start(Constants.OB);  // no longer needed to protect OrderedBarrier
      synchronized (this) {  // protect against another thread setting params
        if (_f != null) throw new OptimizerException("DGA.minimize(): "+
          "another thread is concurrently executing the method on the same object");
        _f = f;
        _lastIncObserved=Double.MAX_VALUE;  // reset value: synched to keep
                                            // FindBugs happy
      }
      // add the function itself onto the parameters hashtable for possible use
      // by operators. The function has to be reentrant in any case for multiple
      // concurrent evaluations to be possible in the first place so there is no
      // new issue raised here
      if (_params.get("dga.function") == null)
        _params.put("dga.function", f);
      int nt = 1;
      Integer ntI = (Integer) _params.get("dga.numthreads");
      if (ntI != null) nt = ntI.intValue();
      if (nt < 1)throw new OptimizerException(
          "DGA.minimize(): invalid number of threads specified");
			// sanity check: num_gens*num_threads*poplimit < MsgPassingCoordinator.getMaxSize()
			int poplimit = 100;
			Integer plI = null;
			try {
				plI = (Integer) _params.get("dga.poplimit");
				if (plI!=null) poplimit = plI.intValue();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
      int numgens = 1;
      try {
        Integer ngI = (Integer) _params.get("dga.numgens");
        if (ngI!=null) numgens = ngI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
			int pnt = poplimit*nt*numgens;
			final int mpcms = MsgPassingCoordinator.getMaxSize(); 
			if (pnt<0 || pnt>mpcms)
				throw new OptimizerException("DGA.minimize(): poplimit x numthreads x numgens "+
								                     "must be less than MsgPassingCoordinator.getMaxSize() (="+mpcms+")");
			// end sanity check assertion
      RndUtil.addExtraInstances(nt);  // not needed
      // check if this object will participate in an ensemble
      String ensemble_name = (String) _params.get("ensemblename");
      _threads = new DGAThread[nt];
      _islandsPop = new int[nt];
      for (int i = 0; i < nt; i++) _islandsPop[i] = 0; // init. will be in synch
                                                       // with worker threads
                                                       // below (FindBugs
                                                       // complains unjustly)
      try {
        Barrier.setNumThreads("dga."+_id, nt); // init the Barrier obj.
      }
      catch (ParallelException e) {
        e.printStackTrace();
        throw new OptimizerException("barrier init. failed");
      }

      for (int i = 0; i < nt; i++) {
        _threads[i] = new DGAThread(this, i);
        OrderedBarrier.addThread(_threads[i], "dga."+_id); // for use in synchronizing/ordering
        if (ensemble_name!=null) {
          ComplexBarrier.addThread(ensemble_name, _threads[i]);
        }
      }
      if (ensemble_name!=null) {  // presumably, the coordinating ensemble
                                  // has already called:
        // Barrier.setNumThreads(ensemble_name+"_master", <numOptimizers>)
        try {
          Barrier.getInstance(ensemble_name + "_master").barrier();
        }
        catch (NullPointerException e) {
          e.printStackTrace();
          utils.Messenger.getInstance().msg("this DGA.minimize(f) method must "+
                                            "be invoked from within the minimize(f)"+
                                            " method of an ensemble optimizer "+
                                            "that has properly called the "+
                                            "Barrier.setNumThreads(<name>_master,<num>)"+
                                            " method.",0);
          throw new OptimizerException("DGA.minimize(f): ensemble broken");
        }
      }
      for (int i = 0; i < nt; i++) {
        _threads[i].start();
      }

      // receive incumbents and call setIncumbent() in deterministic order
      for (int i=0; i<numgens; i++) {
        for (int j=0; j<nt; j++) {
          while (true) {
            DGAIndividual ind = (DGAIndividual)
                MsgPassingCoordinator.getInstance("dga."+_id).recvData( -1, j);  // itc 2014-31-10: name of MPC used to be "dga" only
            if (ind != null) setIncumbent(ind);
            else break;  // DGAThread sends null when done sending incumbents
          }
        }
      }

      // wait for all threads to finish
      for (int i = 0; i < nt; i++) {
        DGAThreadAux rti = _threads[i].getDGAThreadAux();
        rti.waitForTask();
      }

			/*
			System.err.println("Total #DGAIndividual Objects Created="+DGAIndividual.getTotalNumObjs());
			System.err.println("Total #DGAIndividual Objects Released="+DGAIndividual.getTotalNumObjsDeleted());
			*/

      synchronized (this) {
        utils.Messenger.getInstance().msg("DGA.minimize(): total #inc_updates="+_numIncUpdates, 1);
				Chromosome2ArgMakerIntf c2amaker = (Chromosome2ArgMakerIntf) _params.
            get("dga.c2amaker");
        Object arg = _inc;
        if (c2amaker != null)
          arg = c2amaker.getArg(_inc, _params);
        return new PairObjDouble(arg, _incValue);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("DGA.minimize() failed");
    }
    finally {
      reset();
      // atomic.end(Constants.OB);  // no longer needed to protect OrderedBarrier
    }
  }


  /**
   * compute the id of the island to which the island with id myid should send
   * any immigrants
   * @param myid int
   * @return int
   */
  synchronized int getImmigrationIsland(int myid) {
    for (int i=0; i<_islandsPop.length; i++)
      if (myid!=i && (_islandsPop[i]==0 || _islandsPop[myid]>2.5*_islandsPop[i])) return i;
    return -1;
  }


  /**
   * set the population of the island with the given id.
   * @param id int
   * @param size int
   */
  synchronized void setIslandPop(int id, int size) {
    _islandsPop[id] = size;
  }


  /**
   * return the DGAThread object corresponding to the given id.
   * @param id int
   * @return DGAThread
   */
  synchronized DGAThread getDGAThread(int id) {
    return _threads[id];
  }


  /**
   * get the current incumbent value
   * @return double
   */
  synchronized double getIncValue() {
    return _incValue;
  }


  /**
   * update if we have an incumbent
   * @param ind DGAIndividual
   * @throws OptimizerException in case of insanity (may only happen if the
   * function to be minimized is not reentrant and the debug bit
   * <CODE>Constants.DGA</CODE> is set in the <CODE>Debug</CODE> class)
   */
  void setIncumbent(DGAIndividual ind) throws OptimizerException {
    // method used to be synchronized but does not need to be
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      if (_incValue > ind.getValue()) {  // minimization problems only
        double rthres = 0.01;
        Double relthresD = (Double) _params.get(
                                      "dga.observernotificationminthreshold");
        if (relthresD != null) rthres = relthresD.doubleValue();
        double rel_diff = ind.getValue() != 0 ?
            Math.abs( (_lastIncObserved - ind.getValue()) / ind.getValue()) :
            Double.MAX_VALUE;
        Messenger.getInstance().msg("Updating Incumbent w/ val=" +
                                    ind.getValue(),0);
        if (Debug.debug(Constants.DGA) != 0) {
          // sanity check
          Object arg = ind.getChromosome(); // assume the chromosome Object is
          // the same used for function evals.
          if (_c2amaker != null) // oops, no it wasn't
            arg = _c2amaker.getArg(ind.getChromosome(), _params);
          double incval = _f.eval(arg, _params);
          if (Math.abs(incval - ind.getValue()) > 1.e-25) {
            Messenger.getInstance().msg("DGA.setIncumbent(): ind-val=" +
                                        ind.getValue() + " fval=" + incval +
                                        " ???", 0);
            throw new OptimizerException(
                        "DGA.setIncumbent(): insanity detected; " +
                        "most likely evaluation function is " +
                        "NOT reentrant... " +
                        "Add the 'function.notreentrant,num'" +
                        " (num=1 or 2) to run parameters");
          }
          // end sanity check
        }
        _incValue = ind.getValue();
        _inc = (ind.getChromosome() instanceof PoolableObjectIntf) ?
								((PoolableObjectIntf)ind.getChromosome()).cloneObject() :
								ind.getChromosome();
				++_numIncUpdates;
        if (rel_diff > rthres) {
          _lastIncObserved = ind.getValue();
          notifyObservers(); // notify any observers listening
        }
      }
    }
    catch (ParallelException e) {
      e.printStackTrace();
      throw new OptimizerException("DGA.minimize(f): double locking failed");
    }
    finally {
      try {
        DMCoordinator.getInstance("popt4jlib").releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * get this DGA object's id
   * @return int
   */
  synchronized int getId() { return _id; }


  /**
   * moves all chromosomes that the observers or subjects have added so far to the
   * _individuals List<DGAIndividual> of the DGAThread with id 0, and clears
   * the solutions from the _observers and _subjects maps.
   * @param tinds List ArrayList<DGAIndividual>
   * @param params Hashtable the optimization params
   */
  void transferSolutionsTo(List tinds, Hashtable params) {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      // 1. observers
      int ocnt=0;
      Iterator it = _observers.keySet().iterator();
      while (it.hasNext()) {
        ObserverIntf obs = (ObserverIntf) it.next();
        Vector sols = (Vector) _observers.get(obs);
        int solssz = sols.size();
        for (int i = 0; i < solssz; i++) {
          try {
            Object chromosomei = null;
            Arg2ChromosomeMakerIntf a2cmaker = (Arg2ChromosomeMakerIntf) params.get("dga.a2cmaker");
            if (a2cmaker != null)
              chromosomei = a2cmaker.getChromosome(sols.elementAt(i), params);
            else {
							chromosomei = sols.elementAt(i);
							if (chromosomei instanceof PoolableObjectIntf) {
								PoolableObjectIntf pci = (PoolableObjectIntf) chromosomei;
								if (pci.isManaged()) chromosomei = pci.cloneObject();
							}
						} // assume chromosome and arg are the same
            DGAIndividual indi = new DGAIndividual(chromosomei, this, params);
            tinds.add(indi);
            ++ocnt;
          }
          catch (Exception e) {
            e.printStackTrace(); // report failure to create individual out of the
            // provided chromosome Object
          }
        }
        sols.clear();
      }
      // 2. subjects
      int scnt=0;
      it = _subjects.keySet().iterator();
      while (it.hasNext()) {
        SubjectIntf subj = (SubjectIntf) it.next();
        Vector sols = (Vector) _subjects.get(subj);
        int solssz = sols.size();
        for (int i = 0; i < solssz; i++) {
          try {
            Object chromosomei = null;
            Arg2ChromosomeMakerIntf a2cmaker = (Arg2ChromosomeMakerIntf) params.get("dga.a2cmaker");
            if (a2cmaker != null)
              chromosomei = a2cmaker.getChromosome(sols.elementAt(i), params);
            else {
							chromosomei = sols.elementAt(i);
							if (chromosomei instanceof PoolableObjectIntf) {
								PoolableObjectIntf pci = (PoolableObjectIntf) chromosomei;
								if (pci.isManaged()) chromosomei = pci.cloneObject();
							}
						} // assume chromosome and arg are the same
            DGAIndividual indi = new DGAIndividual(chromosomei, this, params);
            tinds.add(indi);
            ++scnt;
          }
          catch (Exception e) {
            e.printStackTrace(); // report failure to create individual out of the
            // provided chromosome Object
          }
        }
        sols.clear();
      }
      Messenger.getInstance().msg("DGA.transfer(): totally "+
                                  ocnt+" sols from observers and "+
                                  scnt+" sols from subjects transferred",2);
    }
    catch (ParallelException e) {
      e.printStackTrace();
    }
    finally {
      try {
        DMCoordinator.getInstance("popt4jlib").releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * get the object responsible for converting a chromosome Object to another
   * Object that can be passed in as argument of the function _f being
   * optimized
   * @return Chromosome2ArgMakerIntf
   */
  synchronized Chromosome2ArgMakerIntf getChromosome2ArgMaker() {
    // synchronized to keep FindBugs happy
    return _c2amaker;
  }


  /**
   * reset the OrderedBarrier object for new use, as well the other members
   * that need resetting.
   */
  private synchronized void reset() {
    try {
      OrderedBarrier ob = OrderedBarrier.getInstance("dga."+_id);
			if (ob!=null) ob.reset();
      try {
				OrderedBarrier.removeInstance("dga."+_id);
			}
			catch (ParallelException e) {
				e.printStackTrace();
			}
			try {
				parallel.Barrier.removeInstance("dga."+_id);
			}
			catch (ParallelException e) {
				e.printStackTrace();
			}
      _f = null;
      _inc = null;
      _incValue = Double.MAX_VALUE;
			_numIncUpdates = 0;
    }
    catch (ParallelException e) {
      e.printStackTrace();
    }
  }


  /**
   * add the soln into a hashmap maintaining (SubjectIntf, Object soln) pairs
   * so that the soln is inserted in the first DGAThread's population in the
   * next iteration. Only called from the <CODE>notifyChange()</CODE> method.
   * @param subject SubjectIntf
   * @param soln Object
   */
  private void addSubjectIncumbent(SubjectIntf subject, Object soln) {
    // add new solution back
    Vector sols = (Vector) _subjects.get(subject);
    if (sols == null) sols = new Vector();
    sols.addElement(soln);
    _subjects.put(subject, sols);
  }


  /**
   * auxiliary method incrementing the unique ids for objects of this class
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
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DGAThread extends Thread {
  private DGAThreadAux _aux;


  public DGAThread(DGA master, int id) throws OptimizerException {
    _aux = new DGAThreadAux(master, id);
  }


  public DGAThreadAux getDGAThreadAux() {
    return _aux;
  }


  public void run() {
    _aux.runTask();
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
class DGAThreadAux {
  private int _id;
  private int _uid;
  private DGA _master;
  private Hashtable _p;
  private Hashtable _fp;  // params outside the popt4jlib packages
  private boolean _finish = false;
  private ArrayList _individuals;  // List<Individual>
  private ArrayList _immigrantsPool;  // List<Individual>
  private int _maxpopnum = 100;  // max pop. size used to be init. to 50
  private XoverOpIntf _xoverOp=null;
  private MutationOpIntf _mutationOp=null;
	private IndComp _indComp = new IndComp();


  public DGAThreadAux(DGA master, int id) throws OptimizerException {
    _master = master;
    _id = id;
    _uid = (int) DataMgr.getUniqueId();
    _p = _master.getParams();  // returns a copy
    _p.put("thread.localid", new Integer(_id));
    _p.put("thread.id",new Integer(_uid));  // used to be _id
    // create the _funcParams
    _fp = new Hashtable();
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
              packname.startsWith("parallel"))continue; // don't include such objects
        }
        else {
          Messenger.getInstance().msg("no package info for object with key "+key,2);
        }
        _fp.put(key,val);
      }
    }
    // end creating _funcParams
		try {
			_maxpopnum = ((Integer) _p.get("dga.poplimit")).intValue();
		}
		catch (Exception e) {
			// no-op
			Messenger.getInstance().msg("no integer dga.poplimit value found for _maxpopnum: set to default 100.", 2);
		}
    _immigrantsPool = new ArrayList();
    _xoverOp = (XoverOpIntf) _p.get("dga.xoverop");
    _mutationOp = (MutationOpIntf) _p.get("dga.mutationop");
  }

/*
  public void runTask() {
    // start: do the DGA
    try {
      initPopulation();
      System.err.println("initPopulation() done.");
      int numgens = 10;
      Integer ngI = (Integer) _master.getParams().get("dga.numgens");
      if (ngI!=null) numgens = ngI.intValue();
      for (int gen = 0; gen < numgens; gen++) {
        parallel.Barrier.getInstance().barrier();  // synchronize with other threads
        System.err.println("Island-Thread id=" + _id + " running gen=" + gen +
                           " popsize=" + _individuals.size());
        recvInds();
        if (_individuals.size()==0) {
          parallel.Barrier.getInstance().barrier();  // synchronize with other threads
          continue;
        }
        nextGeneration();
        sendInds();
        //updateMasterSolutions(); // update the intermediateClusters of _master
        _master.setIslandPop(_id, _individuals.size());
        parallel.Barrier.getInstance().barrier();  // synchronize with other threads
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    // end: declare finish
    setFinish();
  }
*/

  public void runTask() {
		final int master_id = _master.getId();
    // start: do the DGA
    try {
      initPopulation();
    }
    catch (Exception e) {
      e.printStackTrace();  // no-op
    }
    //System.err.println("initPopulation() done.");
    int numgens = 1;
    String ensemble_name=null;
    try {
      Integer ngI = (Integer) _p.get("dga.numgens");
      if (ngI != null) numgens = ngI.intValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    try {
      ensemble_name = (String) _p.get("ensemblename");
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    for (int gen = 0; gen < numgens; gen++) {
      Barrier.getInstance("dga."+master_id).barrier();  // synchronize with other threads
      try {
        if (ensemble_name!=null)
          ComplexBarrier.getInstance(ensemble_name).barrier(); // synchronize with other optimizers' threads
      }
      catch (ParallelException e) {
        e.printStackTrace();  // no-op
      }
      Messenger.getInstance().msg("Island-Thread id=" + _id +
                                  " running gen=" + gen +
                                  " popsize=" + _individuals.size(),1);
      recvInds();
      if (_individuals.size()>0) {
        try {
          nextGeneration(gen);
        }
        catch (Exception e) {
          e.printStackTrace();  // no-op
        }
      }
      else {  // send null signal to master thread regarding incumbents
				// itc 2014-31-10: change name of MPC from "dga" to "dga."+master_id 
				// to avoid interference issues in case there are two or more threads in
				// a program running simultaneously the minimize(f) method of different
				// DGA objects.
				MsgPassingCoordinator.getInstance("dga."+master_id).sendDataBlocking(_id, null);  // signal end of incumbents
      }
      sendInds();
      _master.setIslandPop(_id, _individuals.size());
      Barrier.getInstance("dga."+_master.getId()).barrier();  // synchronize with other threads
    }
    if (ensemble_name!=null) {  // remove thread from ensemble barrier
      try {
        ComplexBarrier.removeCurrentThread(ensemble_name);
      }
      catch (Exception e) {
        e.printStackTrace();  // no-op
      }
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


  /* GA methods */


  private void initPopulation() throws OptimizerException, InterruptedException {
    int initpopnum = 10;
    Integer initpopnumI = ((Integer) _p.get("dga.numinitpop"));
    if (initpopnumI!=null) initpopnum = initpopnumI.intValue();
    _individuals = new ArrayList();  // List<DGAIndividual>
    RandomChromosomeMakerIntf amaker = (RandomChromosomeMakerIntf) _p.get("dga.randomchromosomemaker");
    // if no such maker is provided, can only throw exception
    if (amaker==null)
      throw new OptimizerException("DGAThreadAux.initPopulation(): "+
                                   "no RandomChromosomeMakerIntf object "+
                                   "passed in the params");
    for (int i=0; i<initpopnum; i++) {
      Object chromosome = amaker.createRandomChromosome(_fp);  // used to be _p
      DGAIndividual indi = DGAIndividual.newInstance(chromosome, _master, _fp);  // used to be _p
			indi.incrAge();  // itc 2014-10-26: set each individual's age to 1.
      //System.out.println("Individual-"+i+"="+indi);
      _individuals.add(indi);
    }
    // now compute fitnesses of each individual
    computeFitness(-1);
    // finally update incumbent
    DGAIndividual best = null;
    double best_val = Double.MAX_VALUE;
    for (int i=0; i<_individuals.size(); i++) {
      DGAIndividual indi = (DGAIndividual) _individuals.get(i);
      double vi = indi.getValue();
      if (vi<best_val) {
        best = indi;
        best_val = vi;
      }
    }
    _master.setIncumbent(best);  // update master's best soln found if needed
  }


  private synchronized void recvInds() {
    if (_immigrantsPool.size()>0) {
      _individuals.addAll(_immigrantsPool);
      _immigrantsPool.clear();
    }
    // if it's the thread w/ id=0, add any solutions from the obervers
    if (_id==0) {
      _master.transferSolutionsTo(_individuals, _fp);  // used to be _p
    }
  }


  private void sendInds() {
    int sendTo = _master.getImmigrationIsland(_id);
    if (sendTo>=0 && _individuals.size()>0) {  // send immigrants only if population>0
			List immigrants = getImmigrants();
      DGAThreadAux receiverThreadAux = _master.getDGAThread(sendTo).getDGAThreadAux();
      receiverThreadAux.recvIndsAux(immigrants);
    }
    else {
      // synchronize order with null task
      try {
        OrderedBarrier.getInstance("dga."+_master.getId()).orderedBarrier(null);
      }
      catch (ParallelException e) {
        e.printStackTrace();  // cannot reach this point
      }
    }
  }


  // used to be synchronized
  private void recvIndsAux(List immigrants) {
    // guarantee the order in which the immigrants are placed in the _individuals
    // so that all runs with same seed produce identical results
    try {
      OrderedBarrier.getInstance("dga."+_master.getId()).orderedBarrier(new RecvIndTask(
          _immigrantsPool, immigrants));
    }
    catch (ParallelException e) {
      e.printStackTrace();  // cannot reach this point
    }
  }


  private void nextGeneration(int gen) throws OptimizerException {
    // 0. init stuff
    // 1. select pairs of individuals
    // 2. do Xover
    // 3. do mutation
    // 4. compute value and fitness for each
    // 5. update age, remove old and unfit
		final int master_id = _master.getId();
    double xoverprob = 0.7;
    try {
      Double xoverprobD = (Double) _p.get("dga.xoverprob");
      if (xoverprobD != null) xoverprob = xoverprobD.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    int popsize = _individuals.size();
    int poplimit = _maxpopnum;  // pop limit per island
		/*
		try {
      Integer plI = (Integer) _p.get("dga.poplimit");
      if (plI != null) poplimit = plI.intValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    */
		double piesz[] = new double[popsize];
    piesz[0] = ((DGAIndividual) _individuals.get(0)).getFitness();
    double tot_val = piesz[0];
    for (int i=1; i<popsize; i++) {
      piesz[i] = piesz[i-1]+((DGAIndividual) _individuals.get(i)).getFitness();
      tot_val += ((DGAIndividual) _individuals.get(i)).getFitness();
    }
		
		final Random rand = RndUtil.getInstance(_uid).getRandom();  // used to be _id

    for (int i=0; i<popsize; i++) {
      if (poplimit<=_individuals.size()) break;  // no more procreation this generation...
      // Xover probability
      double r = rand.nextDouble();  // used to be _id
      if (r>xoverprob*xoverprob) continue;  // on average, we should run the exp 0.7*0.7*popsize times
      // select two individuals
      // first
      double r1 = rand.nextDouble()*tot_val;
      int parid1=0;
      while (r1>piesz[parid1]) parid1++;
      // second
      double r2 = rand.nextDouble()*tot_val;
      int parid2=0;
      while (r2>piesz[parid2]) parid2++;

      // Xover
      DGAIndividual par1 = (DGAIndividual) _individuals.get(parid1);
      DGAIndividual par2 = (DGAIndividual) _individuals.get(parid2);
      try {
        Pair offspring = _xoverOp.doXover(par1.getChromosome(), par2.getChromosome(), _p);
                                                        // operation may throw if
                                                        // children are infeasible
        //DGAIndividual child1 = new DGAIndividual(offspring.getFirst(), _master, _fp);  // used to be _p
        //DGAIndividual child2 = new DGAIndividual(offspring.getSecond(), _master, _fp);  // used to be _p
        DGAIndividual child1 = null;
        DGAIndividual child2 = null;
        // Mutation
        if (_mutationOp!=null) {  // do we have a mutation operator?
          try {
            //Pair children = _mutationOp.doMutation(child1.getChromosome(),
            //                                       child2.getChromosome(), _p);
            Pair children = _mutationOp.doMutation(offspring.getFirst(),
                                                   offspring.getSecond(), _p);
            child1 = DGAIndividual.newInstance(children.getFirst(), _master, _fp);  // used to be _p
            child2 = DGAIndividual.newInstance(children.getSecond(), _master, _fp);  // used to be _p
          }
          catch (OptimizerException e) {
            e.printStackTrace();
            // no-op: mutation simply not done
						if (child1!=null) child1.release();  // protect against pool leaks
						if (child2!=null) child2.release();  // same as above
            child1 = DGAIndividual.newInstance(offspring.getFirst(), _master, _fp);  // used to be _p
            child2 = DGAIndividual.newInstance(offspring.getSecond(), _master, _fp);  // used to be _p
          }
        }
        else {
          child1 = DGAIndividual.newInstance(offspring.getFirst(), _master, _fp);  // used to be _p
          child2 = DGAIndividual.newInstance(offspring.getSecond(), _master, _fp);  // used to be _p
        }
        _individuals.add(child1);
        _individuals.add(child2);
      }
      catch (OptimizerException e) {
        e.printStackTrace();  // no-op
      }
      catch (IllegalArgumentException e) {  // will happen if the function
                                            // evaluation throws.
        e.printStackTrace();  // no-op
      }
    }
    // values and fitnesses
    double min_val = computeFitness(gen);

    // survival of the fittest
    int cutoffage = 5;
    double varage = 0.9;
    try {
      Integer coaI = (Integer) _p.get("dga.cutoffage");
      if (coaI != null) cutoffage = coaI.intValue();
      Double vaD = (Double) _p.get("dga.varage");
      if (vaD != null) varage = vaD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();  // no-op
    }
		double max_val_so_far = Double.NEGATIVE_INFINITY;
		double val_cutoffpoint = cutoffValue();  // figure out the median of the individuals' fitness values
    int num_removed = 0; 
		for (int j=_individuals.size()-1; j>=0; j--) {
      DGAIndividual indj = (DGAIndividual) _individuals.get(j);
      indj.incrAge();  // increase age
      int agej = indj.getAge();
      double fitj = indj.getFitness();
      double valj = indj.getValue();
      if (valj<=min_val) {  // minimization problem only
        // System.err.println("Thread-id:"+_id+": updating _master w/ inc with val="+valj+" (j="+j+")");
        // _master.setIncumbent(indj);  // update the global incumbent
        // update the global incumbent in thread-order
				DGAIndividual indj_copy = new DGAIndividual(indj);  // cannot submit to
				// master the original individual, as it may be managed by this thread.
				// itc 2014-31-10: change name of MPC from "dga" to "dga."+master_id 
				// to avoid interference issues in case there are two or more threads in
				// a program running simultaneously the minimize(f) method of different
				// DGA objects.
        MsgPassingCoordinator.getInstance("dga."+master_id).sendDataBlocking(_id, indj_copy);
      } else if (valj>max_val_so_far) max_val_so_far = valj;
      double rj = rand.nextGaussian()*varage+cutoffage; 
      double fj = rand.nextDouble();  
      boolean fit_cutting = false;
      // fit_cutting = fj > fitj/max_fit;
      fit_cutting = (fitj<val_cutoffpoint && fj>0.01 &&
                     (fitj<1 || j>1));  // don't kill a homogeneous population just for that
      if (rj<agej || fit_cutting) {
        // remove from the population the indj guy
				//System.err.println("removing ind id="+j+" w/ value="+indj.getValue()+
        //                   " w/ fitness="+fitj+" (cutfit="+val_cutoffpoint+" fj="+fj+") w/ age="+agej+
        //                   " (rj="+rj+"), max_val_so_far="+max_val_so_far);
        _individuals.remove(j);
				indj.release();  // release space from thread-local object pool
				++num_removed;
      }
    }
		// itc 2014-31-10: change name of MPC from "dga" to "dga."+master_id 
		// to avoid interference issues in case there are two or more threads in
		// a program running simultaneously the minimize(f) method of different
		// DGA objects.
    MsgPassingCoordinator.getInstance("dga."+master_id).sendDataBlocking(_id, null);  // signal end of incumbents

    computeFitness(gen);  // final update of fitness
  }


  private double computeFitness(int gen) {
    double tot_val = 0.0;
    double min_val = Double.MAX_VALUE;
    double max_val = Double.NEGATIVE_INFINITY;
    double avg_val = 0.0;
    for (int i=0; i<_individuals.size(); i++) {
      DGAIndividual indi = (DGAIndividual) _individuals.get(i);
      tot_val += indi.getValue();
      if (indi.getValue()>=max_val) max_val = indi.getValue();
      if (indi.getValue()<=min_val) min_val = indi.getValue();
    }
    double diff=max_val-min_val;
    // now compute fitnesses of each individual
    for (int i=0; i<_individuals.size(); i++) {
      DGAIndividual indi = (DGAIndividual) _individuals.get(i);
      if (indi.getValue()==min_val) indi.setFitness(1.0);  // avoid division by zero issues
      else indi.setFitness((max_val-indi.getValue())/diff);  // used to be indi.setFitness(min_val/indi.getValue());
    }
    avg_val = tot_val/_individuals.size();
    Messenger.getInstance().msg("IslandThread-id="+_id+
						                    " (gen="+gen+"): computeFitness(): min_val="+min_val+
						                    " avg_val="+avg_val+" max_val="+max_val,2);
    return min_val;
  }


  private double cutoffValue() {
    final int maxpopnum = 50;  // limit the number of individuals to "fit-cut".
		Object[] arr = _individuals.toArray();
    Arrays.sort(arr, _indComp);
    int vind = arr.length<5 ? 1 : arr.length/2;
    if (arr.length<=1) vind = 0;
    double res = ((DGAIndividual) arr[vind]).getFitness();
    if (vind > maxpopnum) res = ((DGAIndividual) arr[maxpopnum]).getFitness();
    return res;
  }


  private List getImmigrants() {
    ArrayList imms = new ArrayList();
		if (_individuals.size()<2) return imms;  // no immigration when pop too low
    // move two top individuals
    double best_val = Double.MAX_VALUE;
    int best_ind = -1;
    for (int i=0; i<_individuals.size(); i++) {
      DGAIndividual indi = (DGAIndividual) _individuals.get(i);
      double ival = indi.getValue();
      if (ival<best_val) {
        best_ind = i;
        best_val = ival;
      }
    }
    if (best_ind>=0) {
      // imms.add(_individuals.get(best_ind));
			// create a new DGAIndividual to send, as managed ones cannot leave.
			DGAIndividual best = (DGAIndividual) _individuals.get(best_ind);
			DGAIndividual copy_best = new DGAIndividual(best);
			imms.add(copy_best);
      _individuals.remove(best_ind);
			best.release();
    }
    // repeat for second guy, only if there is someone to leave behind in this island
    if (_individuals.size()>1) {
      best_val = Double.MAX_VALUE;
      best_ind = -1;
      for (int i = 0; i < _individuals.size(); i++) {
        DGAIndividual indi = (DGAIndividual) _individuals.get(i);
        double ival = indi.getValue();
        if (ival < best_val) {
          best_ind = i;
          best_val = ival;
        }
      }
			if (best_ind>=0) {
				// imms.add(_individuals.get(best_ind));
				// create a new DGAIndividual to send, as managed ones cannot leave.
				DGAIndividual best = (DGAIndividual) _individuals.get(best_ind);
				DGAIndividual copy_best = new DGAIndividual(best);
				imms.add(copy_best);
				_individuals.remove(best_ind);
				best.release();
			}
    }
    return imms;
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
class DGAIndividual {
	private final static boolean _USE_POOLS=true;  // compile-time flag indicates use of pools or not
  private Object _chromosome;
  private int _age;
  private double _val=Double.MAX_VALUE;  // raw objective value
  private double _fitness;  // normalized value in [0,1]
  // private DGA _master;  // ref. back to master DGA object
  private Chromosome2ArgMakerIntf _c2amaker;
  private FunctionIntf _f;
	// thread-local object pool related data
  private DGAIndividualPool _pool;
  private int _poolPos;
  private boolean _isUsed;
  /*
	private static long _totalNumObjs=0;
	private static long _totalNumObjsDeleted=0;
	*/


  public static DGAIndividual newInstance(Object chromosome, DGA master, Hashtable funcparams) {
    if (_USE_POOLS)
			return DGAIndividualPool.getObject(chromosome, master, funcparams);
		else {  // no pools used
			try {
				return new DGAIndividual(chromosome, master, funcparams);
			}
			catch (OptimizerException e) {
				e.printStackTrace();
				return null;
			}
		}
  }


  public static DGAIndividual newInstance(Object chromosome, double val, double fit, DGA master) {
    if (_USE_POOLS) 
			return DGAIndividualPool.getObject(chromosome, val, fit, master);
		else return new DGAIndividual(chromosome, val, fit, master);  // no pools used
  }


  /**
   * this constructor is to be used only from the DGAIndividualPool for
   * constructing managed objects (re-claimable ones).
   * @param pool
   * @param poolpos
   */
  DGAIndividual(DGAIndividualPool pool, int poolpos) {
    _pool=pool;
    _poolPos=poolpos;
    _isUsed=false;
		// let the other "major" data-members at default values
    /*
    synchronized (DGAIndividual.class) {
      ++_totalNumObjs;
    }
    */
  }


  DGAIndividual(Object chromosome, DGA master, Hashtable funcparams)
      throws OptimizerException {
    _age = 0;
    _chromosome = chromosome;
    // _master = master;
    _c2amaker = (Chromosome2ArgMakerIntf) master.getChromosome2ArgMaker();
    _f = master.getFunction();  // was _master._f; which is also safe
    computeValue(funcparams);  // may throw OptimizerException
    _fitness = 0.0;
		// pool params
    _pool=null;
    _poolPos=-1;
    _isUsed=true;
    /*
    synchronized (DGAIndividual.class) {
      ++_totalNumObjs;
			if (_totalNumObjs>80000) {
				try {
					String str = "DGAIndPool Free Pool Poss="+DGAIndividualThreadLocalPools.getThreadLocalPool().getFreePositions();
					throw new OptimizerException("DGAindividual(chr,master,fp): limit exceeded: tno="+_totalNumObjs+" tnor="+_totalNumObjsDeleted+"\n"+str);
				}
				catch (OptimizerException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
    }
    */
  }


	void setData(Object chromosome, DGA master, Hashtable funcparams) throws OptimizerException {
		if (!_isUsed) {
      Integer null_y=null;
      System.err.println(null_y.intValue());  // force a NullPointerException to debug error
		}
		_age = 0;
    _chromosome = chromosome;
    // _master = master;
    _c2amaker = (Chromosome2ArgMakerIntf) master.getChromosome2ArgMaker();
    _f = master.getFunction();  // was _master._f; which is also safe
    computeValue(funcparams);  // may throw OptimizerException
    _fitness = 0.0;
	}


  DGAIndividual(Object chromosome, double val, double fit, DGA master) {
    _age = 0;
    _chromosome = chromosome;
    _val = val;
    _fitness = fit;
    // _master = master;
    _c2amaker = (Chromosome2ArgMakerIntf) master.getChromosome2ArgMaker();
    _f = master.getFunction();  // was _master._f; which is also safe
		// pool params
    _pool=null;
    _poolPos=-1;
    _isUsed=true;
    /*
    synchronized (DGAIndividual.class) {
      ++_totalNumObjs;
    }
    */
	}


	void setData(Object chromosome, double val, double fit, DGA master) {
		if (!_isUsed) {
      Integer null_y=null;
      System.err.println(null_y.intValue());  // force a NullPointerException to debug error
		}
		_age = 0;
    _chromosome = chromosome;
    _val = val;
    _fitness = fit;
    // _master = master;
    _c2amaker = (Chromosome2ArgMakerIntf) master.getChromosome2ArgMaker();
    _f = master.getFunction();  // was _master._f; which is also safe
	}


	DGAIndividual(DGAIndividual other) {
		_age = other._age;
		_chromosome = (other._chromosome instanceof PoolableObjectIntf) ?
						((PoolableObjectIntf) other._chromosome).cloneObject() :
						other._chromosome;
		_val = other._val;
		_fitness = other._fitness;
		_c2amaker = other._c2amaker;
		_f = other._f;
		// itc 2014-23-10: added below settings
		// pool params
    _pool=null;
    _poolPos=-1;
    _isUsed=true;
    /*
    synchronized (DGAIndividual.class) {
      ++_totalNumObjs;
    }
    */
	}


  /**
   * indicate item is available for re-use by Object-Pool to which it belongs,
   * and resets its "major" data IFF it is a managed object.
   */
  public void release() {
		/*
		synchronized (DGAIndividual.class) {
			++_totalNumObjsDeleted;
		}
		*/
    if (_pool!=null) {
      _isUsed=false;
			if (_chromosome instanceof PoolableObjectIntf) {  // release
				((PoolableObjectIntf) _chromosome).release();
			}
      _chromosome = null;
      _age=0;
			_val = Double.MAX_VALUE;
			_fitness = 0.0;
			_c2amaker = null;
			_f = null;
      _pool.returnObjectToPool(this);
    }
  }


  /**
   * return true IFF the object is managed and "currently used", or un-managed.
   * @return boolean
   */
  boolean isUsed() {
    return _isUsed;
  }


  /*
  public static synchronized long getTotalNumObjs() {
    return _totalNumObjs;
  }
	public static synchronized long getTotalNumObjsDeleted() {
		return _totalNumObjsDeleted;
	}
  */


  void setIsUsed() {
    _isUsed=true;
  }


  int getPoolPos() {
    return _poolPos;
  }


  public String toString() {
    String r = "Chromosome=[";
    r += _chromosome.toString();
    r += "] val="+_val+" fitness="+_fitness+" age="+_age;
    return r;
  }
  Object getChromosome() { return _chromosome; }
  int getAge() { return _age; }
  void incrAge() { _age++; }
  double getValue() { return _val; }  // enhance the density value differences
  double getFitness() { return _fitness; }
  void setFitness(double f) { _fitness = f; }
  private void computeValue(Hashtable params) throws OptimizerException {
    if (_val==Double.MAX_VALUE) {  // don't do the computation if already done
      Object arg = null;
      if (_c2amaker == null) arg = _chromosome;
      else arg = _c2amaker.getArg(_chromosome, params);
      _val = _f.eval(arg, params);
    }
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
class IndComp implements Comparator, java.io.Serializable {
  private final static long serialVersionUID = -5696973479537608714L;

  public int compare(Object i1, Object i2) {
    DGAIndividual ind1 = (DGAIndividual) i1;
    DGAIndividual ind2 = (DGAIndividual) i2;
    if (ind1.getValue()>ind2.getValue()) return -1;
    else if (ind1.getValue()<ind2.getValue()) return 1;
    else return 0;
  }
}


// thread-local pools mechanism classes below


/**
 * The class is responsible for maintaining a sufficiently large array of
 * DGAIndividual objects, and there will be a pool for each thread of execution.
 * In this way, there will be no need for invoking the new operator within the
 * running threads requiring such objects, unless a thread at some
 * point runs out of space in its thread-local pool.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DGAIndividualPool {
  /**
   * the maximum number of objects this pool can handle.
   */
  private static final int _NUMOBJS = 10000;
  private ArrayList _pool;
  private int _maxUsedPos=-1;
	private int _minUsedPos=_NUMOBJS;
	/**
	 * compile-time constant used to detect if an object is released by a thread
	 * other than the one used in "creating" it, and throwing an unchecked
	 * NullPointerException.
	 */
	private static final boolean _DO_RELEASE_SANITY_TEST=true;
	/**
	 * compile-time constant used to detect if an object gotten by a thread
	 * is already used, and throwing an unchecked NullPointerException.
	 */
	private static final boolean _DO_GET_SANITY_TEST=true;


  /**
   * sole constructor. Creates <CODE>_NUMOBJS</CODE> "empty"
   * DGAIndividual objects.
   */
  DGAIndividualPool() {
    _pool = new ArrayList(_NUMOBJS);
		/* itc: 2015-01-15: moved code below to initialize() method
    for (int i=0; i<_NUMOBJS; i++) {
      _pool.add(new DGAIndividual(this, i));
    }
		*/
  }


	/**
   * factory object creation method, first tries to return a managed object from
   * the pool, and if it cannot find one, creates a new (unmanaged) one. The
   * returned object, always has its data members (values) correctly set.
	 * @param chromosome
	 * @param master
	 * @param funcparams
	 * @return DGAIndividual
	 */
  static DGAIndividual getObject(Object chromosome, DGA master, Hashtable funcparams) {
    DGAIndividualPool pool = DGAIndividualThreadLocalPools.getThreadLocalPool();
    DGAIndividual p = pool.getObjectFromPool();
    try {
			if (p!=null) {  // ok, return managed object
		    p.setData(chromosome, master, funcparams);
			  return p;
			} else  // oops, create new unmanaged object
				return new DGAIndividual(chromosome, master, funcparams);
		}
		catch (OptimizerException e) {
			e.printStackTrace();
			return null;
		}		
  }


	/**
   * factory object creation method, first tries to return a managed object from
   * the pool, and if it cannot find one, creates a new (unmanaged) one. The
   * returned object, always has its data members (values) correctly set.
	 * @param chromosome
	 * @param val
	 * @param fit
	 * @param master
	 * @return DGAIndividual
	 */
  static DGAIndividual getObject(Object chromosome, double val, double fit, DGA master) {
    DGAIndividualPool pool = DGAIndividualThreadLocalPools.getThreadLocalPool();
    DGAIndividual p = pool.getObjectFromPool();
    if (p!=null) {  // ok, return managed object
      p.setData(chromosome, val, fit, master);
      return p;
    } else  // oops, create new unmanaged object
      return new DGAIndividual(chromosome, val, fit, master);
  }

	
	/**
	 * method is only once called from <CODE>DGAIndividualThreadLocalPools</CODE>
	 * right after this object is constructed, so as to avoid escaping "this" in
	 * the constructor.
	 */
	void initialize() {
    for (int i=0; i<_NUMOBJS; i++) {
      _pool.add(new DGAIndividual(this, i));
    }		
	}
	

  /**
   * return an managed "free" object from the pool, or null if it cannot find
   * one. Tries from the right, then from the left end of the "used part" of the
	 * pool.
   * @return DGAIndividual
   */
  DGAIndividual getObjectFromPool() {
    if (_maxUsedPos<_NUMOBJS-1) {
      _maxUsedPos++;
      DGAIndividual ind = (DGAIndividual) _pool.get(_maxUsedPos);
				if (_DO_GET_SANITY_TEST && ind.isUsed()) {
					Integer yI = null;
					System.err.println("getObjectFromPool(): right doesn't work: null ref yI="+yI.intValue());  // force NullPointerException
				}
      ind.setIsUsed();
			if (_minUsedPos>_maxUsedPos) _minUsedPos = _maxUsedPos;
      return ind;
    } else {  // try the left end
			if (_minUsedPos>0) {
				_minUsedPos--;
				DGAIndividual ind = (DGAIndividual) _pool.get(_minUsedPos);
				if (_DO_GET_SANITY_TEST && ind.isUsed()) {
					Integer yI = null;
					System.err.println("getObjectFromPool(): left doesn't work: null ref yI="+yI.intValue());  // force NullPointerException
				}
				ind.setIsUsed();
				if (_minUsedPos>_maxUsedPos) _maxUsedPos = _minUsedPos;
				return ind;
			}
		}
    return null;  // catch all clause: didn't find a free position
  }


  void returnObjectToPool(DGAIndividual ind) {
		if (_DO_RELEASE_SANITY_TEST) {
			DGAIndividualPool pool = DGAIndividualThreadLocalPools.getThreadLocalPool();
			if (pool!=this) {
				Integer yI = null;
				System.err.println("null ref yI="+yI.intValue());  // force NullPointerException
			}
		}
		// corner case: the returned object was the only one "out-of-the-pool"
		if (_maxUsedPos==_minUsedPos) {
			if (_DO_RELEASE_SANITY_TEST) {
				if (ind.getPoolPos()!=_minUsedPos) {
					Integer yI = null;
					System.err.println("null ref yI="+yI.intValue());  // force NullPointerException					
				}
			}
			_maxUsedPos = -1;
			_minUsedPos = _NUMOBJS;
			return;
		}
    if (ind.getPoolPos()==_maxUsedPos) {
      --_maxUsedPos;
      while (_maxUsedPos>=0 &&
             ((DGAIndividual)_pool.get(_maxUsedPos)).isUsed()==false)
        --_maxUsedPos;
    }
		if (ind.getPoolPos()==_minUsedPos) {
			++_minUsedPos;
			while (_minUsedPos<_NUMOBJS &&
						 ((DGAIndividual)_pool.get(_minUsedPos)).isUsed()==false)
				++_minUsedPos;
		}
    return;
  }
	
	
	/**
	 * method used for debugging purposes only.
	 * @return String
	 */
	String getFreePositions() { 
		int num_used = 0;
		int num_free = 0;
		String res="";
		int i=0;
		boolean first_time=true;
		boolean found_free;
		while (i<_NUMOBJS) {
			found_free=false;
			// skip used positions
			while (((DGAIndividual)_pool.get(i)).isUsed()) {
				if (i<_NUMOBJS-1) {
					++i;
					++num_used;  // don't count here the last element in the pool
				}
				else break;
			}
			if (!((DGAIndividual)_pool.get(i)).isUsed()) {  // found start of free objs
				found_free = true;
				if (!first_time) res += ",";
				else first_time=false;
				res += "["+i+",";
			}  // found free pos
			for (; i<_NUMOBJS; i++) {  // move over free positions
				if (((DGAIndividual)_pool.get(i)).isUsed()) {
					++num_used;
					break;
				}
				else ++num_free;
			}
			if (found_free) res += (i-1)+"]";
			++i;
		}
		if (res.length()==0) res = "[]";
		return res+" num_used="+num_used+" num_free="+num_free;
  }

}


/**
 * auxiliary class using the <CODE>ThreadLocal</CODE> mechanism to create
 * thread-local DGAIndividual object pools. Essentially, the three classes
 * <CODE>DGAIndividual, DGAIndividualPool, DGAIndividualThreadLocalPools</CODE>
 * implement a pattern that this author termed "The Thread-Local Object-Pool
 * Design Pattern" that can result in dramatic speedups in run-time especially
 * when an application would need to create big numbers of DGAIndividual objects
 * simultaneously from many concurrent threads. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DGAIndividualThreadLocalPools {
  private static ThreadLocal _pools = new ThreadLocal() {
    protected Object initialValue() {
      return null;
    }
  };

  static DGAIndividualPool getThreadLocalPool() {
    DGAIndividualPool p = (DGAIndividualPool) _pools.get();
    if (p==null) {
      p = new DGAIndividualPool();
			p.initialize();
      _pools.set(p);
    }
    return p;
  }

}

