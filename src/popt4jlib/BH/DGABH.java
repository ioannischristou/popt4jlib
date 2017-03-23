package popt4jlib.BH;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import parallel.Barrier;
import parallel.ComplexBarrier;
import parallel.DMCoordinator;
import parallel.OrderedBarrier;
import parallel.ParallelException;
import parallel.TaskObject;
import parallel.distributed.PDBTExecInitedClt;
import parallel.distributed.RRObject;
import popt4jlib.Arg2ChromosomeMakerIntf;
import popt4jlib.Chromosome2ArgMakerIntf;
import popt4jlib.Constants;
import popt4jlib.FunctionEvaluationTask;
import popt4jlib.LocalSearchFunctionEvaluationTask;
import popt4jlib.FunctionIntf;
import popt4jlib.GLockingObservableObserverBase;
import popt4jlib.LocalOptimizerIntf;
import popt4jlib.ImmigrationIslandOpIntf;
import popt4jlib.ObserverIntf;
import popt4jlib.OptimizerException;
import popt4jlib.OptimizerIntf;
import popt4jlib.RandomChromosomeMakerIntf;
import popt4jlib.SubjectIntf;
import popt4jlib.LocalSearch.IdentitySearchOptimizer;
import utils.DataMgr;
import utils.Debug;
import utils.Messenger;
import utils.PairObjDouble;
import utils.Params;
import utils.RndUtil;


