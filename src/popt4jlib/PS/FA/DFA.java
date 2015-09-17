package popt4jlib.PS.FA;

import java.util.*;
import parallel.*;
import utils.*;
import popt4jlib.*;

/**
 * A parallel implementation of the Firefly Algorithm for
 * function optimization, implementing also the SubjectIntf and ObserverIntf
 * objects by extending GLockingObservableObserverBase so that it
 * may be combined with other optimization processes and produce better results.
 * By implementing the SubjectIntf of the well-known Observer Design Pattern,
 * the class allows any observer objects implementing the ObserverIntf to be
 * notified whenever a new incumbent is found and then to enrich the class's
 * population (in island thread with id=0) by adding back into the first island'
 * population new (hopefully better) solutions. The solutions moved back and
 * forth between optimizers have to be moved as function argument objects and
 * not as chromosome objects. However, the Subject/Observer interactions destroy
 * the otherwise deterministic order & results of the DFA execution due to the
 * fact that the order in which DFAThreads call the setIncumbent() method which
 * in turn may call the notifyObservers() method is not deterministically
 * fixed (depends on thread schedules).
 *
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final public class DFA extends GLockingObservableObserverBase implements OptimizerIntf {
  private static int _nextId=0;
  private int _id;
  private HashMap _params;
  double _incValue=Double.MAX_VALUE;
  Object _inc;  // incumbent chromosome
  FunctionIntf _f;

  private DFAThread[] _threads=null;
  private int[] _islandsPop;


  /**
   * public no-arg constructor
   */
  public DFA() {
		super();
		_id = incrID();
  }


  /**
   * public constructor accepting the optimization parameters (making a local
   * copy of them).
   * @param params HashMap
   */
  public DFA(HashMap params) {
    this();
    try {
      setParams(params);
    }
    catch (Exception e) {
      // no-op: cannot reach this point
    }
  }


  /**
   * returns a copy of the parameters of this DFA object.
   * @return HashMap
   */
  public synchronized HashMap getParams() {
    return new HashMap(_params);
  }


  /**
   * the optimization params are set to p. The method will throw if it is
   * invoked while another thread is running the minimize(f) method on the
   * same DPSO object.
   * @param p HashMap the parameters to pass-in
   * @throws OptimizerException if another thread is concurrently running the
   * <CODE>minimize(f)</CODE> method of this object.
   */
  public synchronized void setParams(HashMap p) throws OptimizerException {
    if (_f!=null) throw new OptimizerException("cannot modify parameters while running");
    _params = null;
    _params = new HashMap(p);  // own the params
  }


  /**
   * returns the currently best known function argument that  minimizes the _f
   * function. The ObserverIntf objects would need this method to get the
   * current incumbent (and use it as they please). Note: even though the
   * method is not synchronized, it still executes atomically, and is in synch
   * with its counterpart, setIncumbent().
   * @return Object
   */
  protected Object getIncumbentProtected() {
    try {
      Chromosome2ArgMakerIntf c2amaker =
          (Chromosome2ArgMakerIntf) _params.get("dfa.c2amaker");
      Object arg = null;
      try {
        if (c2amaker == null) arg = _inc;
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
  }
  /**
   * returns the current function that is being minimized (may be null)
   * @return FunctionIntf
   */
  public synchronized FunctionIntf getFunction() {
    return _f;
  }


  /**
   * the most important method of the class. Some parameters must have been
   * previously passed in the _params map (via the ctor arg or via a call to
   * setParams(p) to do that).
   * These parameters are:
   * <ul>
   * <li> &lt;"dfa.randomfireflymaker",RandomChromosomeMakerIntf maker&gt; mandatory,
   * the RandomChromosomeMakerIntf Object responsible for creating valid random
   * chromosome Objects to populate the firefly-islands.
   * <li> &lt;"dfa.cupdater",ChromosomeUpdaterIntf updater&gt; mandatory, an Object
   * that implements the ChromosomeUpdaterIntf that produces the next position
   * of a particle given its current position and that of its better neighbors.
   * <li> &lt;"dfa.numthreads",Integer nt&gt; optional, how many threads will be used,
   * default is 1. Each thread corresponds to an island in the DGA model.
   * <li> &lt;"dfa.c2amaker",Chromosome2ArgMakerIntf c2a&gt; optional, the object that is
   * responsible for tranforming a chromosome Object to a function argument
   * Object. If not present, the default identity transformation is assumed.
   * <li> &lt;"dfa.a2cmaker",Arg2ChromosomeMakerIntf a2c&gt; optional, the object that is
   * responsible for transforming a FunctionIntf argument Object to a chromosome
   * Object. If not present, the default identity transformation is assumed. The
   * a2c object is only useful when other ObserverIntf objects register for this
   * SubjectIntf object and also add back solutions to it (as FunctionIntf args)
   * <li> &lt;"dfa.numgens",Integer ng&gt; optional, the number of generations to run the
   * DFA, default is 1.
   * <li> &lt;"dfa.immprob",Double prob&gt; optional, the probability with which an sub-
   * population will send some of its members to migrate to another (island)
   * sub-population, default is 0.01
   * <li> &lt;"dfa.numinitpop",Integer ip&gt; optional, the initial population number for
   * each island, default is 10.
   * <li> &lt;"ensemblename", String name&gt; optional, the name of the synchronized
   * optimization ensemble in which this DFA object will participate. In case
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
   * <pre>
	 * <CODE>
   * Barrier.removeInstance(name+"_master");
   * Barrier.setNumThreads(name+"_master", numOptimizers);
   * ComplexBarrier.removeInstance(name);
   * </CODE>
	 * </pre>
   * series of calls.
   * </ul>
   * @param FunctionIntf f
   * @throws OptimizerException if the process fails
   * @return PairObjDouble // Pair&lt;Object arg, Double val&gt;
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("DFA.minimize(f): null f");
    try {
      // atomic.start(Constants.OB);  // no longer needed to protect OrderedBarrier
      synchronized (this) {
        if (_f != null)throw new OptimizerException("DFA.minimize(): "+
          "another thread is concurrently executing the method on the same object");
        _f = f;
      }
      // add the function itself onto the parameters hashtable for possible use
      // by operators. The function has to be reentrant in any case for multiple
      // concurrent evaluations to be possible in the first place so there is no
      // new issue raised here
      if (_params.get("dfa.function") == null)
        _params.put("dfa.function", f);
      int nt = 1;
      Integer ntI = (Integer) _params.get("dfa.numthreads");
      if (ntI != null) nt = ntI.intValue();
      if (nt < 1)throw new OptimizerException(
          "DFA.minimize(): invalid number of threads specified");
      RndUtil.addExtraInstances(nt);  // not needed
      // check if this object will participate in an ensemble
      String ensemble_name = (String) _params.get("ensemblename");
      _threads = new DFAThread[nt];
      _islandsPop = new int[nt];
      for (int i = 0; i < nt; i++) _islandsPop[i] = 0; // init.
      // the _XXX data members are correctly protected since this method executes
      // atomically, and the working threads are started below. FindBugs
      // complains unjustly here...
      try {
        parallel.Barrier.setNumThreads("dfa." + _id, nt); // init the Barrier obj.
      }
      catch (parallel.ParallelException e) {
        e.printStackTrace();
        throw new OptimizerException("barrier init. failed");
      }

      for (int i = 0; i < nt; i++) {
        _threads[i] = new DFAThread(this, i);
        OrderedBarrier.addThread(_threads[i], "dfa."+_id); // for use in synchronizing/ordering
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
          utils.Messenger.getInstance().msg("this DFA.minimize(f) method must "+
                                            "be invoked from within the minimize(f)"+
                                            " method of an ensemble optimizer "+
                                            "that has properly called the "+
                                            "Barrier.setNumThreads(<name>_master,<num>)"+
                                            " method.",0);
          throw new OptimizerException("DFA.minimize(f): ensemble broken");
        }
      }
      for (int i = 0; i < nt; i++) {
        _threads[i].start();
      }
      // the following is equivalent to for-each thread { thread.join(); }
      for (int i = 0; i < nt; i++) {
        DFAThreadAux rti = _threads[i].getDFAThreadAux();
        rti.waitForTask();
      }

      // done
      synchronized (this) {
        Chromosome2ArgMakerIntf c2amaker = (Chromosome2ArgMakerIntf) _params.
            get("dfa.c2amaker");
        Object arg = _inc;
        if (c2amaker != null) arg = c2amaker.getArg(_inc, _params);
        return new PairObjDouble(arg, _incValue);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("DFA.minimize() failed");
    }
    finally {
      reset();
      // atomic.end(Constants.OB);  // no longer needed to protect OrderedBarrier
    }
  }


  /**
   * reset the barrier and other objects used in the process.
   */
  private synchronized void reset() {
    try {
      OrderedBarrier.getInstance("dfa."+_id).reset();
      OrderedBarrier.removeInstance("dfa."+_id);
      Barrier.removeInstance("dfa."+_id);
      _f = null;
      _inc = null;
      _incValue = Double.MAX_VALUE;
    }
    catch (ParallelException e) {
      e.printStackTrace();
    }
  }


  /**
   * return the immigration island where the island with id myid must send its
   * individuals.
   * @param myid int
   * @return int
   */
  synchronized int getImmigrationIsland(int myid) {
    for (int i=0; i<_islandsPop.length; i++)
      if (myid!=i && (_islandsPop[i]==0 || _islandsPop[myid]>2.5*_islandsPop[i])) return i;
    // populations are more or less the same size, so immigration will occur
    // with some small probability to an immediate neighbor
    double immprob = 0.01;
    try {
      Double ipD = (Double) _params.get("dfa.immprob");
      if (ipD!=null && ipD.doubleValue()>=0) immprob = ipD.doubleValue();
    }
    catch (Exception e) {
      e.printStackTrace();  // no-op
    }
    if (_islandsPop.length>1) {
      double r = RndUtil.getInstance(myid).getRandom().nextDouble();
      if (r < immprob) {
        if (myid > 0) return myid - 1;
        else return _islandsPop.length-1;
      }
    }
    return -1;
  }


  /**
   * sets the populations of island with the given id to the size given.
   * @param id int
   * @param size int
   */
  synchronized void setIslandPop(int id, int size) {
    _islandsPop[id] = size;
  }


  /**
   * get the DFAThread with the given id.
   * @param id int
   * @return DFAThread
   */
  synchronized DFAThread getDFAThread(int id) {
    return _threads[id];
  }


  /**
   * get the current incumbent value.
   * @return double
   */
  synchronized double getIncValue() {
    return _incValue;
  }


  /**
   * update if we have an incumbent.
   * @param ind DFAIndividual
   * @throws OptimizerException in case of insanity (may only happen if the
   * function to be minimized is not reentrant and the debug bit
   * <CODE>Constants.DFA</CODE> is set in the <CODE>Debug</CODE> class)
   */
  void setIncumbent(DFAIndividual ind) throws OptimizerException {
    // method used to be synchronized but doesn't need to be
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      if (_incValue > ind.getValue()) {  // minimization problems only
        System.err.println("Updating Incumbent w/ val=" + ind.getValue());
        if (Debug.debug(Constants.DFA) != 0) {
          // sanity check
          Object arg = ind.getChromosome(); // assume the chromosome Object is
          // the same used for function evals.
          Chromosome2ArgMakerIntf c2amaker = (Chromosome2ArgMakerIntf) _params.
                                               get("dfa.c2amaker");
          if (c2amaker != null) // oops, no it wasn't
            arg = c2amaker.getArg(ind.getChromosome(), _params);
          double incval = _f.eval(arg, _params);
          if (Math.abs(incval - ind.getValue()) > 1.e-25) {
            Messenger.getInstance().msg("DFA.setIncumbent(): ind-val=" +
                                        ind.getValue() + " fval=" + incval +
                                        " ???", 0);
            throw new OptimizerException(
                "DFA.setIncumbent(): insanity detected; " +
                "most likely evaluation function is " +
                "NOT reentrant... " +
                "Add the 'function.notreentrant,num'" +
                " pair (num=1 or 2) to run parameters");
          }
          // end sanity check
        }
        _incValue = ind.getValue();
        _inc = ind.getChromosome();
        notifyObservers(); // notify any observers listening
      }
    }
    catch (ParallelException e) {
      e.printStackTrace();
      throw new OptimizerException("DFA.setIncumbent(): double lock somehow failed...");
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
   * moves all chromosomes that the observers and subjects have added so far to
   * _individuals Vector&lt;DFAIndividual&gt; of the DFAThread with id 0, and clears
   * the solutions from the _observers and _subjects map.
   * @param tinds Vector
   * @param params HashMap the optimization params
   * @param funcParams the function parameters
   */
  synchronized void transferSolutionsTo(Vector tinds, HashMap params, HashMap funcParams) {
    // 1. observers
    int ocnt = 0;
    Iterator it = getObservers().keySet().iterator();
    while (it.hasNext()) {
      ObserverIntf obs = (ObserverIntf) it.next();
      Vector sols = (Vector) getObservers().get(obs);
      int solssz = sols.size();
      for (int i=0; i<solssz; i++) {
        try {
          Object chromosomei = null;
          Arg2ChromosomeMakerIntf a2cmaker = (Arg2ChromosomeMakerIntf) params.get("dfa.a2cmaker");
          if (a2cmaker != null)
            chromosomei = a2cmaker.getChromosome(sols.elementAt(i), params);
          else chromosomei = sols.elementAt(i); // assume chromosome and arg are the same
          DFAIndividual indi = new DFAIndividual(chromosomei, this, params, funcParams);
          tinds.addElement(indi);
          ++ocnt;
        }
        catch (OptimizerException e) {
          e.printStackTrace();  // report failure to create individual out of the
                                // provided chromosome Object
        }
      }
      sols.clear();
    }
    // 2. subjects
    int scnt = 0;
    it = getSubjects().keySet().iterator();
    while (it.hasNext()) {
      SubjectIntf subject = (SubjectIntf) it.next();
      Vector sols = (Vector) getSubjects().get(subject);
      int solssz = sols.size();
      for (int i=0; i<solssz; i++) {
        try {
          Object chromosomei = null;
          Arg2ChromosomeMakerIntf a2cmaker = (Arg2ChromosomeMakerIntf) params.get("dfa.a2cmaker");
          if (a2cmaker != null)
            chromosomei = a2cmaker.getChromosome(sols.elementAt(i), params);
          else chromosomei = sols.elementAt(i); // assume chromosome and arg are the same
          DFAIndividual indi = new DFAIndividual(chromosomei, this, params, funcParams);
          tinds.addElement(indi);
          ++scnt;
        }
        catch (OptimizerException e) {
          e.printStackTrace();  // report failure to create individual out of the
                                // provided chromosome Object
        }
      }
      sols.clear();
    }
    Messenger.getInstance().msg("DFA.transfer(): totally "+
                                ocnt+" sols from observers and "+
                                scnt+" sols from subjects transferred",2);
  }


  /**
   * get the id of this object.
   * @return int
   */
  synchronized int getId() { return _id; }


  /**
   * auxiliary method providing unique ids for objects of this class
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
 * <p>Copyright: Copyright (c) 2011-2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DFAThread extends Thread {
  private DFAThreadAux _aux;


  public DFAThread(DFA master, int id) throws OptimizerException {
    _aux = new DFAThreadAux(master, id);
  }


  public DFAThreadAux getDFAThreadAux() {
    return _aux;
  }


  public void run() {
    _aux.runTask();
  }


  // int getTId() { return _aux.getId(); }
}


/**
 * auxiliary class not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DFAThreadAux {
  private int _id;
  private int _uid;
  private DFA _master;
  private HashMap _p;
  private HashMap _fp;
  private boolean _finish = false;
  private Vector _individuals;  // Vector<Individual>
  private Vector _immigrantsPool;  // Vector<Individual>
  private double _inc=Double.MAX_VALUE;  // best island value
  private Object _incarg=null;  // the best position for the island-swarm


  public DFAThreadAux(DFA master, int id) throws OptimizerException {
    _master = master;
    _id = id;
    _uid = (int) DataMgr.getUniqueId();
    _p = _master.getParams();  // returns a copy
    _p.put("thread.localid", new Integer(_id));
    _p.put("thread.id",new Integer(_uid));  // used to be _id
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
              packname.startsWith("parallel"))continue; // don't include such objects
        }
        else {
          Messenger.getInstance().msg("no package info for object with key "+key,2);
        }
        _fp.put(key,val);
      }
    }
    // end creating _funcParams
    _immigrantsPool = new Vector();
  }


  public void runTask() {
    // start: do the DFA
    try {
      initPopulation();
    }
    catch (Exception e) {
      e.printStackTrace();  // no-op
    }
    //System.err.println("initPopulation() done.");
    int numgens = 1;
    Integer ngI = (Integer) _p.get("dfa.numgens");
    if (ngI!=null) numgens = ngI.intValue();
    String ensemble_name=null;
    try {
      ensemble_name = (String) _p.get("ensemblename");
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    ChromosomeUpdaterIntf cupdater = (ChromosomeUpdaterIntf) _p.get("dfa.cupdater");
    for (int gen = 0; gen < numgens; gen++) {
      Barrier.getInstance("dfa."+_master.getId()).barrier();  // synchronize with other threads
      try {
        if (ensemble_name!=null)
          ComplexBarrier.getInstance(ensemble_name).barrier(); // synchronize with other optimizers' threads
      }
      catch (ParallelException e) {
        e.printStackTrace();  // no-op
      }
      //System.err.println("Island-Thread id=" + _id + " running gen=" + gen +
      //                   " popsize=" + _individuals.size());
      recvInds();
      if (_individuals.size()>0) {
        try {
          nextGeneration();
        }
        catch (Exception e) {
          e.printStackTrace();  // no-op
        }
      }
      sendInds();
      _master.setIslandPop(_id, _individuals.size());
      Barrier.getInstance("dfa."+_master.getId()).barrier();  // synchronize with other threads
      if (_id==0) cupdater.incrementGeneration();  // the 1st thread increases the generation count in the updater object
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


  int getId() { return _id; }


  /* DFA-GA methods */


  private void initPopulation() throws OptimizerException, InterruptedException {
    int initpopnum = 10;
    Integer initpopnumI = ((Integer) _p.get("dfa.numinitpop"));
    if (initpopnumI!=null) initpopnum = initpopnumI.intValue();
    _individuals = new Vector();  // Vector<DPSOIndividual>
    RandomChromosomeMakerIntf amaker = (RandomChromosomeMakerIntf) _p.get("dfa.randomfireflymaker");
    // if no such makers are provided, can only throw exception
    for (int i=0; i<initpopnum; i++) {
      Object chromosome = amaker.createRandomChromosome(_p);
      DFAIndividual indi = new DFAIndividual(chromosome, _master, _p, _fp);
      //System.out.println("Individual-"+i+"="+indi);
      _individuals.addElement(indi);
    }
    // finally update island's incumbent
    DFAIndividual best = null;
    for (int i=0; i<_individuals.size(); i++) {
      DFAIndividual indi = (DFAIndividual) _individuals.elementAt(i);
      double vi = indi.getValue();
      if (vi<_inc) {
        _incarg = indi;
        _inc = vi;
        best = indi;
      }
    }
    if (best!=null) _master.setIncumbent(best);  // update master's best soln found if needed
  }


  private synchronized void recvInds() {
    if (_immigrantsPool.size()>0) {
      _individuals.addAll(_immigrantsPool);
      // update island's best
      for (int i=0; i<_immigrantsPool.size(); i++) {
        try {
          DFAIndividual indi = (DFAIndividual) _immigrantsPool.elementAt(i);
          Object bpi = indi.getBestChromosome();
          double vali = indi.getBestChromosomeValue();
          if (vali < _inc) {
            _inc = indi.getValue();
            _incarg = bpi;
          }
        }
        catch (Exception e) {
          e.printStackTrace();  // no-op
        }
      }
      _immigrantsPool.clear();
    }
    // if it's the thread w/ id=0, add any solutions from the obervers
    if (_id==0) {  // used to be ((DPSOThread) Thread.currentThread()).getId()==0
      _master.transferSolutionsTo(_individuals, _p, _fp);
    }
  }


  private void sendInds() {
    int sendTo = _master.getImmigrationIsland(_id);
    if (sendTo>=0) {
      Vector immigrants = getImmigrants();
      DFAThreadAux receiverThreadAux = _master.getDFAThread(sendTo).getDFAThreadAux();
      receiverThreadAux.recvIndsAux(immigrants);
    }
    else {
      // synchronize order with null task
      try {
        OrderedBarrier.getInstance("dfa."+_master.getId()).orderedBarrier(null);
      }
      catch (ParallelException e) {
        e.printStackTrace();  // cannot reach this point
      }
    }
  }


  // used to be synchronized
  private void recvIndsAux(Vector immigrants) {
    // guarantee the order in which the immigrants are placed in the _individuals
    // so that all runs with same seed produce identical results
    try {
      OrderedBarrier.getInstance("dfa."+_master.getId()).orderedBarrier(new popt4jlib.PS.RecvIndTask(
          _immigrantsPool, immigrants));
    }
    catch (ParallelException e) {
      e.printStackTrace();  // cannot reach this point
    }
    // _immigrantsPool.addAll(immigrants);
  }


  /**
   * the essence of the class. This method specifies how the next generation of
   * individuals is built from the current generation.
   * @throws OptimizerException
   */
  private void nextGeneration() throws OptimizerException {
    // update individuals' position (chromosome)
    ChromosomeUpdaterIntf cupdater = (ChromosomeUpdaterIntf) _p.get("dfa.cupdater");
    for (int i=0; i<_individuals.size(); i++) {
      DFAIndividual indi = (DFAIndividual) _individuals.elementAt(i);
      double fi = indi.getValue();
      for (int j = i+1; j < _individuals.size(); j++) {
        DFAIndividual indj = (DFAIndividual) _individuals.elementAt(j);
        double fj = indj.getValue();
        if (fj < fi) { // move firefly-i towards firefly-j
          Object newchromosomei = cupdater.update(indi.getChromosome(),
                                                  indj.getChromosome(),
                                                  _p);
          try {
            indi.setValues(newchromosomei, _p, _fp);
            // update island and total best
            if (indi.getValue() < _inc) {
              _inc = indi.getValue();
              _incarg = indi.getChromosome();
              _master.setIncumbent(indi);
            }
          }
          catch (Exception e) {
            e.printStackTrace(); // no-op
          }
        }
      }
    }
  }


  private Vector getImmigrants() {
    Vector imms = new Vector();
    if (_individuals.size()<2) return imms;  // no immigration when pop too low
    // move two top individuals
    double best_val = Double.MAX_VALUE;
    int best_ind = -1;
    for (int i=0; i<_individuals.size(); i++) {
      DFAIndividual indi = (DFAIndividual) _individuals.elementAt(i);
      double ival = indi.getValue();
      if (ival<best_val) {
        best_ind = i;
        best_val = ival;
      }
    }
    if (best_ind>=0) {
      imms.add(_individuals.elementAt(best_ind));
      _individuals.remove(best_ind);
    }
    // repeat for second guy, only if there is someone to leave behind in this island
    if (_individuals.size()>1) {
      best_val = Double.MAX_VALUE;
      best_ind = -1;
      for (int i = 0; i < _individuals.size(); i++) {
        DFAIndividual indi = (DFAIndividual) _individuals.elementAt(i);
        double ival = indi.getValue();
        if (ival < best_val) {
          best_ind = i;
          best_val = ival;
        }
      }
      imms.add(_individuals.elementAt(best_ind));
      _individuals.remove(best_ind);
    }
    return imms;
  }

}


/**
 * auxiliary class not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DFAIndividual {
  private Object _x;  // current position
  private Object _pb;  // best known position
  private double _pbval = Double.MAX_VALUE;
  private double _val=Double.MAX_VALUE;  // raw objective value
  private FunctionIntf _f=null;
  private DFA _master;  // ref. back to master DFA object


  public DFAIndividual(Object chromosome, DFA master,
                        HashMap params, HashMap funcparams)
      throws OptimizerException {
    _x = chromosome;
    _pb = _x;  // initial best position
    _master = master;
    _f = _master.getFunction();
    computeValue(params, funcparams);  // may throw OptimizerException
    _pbval = _val;
  }


  public String toString() {
    String r = "Chromosome=[";
    r += _x.toString();
    r += "] val="+_val+"] BestKnown=["+_pb.toString()+"]";
    return r;
  }
  public Object getChromosome() { return _x; }
  public Object getBestChromosome() { return _pb; }
  public double getBestChromosomeValue() { return _pbval; }
  public double getValue() { return _val; }


  void setValues(Object chromosome, HashMap params, HashMap funcParams)
    throws OptimizerException {
    _x = chromosome;
    Chromosome2ArgMakerIntf c2amaker =
        (Chromosome2ArgMakerIntf) params.get("dfa.c2amaker");
    Object arg = null;
    if (c2amaker == null) arg = _x;
    else arg = c2amaker.getArg(_x, params);
    _val = _f.eval(arg, funcParams);  // was _master._f which is also safe
    // update _pb & _pbval
    if (_val < _pbval) {
      _pb = chromosome;
      _pbval = _val;
    }
  }
  private void computeValue(HashMap params, HashMap funcParams)
      throws OptimizerException {
    if (_val==Double.MAX_VALUE) {  // don't do the computation if already done
      Chromosome2ArgMakerIntf c2amaker =
          (Chromosome2ArgMakerIntf) params.get("dfa.c2amaker");
      Object arg = null;
      if (c2amaker == null) arg = _x;
      else arg = c2amaker.getArg(_x, params);
      _val = _f.eval(arg, funcParams);  // was _master._f which is also safe
    }
  }
}