/**
 * A parallel/distributed implementation of the Generalized Adaptive Basin 
 * Hopping algorithm, using an island-model of computation, where multiple 
 * populations are evolved in separate "islands", exchanging individuals in the
 * same migration model as in DGA and DPSO classes. For more information on the
 * Generalized Adaptive Basin Hopping meta-heuristic see:
 * D. Izzo, "PYGMO AND PYKEP: OPEN SOURCE TOOLS FOR MASSIVELY PARALLEL 
 * OPTIMIZATION IN ASTRODYNAMICS (THE CASE OF INTERPLANETARY TRAJECTORY 
 * OPTIMIZATION)", Proc. of the 5th Intl. Conf. on Astrodynamics Tools and 
 * Techniques, ICATT.2012.
 * <p> This implementation features an island model of computation where there
 * exist multiple sub-populations each running in its own thread of computation;
 * migration between sub-populations by default implements a counter-clock-wise
 * unidirectional ring topology; this topology can be over-ridden by passing in
 * the parameters a <CODE>popt4jlib.ImmigrationIslandOpIntf</CODE> object that
 * will decide how the routes are to be defined. In any case, up to 2 particles
 * from each island may move in each generation.</p>
 * <p> Besides the shared-memory parallelism inherent in the class via which 
 * each island runs in its own thread of execution, the class allows for the 
 * distribution of function evaluations in a network of 
 * <CODE>parallel.distributed.PDBTExecInitedWrk</CODE> workers running in their
 * own JVMs (as long as the function arguments and parameters are serializable), 
 * connected to a <CODE>parallel.distributed.PDBTExecInitedSrv</CODE> server 
 * object, to which the DGABH class may also be connected as a client via a
 * <CODE>parallel.distributed.PDBTExecInitedClt</CODE> object; in such cases,
 * the client must first submit a <CODE>parallel.distributed.RRObject</CODE> 
 * that will be the initialization command for the workers (no-op in most cases,
 * unless the function evaluations require some prior initialization). Of course 
 * a server must be up and running, and at least one worker must be connected to
 * this server, for this distribution scheme to work. Also, in case of running
 * distributed computations reusing this DGABH object to run another 
 * optimization problem is only allowed as long as there is no need to send 
 * another initialization command to the network of workers, as workers cannot 
 * be initialized twice.</p>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DGABH extends GLockingObservableObserverBase implements OptimizerIntf {
  private static int _nextId=0;
  private int _id;
  private Params _params;
  double _incValue=Double.MAX_VALUE;
  Object _inc;  // incumbent chromosome
  FunctionIntf _f;

  private DGABHThread[] _threads=null;
  private int[] _islandsPop;

	// this object, if present, dictates the immigration routes between islands.
	private ImmigrationIslandOpIntf _immigrationTopologySelector=null;
	
	private RRObject _pdbtInitCmd=null;  // if running in distributed mode, 
	// this object will be sent first to server as initialization command.


  /**
   * public no-arg constructor.
   */
  public DGABH() {
		super();
    _id = incrID();
  }


  /**
   * public constructor accepting the optimization parameters (making a local
   * copy of them).
   * @param params HashMap
   */
  public DGABH(HashMap params) {
    this();
    try {
      setParams(params);
    }
    catch (Exception e) {
      // no-op: cannot reach this point
    }
  }


  /**
   * returns a copy of the parameters of this DGABH object.
   * @return HashMap
   */
  public synchronized HashMap getParams() {
    return new HashMap(_params.getParamsMap());
  }


  /**
   * the optimization params are set to p. The method will throw if it is
   * invoked while another thread is running the minimize(f) method on the
   * same DGABH object.
   * @param p HashMap the parameters to pass-in
   * @throws OptimizerException if another thread is concurrently running the
   * <CODE>minimize(f)</CODE> method of this object.
   */
  public synchronized void setParams(HashMap p) throws OptimizerException {
    if (_f!=null) 
			throw new OptimizerException("cannot modify parameters while running");
    _params = new Params(p);  // own the params
		// set the distributed computing mode init cmd, if any is specified
		_pdbtInitCmd = (RRObject) _params.getObject("dgabh.pdbtexecinitedwrkcmd");
  }


  /**
   * returns the currently best known function argument that  minimizes the _f
   * function. The ObserverIntf objects would need this method to get the
   * current incumbent (and use it as they please). Note: even though the
   * method is not synchronized, it still executes atomically, and is in synch
   * with its counterpart, <CODE>setIncumbent()</CODE>.
   * @return Object
   */
  protected Object getIncumbentProtected() {
    try {
      Chromosome2ArgMakerIntf c2amaker =
          (Chromosome2ArgMakerIntf) _params.getObject("dgabh.c2amaker");
      Object arg = null;
      try {
        if (c2amaker == null) arg = _inc;
        else arg = c2amaker.getArg(_inc, _params.getParamsMap());
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
   * <li> &lt;"dgabh.randomparticlemaker",RandomChromosomeMakerIntf maker&gt; 
	 * mandatory, the RandomChromosomeMakerIntf Object responsible for creating 
	 * valid random chromosome Objects to populate the islands.
   * <li> &lt;"dgabh.numthreads",Integer nt&gt; optional, how many threads will 
	 * be used, default is 1. Each thread corresponds to an island in the DGA 
	 * model.
   * <li> &lt;"dgabh.c2amaker",Chromosome2ArgMakerIntf c2a&gt; optional, the 
	 * object that is responsible for tranforming a chromosome Object to a 
	 * function argument Object. If not present, the default identity 
	 * transformation is assumed.
   * <li> &lt;"dgabh.a2cmaker",Arg2ChromosomeMakerIntf a2c&gt; optional, the 
	 * object that is responsible for transforming a FunctionIntf argument Object 
	 * to a chromosome Object. If not present, the default identity transformation 
	 * is assumed. The a2c object is not only useful when other ObserverIntf 
	 * objects register for this SubjectIntf object and also add back solutions to 
	 * it (as FunctionIntf args), but also because the local-optimization process 
	 * that is part of the Basin-Hopping algorithm, may work on a different 
	 * representation space than the one the Basin-Hopping works (though there 
	 * does not seem to be any benefit from such a transformation).
   * <li> &lt;"dgabh.numgens",Integer ng&gt; optional, the number of generations 
	 * to run the DGABH, default is 1.
   * <li> &lt;"dgabh.immprob",Double prob&gt; optional, the probability with 
	 * which a sub-population will send some of its members to migrate to another 
	 * (island) sub-population, default is 0.01.
   * <li> &lt;"dgabh.numinitpop",Integer ip&gt; optional, the initial population 
	 * number for each island, default is 10.
	 * <li> &lt;"dgabh.immigrationrouteselector", 
	 *           popt4jlib.ImmigrationIslandOpIntf route_selector&gt;
	 * optional, if present defines the routes of immigration from island to 
	 * island; default is null, which forces the built-in unidirectional ring 
	 * routing topology.
   * <li> &lt;"ensemblename", String name&gt; optional, the name of the 
	 * synchronized optimization ensemble in which this DGABH object will 
	 * participate. In case this value is non-null, then a higher-level ensemble 
	 * optimizer must have appropriately called the method
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
	 * <p>Notice that for all keys above if the exact key of the form "dgabh.X" is 
	 * not found in the params, then the params will be searched for key "X" alone
	 * so that if key "X" exists in the params, it will be assumed that its value
	 * corresponds to the value of "dgabh.X" originally sought.
	 * </p>
	 * <li>&lt;"dgabh.chromosomeperturber", ChromosomePerturberIntf perturber&gt; 
	 * mandatory, the object responsible for producing new individuals that are 
	 * (presumably small) perturbations of an original starting individual. Extra
	 * parameters that the implementing perturber object requires must also be 
	 * present.
	 * <li>&lt;"dgabh.localoptimizer", popt4jlib.LocalOptimizerIntf locOpt&gt; 
	 * optional, the object responsible for performing a local-search around a 
	 * starting point and returning the best individual found by local-search. 
	 * Default is null which implies no local-search process. As with above, any
	 * additional parameters that the implementing locOpt object requires must 
	 * also be present.
	 * <li>&lt;"dgabh.pdbtexecinitedwrkcmd", RRObject cmd &gt; optional, the 
	 * initialization command to send to the network of workers to run function
	 * evaluation tasks, default is null, indicating no distributed computation.
	 * <li>&lt;"dgabh.pdbthost", String pdbtexecinitedhost &gt; optional, the name
	 * of the server to send function evaluation requests, default is localhost.
	 * <li>&lt;"dgabh.pdbtport", Integer port &gt; optional, the port the above 
	 * server listens to for client requests, default is 7891.
	 * </ul>
   * @param f FunctionIntf
   * @throws OptimizerException if the process fails
   * @return PairObjDouble  // Pair&lt;Object arg, Double val&gt;
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("DGABH.minimize(f): null f");
    try {
      synchronized (this) {
				if (_f != null) 
					throw new OptimizerException("DGABH.minimize(): another thread is "+
						                           "concurrently executing the method "+
						                           "on the same object");
        _f = f;
      }
      // add the function itself onto the parameters hashtable for possible use
      // by operators. The function has to be reentrant in any case for multiple
      // concurrent evaluations to be possible in the first place so there is no
      // new issue raised here
      if (_params.getObject("dgabh.function") == null)
        _params.getParamsMap().put("dgabh.function", f);
      int nt = 1;
			Integer ntI = (Integer) _params.getInteger("dgabh.numthreads");
			if (ntI != null) nt = ntI.intValue();
      if (nt < 1)throw new OptimizerException(
          "DGABH.minimize(): invalid number of threads specified");
      RndUtil.addExtraInstances(nt);  // not needed
			// set immigration topology router if it exists
			try {
				_immigrationTopologySelector = 
					(ImmigrationIslandOpIntf) _params.getObject(
						                                  "dgabh.immigrationrouteselector");
			}
			catch (ClassCastException e) {
				throw new OptimizerException("DGABH.minimize(): invalid "+
					                           "ImmigrationIslandOpIntf object specified"+
					                           " in params");
			}
      // check if this object will participate in an ensemble
      String ensemble_name = _params.getString("ensemblename");
      _threads = new DGABHThread[nt];
      _islandsPop = new int[nt];
      for (int i = 0; i < nt; i++) _islandsPop[i] = 0; // init.
      // the _XXX data members are correctly protected since the method executes
      // atomically, and the working threads are started below. FindBugs
      // complains unjustly here...
      try {
        parallel.Barrier.setNumThreads("dgabh." + _id, nt); // init Barrier obj.
      }
      catch (parallel.ParallelException e) {
        e.printStackTrace();
        throw new OptimizerException("barrier init. failed");
      }

      for (int i = 0; i < nt; i++) {
        _threads[i] = new DGABHThread(this, i);
				// call below is for use in synchronizing/ordering
        OrderedBarrier.addThread(_threads[i], "dgabh."+_id); 
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
          e.printStackTrace();  // itc: HERE rm asap
					Messenger mger = utils.Messenger.getInstance();
          mger.msg("this DGABH.minimize(f) method must "+
                   "be invoked from within the minimize(f)"+
                   " method of an ensemble optimizer "+
                   "that has properly called the "+
                   "Barrier.setNumThreads(<name>_master,<num>)"+
                   " method.",0);
          throw new OptimizerException("DGABH.minimize(f): ensemble broken");
        }
      }
      for (int i = 0; i < nt; i++) {
        _threads[i].start();
      }
      // the following is equivalent to for-each thread { thread.join(); }
      for (int i = 0; i < nt; i++) {
        DGABHThreadAux rti = _threads[i].getDGABHThreadAux();
        rti.waitForTask();
      }

      // done
      synchronized (this) {
        Chromosome2ArgMakerIntf c2amaker = 
					(Chromosome2ArgMakerIntf) _params.getObject("dgabh.c2amaker");
        Object arg = _inc;
        if (c2amaker != null) 
					arg = c2amaker.getArg(_inc, _params.getParamsMap());
        return new PairObjDouble(arg, _incValue);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new OptimizerException("DGABH.minimize() failed");
    }
    finally {
      reset();
    }
  }


  /**
   * reset the barrier and other objects used in the process
   */
  private synchronized void reset() {
    try {
      OrderedBarrier.getInstance("dgabh."+_id).reset();
      OrderedBarrier.removeInstance("dgabh."+_id);
      Barrier.removeInstance("dgabh."+_id);
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
   * individuals. By default, unless there is a seriously under-populated 
	 * island compared to the one with id=myid, it implements a uni-directional 
	 * ring migration topology, with a counter-clock-wise direction. This default 
	 * may be over-ridden by including in the parameters passed in, an object 
	 * implementing the <CODE>popt4jlib.ImmigrationIslandOpIntf</CODE> that will
	 * decide to which island migration from island with id=myid should occur 
	 * using knowledge of the current population distribution, as well as the 
	 * current generation number, gen.
   * @param myid int my island id
	 * @param gen int the current generation number
   * @return int if -1 indicates no migration
   */
  synchronized int getImmigrationIsland(int myid, int gen) {
		if (_immigrationTopologySelector!=null) {  // migration topology selector 
			                                         // exists, use it.
			return _immigrationTopologySelector.getImmigrationIsland(
				                                    myid, gen, 
				                                    _islandsPop,_params.getParamsMap());
		}
    for (int i=0; i<_islandsPop.length; i++)
      if (myid!=i && (_islandsPop[i]==0 || 
				  _islandsPop[myid]>2.5*_islandsPop[i])) return i;
    // populations are more or less the same size, so immigration will occur
    // with some small probability to an immediate neighbor
    double immprob = 0.01;
    Double ipD = _params.getDouble("dgabh.immprob");
    if (ipD!=null && ipD.doubleValue()>=0) immprob = ipD.doubleValue();
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
   * get the DGABHThread with the given id.
   * @param id int
   * @return DGABHThread
   */
  synchronized DGABHThread getDGABHThread(int id) {
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
   * @param ind DGABHIndividual
   * @throws OptimizerException in case of insanity (may only happen if the
   * function to be minimized is not reentrant and the debug bit
   * <CODE>Constants.DGABH</CODE> is set in the <CODE>Debug</CODE> class)
   */
  void setIncumbent(DGABHIndividual ind) throws OptimizerException {
    // method used to be synchronized but doesn't need to be
    try {
			Messenger mger = utils.Messenger.getInstance();
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      if (_incValue > ind.getValue()) {  // minimization problems only
        mger.msg("Updating Incumbent w/ val=" + ind.getValue(),0);
        if (Debug.debug(Constants.DGABH) != 0) {
          // sanity check
          Object arg = ind.getChromosome(); // assume the chromosome Object is
          // the same used for function evals.
          Chromosome2ArgMakerIntf c2amaker = 
						(Chromosome2ArgMakerIntf) _params.getObject("dgabh.c2amaker");
          if (c2amaker != null) // oops, no it wasn't
            arg = c2amaker.getArg(ind.getChromosome(), _params.getParamsMap());
          double incval = _f.eval(arg, _params.getParamsMap());
          if (Math.abs(incval - ind.getValue()) > 1.e-25) {
            mger.msg("DGABH.setIncumbent(): ind-val=" +
                     ind.getValue() + " fval=" + incval +" ???", 0);
            throw new OptimizerException(
                "DGABH.setIncumbent(): insanity detected; " +
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
      throw new OptimizerException("DGABH.setIncumbent(): double lock "+
				                           "somehow failed...");
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
   * _individuals List&lt;DGABHIndividual&gt; of the DGABHThread with id 0, and 
	 * clears the solutions from the _observers and _subjects map.
   * @param tinds List  // List&lt;DGABHIndividual&gt;
   * @param funcParams HashMap the function parameters
   */
  synchronized void transferSolutionsTo(List tinds, HashMap funcParams) {
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
          Arg2ChromosomeMakerIntf a2cmaker = 
						(Arg2ChromosomeMakerIntf) _params.getObject("dgabh.a2cmaker");
          if (a2cmaker != null)
            chromosomei = a2cmaker.getChromosome(sols.elementAt(i), 
							                                   _params.getParamsMap());
          else chromosomei = sols.elementAt(i); // chromosome,arg are the same
          DGABHIndividual indi = new DGABHIndividual(chromosomei,
                                                     this, _params, funcParams);
          tinds.add(indi);
          ++ocnt;
        }
        catch (OptimizerException e) {
          e.printStackTrace();  // report failure to create individual out of 
                                // the provided chromosome Object
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
          Arg2ChromosomeMakerIntf a2cmaker = 
						(Arg2ChromosomeMakerIntf) _params.getObject("dgabh.a2cmaker");
          if (a2cmaker != null)
            chromosomei = a2cmaker.getChromosome(sols.elementAt(i), 
							                                   _params.getParamsMap());
          else chromosomei = sols.elementAt(i); // chromosome, arg are the same
          DGABHIndividual indi = new DGABHIndividual(chromosomei,
                                                     this, _params, funcParams);
          tinds.add(indi);
          ++scnt;
        }
        catch (OptimizerException e) {
          e.printStackTrace();  // report failure to create individual out of
                                // the provided chromosome Object
        }
      }
      sols.clear();
    }
    Messenger.getInstance().msg("DGABH.transfer(): totally "+
                                ocnt+" sols from observers and "+
                                scnt+" sols from subjects transferred",2);
  }

	
	/**
	 * get the initialization command for the network of workers to run function
	 * evaluation requests.
	 * @return RRObject
	 */
	synchronized RRObject getPDBTInitCmd() {
		return _pdbtInitCmd;
	}


  /**
   * get the id of this object.
   * @return int
   */
  synchronized int getId() { return _id; }


  /**
   * auxiliary method providing unique ids for objects of this class.
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
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DGABHThread extends Thread {
  private DGABHThreadAux _aux;


  DGABHThread(DGABH master, int id) throws OptimizerException {
    _aux = new DGABHThreadAux(master, id);
  }


  DGABHThreadAux getDGABHThreadAux() {
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
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DGABHThreadAux {
  private int _id;
  private int _uid;
  private DGABH _master;
  private HashMap _p;
  private HashMap _fp;
  private boolean _finish = false;
  private List _individuals;  // List<DGABHIndividual> 
  private Vector _immigrantsPool;  // Vector<Individual>
  private double _inc=Double.MAX_VALUE;  // best island value
  private Object _incarg=null;  // the best position for the island-swarm
	
	private ChromosomePerturberIntf _chromosomePerturber;
	private LocalOptimizerIntf _locOpt;

	private PDBTExecInitedClt _pdbtExecInitedClt = null;

  DGABHThreadAux(DGABH master, int id) throws OptimizerException {
    _master = master;
    _id = id;
    _uid = (int) DataMgr.getUniqueId();
    _p = _master.getParams();  // returns a copy
    _p.put("thread.localid", new Integer(_id));
    _p.put("thread.id",new Integer(_uid));
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
          Messenger.getInstance().msg("no package info for object with key "+
						                          key,2);
        }
        _fp.put(key,val);
      }
    }
    // end creating _funcParams
		// set-up distributed computing mode
		try {
			RRObject init_cmd = _master.getPDBTInitCmd();
			if (init_cmd!=null) {
				String host = (String) _p.get("dgabh.pdbthost");
				if (host==null) host = "localhost";
				int port = 7891;
				try {
					port = ((Integer) _p.get("dgabh.pdbtport")).intValue();
				}
				catch (ClassCastException e) {
					Messenger.getInstance().msg("param value for dgabh.pdbtport "+
						                          "is not an Integer",2);
				}
				_pdbtExecInitedClt = new PDBTExecInitedClt(host,port);
				_pdbtExecInitedClt.submitInitCmd(init_cmd);
			}
		}
		catch (Exception e) {
			Messenger.getInstance().msg("couldn't set distributed computing "+
				                          "environment, will run on this machine only", 
				                          2);
			_pdbtExecInitedClt = null;
		}
		// set-up extra members that are needed
		_chromosomePerturber = 
			(ChromosomePerturberIntf) _p.get("dgabh.chromosomeperturber");
		LocalOptimizerIntf locOpt = 
			(LocalOptimizerIntf) _p.get("dgabh.localoptimizer");
		if (locOpt==null) _locOpt = new IdentitySearchOptimizer();
		else _locOpt = locOpt.newInstance();  // each thread must have its own
		                                      // LocalOptimizerIntf object to call
    _immigrantsPool = new Vector();
  }


  void runTask() {
    // start: do the DGABH
    try {
      initPopulation();
    }
    catch (Exception e) {
      e.printStackTrace();  // no-op
    }
    //System.err.println("initPopulation() done.");
    int numgens = 1;
    Integer ngI = (Integer) _p.get("dgabh.numgens");
    if (ngI!=null) numgens = ngI.intValue();
    String ensemble_name=null;
    try {
      ensemble_name = (String) _p.get("ensemblename");
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    for (int gen = 0; gen < numgens; gen++) {
			// synchronize with other threads
      Barrier.getInstance("dgabh."+_master.getId()).barrier();  
      try {
        if (ensemble_name!=null)  // synchronize with other optimizers' threads
          ComplexBarrier.getInstance(ensemble_name).barrier();  
      }
      catch (ParallelException e) {
        e.printStackTrace();  // no-op
      }
      //System.err.println("Island-Thread id=" + _id + " running gen=" + gen +
      //                   " popsize=" + _individuals.size());
			if (_id==0 && gen % 10 == 0) 
				utils.Messenger.getInstance().msg("DGABHThreadAux.runTask(): "+
					                                "running gen=" + gen, 2);
      recvInds();
      if (_individuals.size()>0) {
        try {
          nextGeneration(gen);
        }
        catch (Exception e) {
          e.printStackTrace();  // no-op
        }
      }
      sendInds(gen);
      _master.setIslandPop(_id, _individuals.size());
			// synchronize with other threads
      Barrier.getInstance("dgabh."+_master.getId()).barrier();  
    }
    if (ensemble_name!=null) {  // remove thread from ensemble barrier
      try {
        ComplexBarrier.removeCurrentThread(ensemble_name);
      }
      catch (Exception e) {
        e.printStackTrace();  // no-op
      }
    }
		if (_pdbtExecInitedClt!=null) {  // disconnect from network of workers
			try {
				_pdbtExecInitedClt.terminateConnection();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
    // end: declare finish
    setFinish();
  }


  synchronized boolean getFinish() {
    return _finish;
  }


  synchronized void setFinish() {
    _finish = true;
    notify();
  }


  synchronized void waitForTask() {
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


  private void initPopulation() 
		throws OptimizerException, InterruptedException {
    Params p2 = new Params(_p);
		int initpopnum = 10;
    Integer initpopnumI = p2.getInteger("dgabh.numinitpop");
    if (initpopnumI!=null) initpopnum = initpopnumI.intValue();
    _individuals = new ArrayList();  // used to be Vector<DGABHIndividual>
    RandomChromosomeMakerIntf amaker = 
			(RandomChromosomeMakerIntf) p2.getObject("dgabh.randomparticlemaker");
    // if no such makers are provided, can only throw exception		
		if (amaker==null) 
			throw new OptimizerException("DGABHThreadAux.initPopulation(): "+
				                           "null randomparticlemaker");
    boolean run_locally = true;
		if (_pdbtExecInitedClt!=null) {  // function evaluations go distributed
			TaskObject[] tasks = new TaskObject[initpopnum];
      Chromosome2ArgMakerIntf c2amaker =
          (Chromosome2ArgMakerIntf) p2.getObject("dgabh.c2amaker");
			Object[] chromosomes = new Object[initpopnum];
			for (int i=0; i<initpopnum; i++) {
				Object chromosome = amaker.createRandomChromosome(_fp);
				chromosomes[i] = chromosome;
	      Object arg = chromosome;
	      if (c2amaker != null) arg = c2amaker.getArg(chromosome, _fp);
				tasks[i] = new FunctionEvaluationTask(_master._f, arg, _fp);
			}
			try {
				Object[] results = _pdbtExecInitedClt.submitWorkFromSameHost(tasks, 1);
				// results are same tasks, but with values for the function evaluations
				for (int i=0; i<initpopnum; i++) {
					double val = ((FunctionEvaluationTask) results[i]).getObjValue();
					Object chromosome = chromosomes[i];
					DGABHIndividual indi = new DGABHIndividual(chromosome, val, _master);
					_individuals.add(indi);
				}
				run_locally = false;
			}
			catch (Exception e) {  // oops, distributed mode failed, go for local
				e.printStackTrace();
				run_locally = true;
			}
		}		
		if (run_locally) {
			for (int i=0; i<initpopnum; i++) {
				Object chromosome = amaker.createRandomChromosome(_p);
				DGABHIndividual indi = new DGABHIndividual(chromosome, _master, 
					                                         p2, _fp);
				_individuals.add(indi);
			}
		}
    // finally update island's incumbent
    DGABHIndividual best = null;
    for (int i=0; i<_individuals.size(); i++) {
      DGABHIndividual indi = (DGABHIndividual) _individuals.get(i);
      double vi = indi.getValue();
      if (vi<_inc) {
        _incarg = indi;
        _inc = vi;
        best = indi;
      }
    }
    if (best!=null) 
			_master.setIncumbent(best);  // update master's best soln found if needed
  }


  private synchronized void recvInds() {
    if (_immigrantsPool.size()>0) {
      _individuals.addAll(_immigrantsPool);
      // update island's best
      for (int i=0; i<_immigrantsPool.size(); i++) {
        try {
          DGABHIndividual indi = (DGABHIndividual) _immigrantsPool.elementAt(i);
          Object bpi = indi.getChromosome();
          double vali = indi.getValue();
          if (vali < _inc) {
            _inc = vali;
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
    if (_id==0) {  // was ((DGABHThread) Thread.currentThread()).getId()==0
      _master.transferSolutionsTo(_individuals, _fp);
    }
  }


  private void sendInds(int gen) {
    int sendTo = _master.getImmigrationIsland(_id, gen);
    if (sendTo>=0) {
      Vector immigrants = getImmigrants();
      DGABHThreadAux receiverThreadAux = _master.getDGABHThread(sendTo).getDGABHThreadAux();
      receiverThreadAux.recvIndsAux(immigrants);
    }
    else {
      // synchronize order with null task
      try {
        OrderedBarrier.getInstance("dgabh."+_master.getId()).orderedBarrier(null);
      }
      catch (ParallelException e) {
        e.printStackTrace();  // cannot reach this point
      }
    }
  }


  // used to be synchronized
  private void recvIndsAux(Vector immigrants) {
    // guarantee the order in which the immigrants are placed in _individuals
    // so that all runs with same seed produce identical results
    try {
      OrderedBarrier.getInstance("dgabh."+_master.getId()).
				orderedBarrier(new RecvIndTask(_immigrantsPool, immigrants));
    }
    catch (ParallelException e) {
      e.printStackTrace();  // cannot reach this point
    }
  }


  private void nextGeneration(int gen) throws OptimizerException {
		Params p3 = new Params(_p);
    // 0. update each individual
		boolean compute_val_locally = _master.getPDBTInitCmd()==null;
    Chromosome2ArgMakerIntf c2amaker = 
			(Chromosome2ArgMakerIntf) p3.getObject("dgabh.c2amaker");
		Arg2ChromosomeMakerIntf a2cmaker = 
			(Arg2ChromosomeMakerIntf) p3.getObject("dgabh.a2cmaker");
		Object[] newchromosomes = null; 
		if (!compute_val_locally) {  // function evaluations go run distributed!
			newchromosomes = new Object[_individuals.size()];
			for (int i=0; i<_individuals.size(); i++) {
				DGABHIndividual indi = (DGABHIndividual) _individuals.get(i);
				try {
					Object newchromosomei = 
						_chromosomePerturber.perturb(indi.getChromosome(), _p);
					newchromosomes[i] = newchromosomei;					
				}
				catch (Exception e) {
					e.printStackTrace();  // no-op
				}
			}			
			TaskObject[] tasksarr = new TaskObject[newchromosomes.length];
			for (int i=0; i<tasksarr.length; i++) { 
				// each task is a request for a local-search starting from the perturbed
				// individual
				Object x0 = c2amaker==null ? newchromosomes[i] : 
					                           c2amaker.getArg(newchromosomes[i],_p);
				tasksarr[i] = 
					new LocalSearchFunctionEvaluationTask(_locOpt.newInstance(), 
						                                    _master._f, x0, _p);
			}
			try {
				Object[] results = 
					_pdbtExecInitedClt.submitWorkFromSameHost(tasksarr, 1);
				// now, decide whether to keep the old or the new population
				// according to best value: if the new population contains an individual
				// strictly dominating all in the old, replace old, else leave old.
				boolean replace=false;
				for (int i=0; i<results.length; i++) {
					double vi = 
						((LocalSearchFunctionEvaluationTask) results[i]).getObjValue();
					if (vi<_inc) {
						replace=true;
						break;
					}
				}
				String do_rep = replace ? " replace " : " not replace ";
				System.err.println("Thread-"+_id+", Generation-"+gen+": will "+do_rep+" existing population");  // itc: HERE rm asap
				if (replace) {
					for (int i=0; i<results.length; i++) {
						DGABHIndividual indi = (DGABHIndividual) _individuals.get(i);
						double vali = 
							((LocalSearchFunctionEvaluationTask) results[i]).getObjValue();
						Object chromoi = 
							a2cmaker==null ? 
							  ((LocalSearchFunctionEvaluationTask)results[i]).getArgMin() :
							  a2cmaker.getChromosome(
									((LocalSearchFunctionEvaluationTask)results[i]).getArgMin(), 
									_p);
						indi.setValues(chromoi, vali);
						// update island and total best
						if (indi.getValue()<_inc) {
							_inc = indi.getValue();
							_incarg = indi.getChromosome();
							_master.setIncumbent(indi);
						}
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();  // itc: HERE rm asap
				// must do the work locally
				utils.Messenger.getInstance().msg("failed to send function evaluation "+
					                                "tasks over the network, will resort"+
					                                " to local computations", 0);
				compute_val_locally = true;
			}			
		}
		if (compute_val_locally) {
			HashMap p2 = new HashMap(_p);
			List new_pop = new ArrayList();  // List<PairObjDouble> >
			boolean replace = false;
			for (int i=0; i<_individuals.size(); i++) {
				DGABHIndividual indi = (DGABHIndividual) _individuals.get(i);
				try {
					Object newchromosomei = 
						_chromosomePerturber.perturb(indi.getChromosome(), _p);
					Object x0i = c2amaker==null ? newchromosomei :
						                           c2amaker.getArg(newchromosomei, _p);
					p2.put("x0", x0i);
					_locOpt.setParams(p2);
					PairObjDouble pair = _locOpt.minimize(_master._f);
					if (pair.getDouble()<_inc) replace=true;
					new_pop.add(pair);
				}
				catch (Exception e) {
				  e.printStackTrace();  
					new_pop.add(null);  // ignore exception
				}
			}
			if (replace) {
				for (int i=0; i<_individuals.size(); i++) {
					DGABHIndividual indi = (DGABHIndividual) _individuals.get(i);
					PairObjDouble p = (PairObjDouble) new_pop.get(i);
					if (p==null) continue;
					Object newchromosomei = p.getArg();
					// does it need conversion?
					newchromosomei = a2cmaker==null ? 
						                 newchromosomei : 
						                 a2cmaker.getChromosome(newchromosomei, _p);
					double vali = p.getDouble();
					indi.setValues(newchromosomei, vali);
					// update island and total best
					if (indi.getValue()<_inc) {
						_inc = indi.getValue();
						_incarg = indi.getChromosome();
						_master.setIncumbent(indi);
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
      DGABHIndividual indi = (DGABHIndividual) _individuals.get(i);
      double ival = indi.getValue();
      if (ival<best_val) {
        best_ind = i;
        best_val = ival;
      }
    }
    if (best_ind>=0) {
      imms.add(_individuals.get(best_ind));
      _individuals.remove(best_ind);
    }
    // repeat for second guy, only if there is someone to leave behind in this 
		// island
    if (_individuals.size()>1) {
      best_val = Double.MAX_VALUE;
      best_ind = -1;
      for (int i = 0; i < _individuals.size(); i++) {
        DGABHIndividual indi = (DGABHIndividual) _individuals.get(i);
        double ival = indi.getValue();
        if (ival < best_val) {
          best_ind = i;
          best_val = ival;
        }
      }
      imms.add(_individuals.get(best_ind));
      _individuals.remove(best_ind);
    }
    return imms;
  }

}


/**
 * auxiliary class not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DGABHIndividual {
  private Object _x;  // current position
  private double _val=Double.MAX_VALUE;  // raw objective value
  private FunctionIntf _f=null;
  private DGABH _master;  // ref. back to master DGABH object


  DGABHIndividual(Object chromosome, DGABH master,
                         Params params, HashMap funcparams)
      throws OptimizerException {
    _x = chromosome;
    _master = master;
    _f = _master.getFunction();
    computeValue(params, funcparams);  // may throw OptimizerException
  }

	
  DGABHIndividual(Object chromosome, double val, DGABH master)
      throws OptimizerException {
    _x = chromosome;
    _master = master;
    _f = _master.getFunction();
		_val = val;
  }


  public String toString() {
    String r = "Chromosome=[";
    r += _x.toString();
    r += "] val="+_val;
    return r;
  }
  Object getChromosome() { return _x; }
  double getValue() { return _val; }


  void setValues(Object chromosome, Params params, HashMap funcParams)
    throws OptimizerException {
    _x = chromosome;
    Chromosome2ArgMakerIntf c2amaker =
        (Chromosome2ArgMakerIntf) params.getObject("dgabh.c2amaker");
    Object arg = null;
    if (c2amaker == null) arg = _x;
    else arg = c2amaker.getArg(_x, params.getParamsMap());
    _val = _f.eval(arg, funcParams);  // was _master._f which is also safe
  }
  void setValues(Object chromosome, double val) throws OptimizerException {
    _x = chromosome;
		_val = val;
  }
  private void computeValue(Params params, HashMap funcParams)
      throws OptimizerException {
    if (_val==Double.MAX_VALUE) {  // don't do the computation if already done
      Chromosome2ArgMakerIntf c2amaker =
        (Chromosome2ArgMakerIntf) params.getObject("dgabh.c2amaker");
      Object arg = null;
      if (c2amaker == null) arg = _x;
      else arg = c2amaker.getArg(_x, params.getParamsMap());
      _val = _f.eval(arg, funcParams);  // was _master._f which is also safe
    }
  }
}

