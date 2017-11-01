package graph.packing;

import graph.*;
import parallel.*;
import parallel.distributed.*;
import utils.*;
import popt4jlib.*;
import popt4jlib.LocalSearch.*;
import java.util.*;
import java.io.*;

/**
 * represents a node in the distributed B &amp; B tree of the hybrid
 * B &amp; B - GASP scheme for the 1-packing problem (max weighted independent
 * set problem with non-negative node weights), using bit-vectors as implemented 
 * in the class <CODE>popt4jlib.BoolVector</CODE> to represent the node-ids of 
 * the partial solution it contains. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DBBNode0 extends DBBNodeBase {

	private final static long _MIN_REQ_ELAPSED_TIME_4_DISTRIBUTION = 10000L;  
  // represents 10 seconds
	private static final double _BND_RATIO_4_EXEC_SUBMISSION = -1.0;  
	// any positive number for the above constant may? have a race-condition in 
	// the finish-line of DBBNode0 objects that would only be removed if lazy
	// decrements were not used. After the conversion of children to be sent to 
	// the local executor with "migrant" status, this race-condition should no
	// longer exist, but is not tested yet.
	private static final double _LSFAC = 0.8;
	private static final double _RSFAC = 1.2;
	private boolean _immigrant = false;  // set when sent for distributed exec.
	protected BoolVector _nodeids = null;  // active node ids in current soln.
	protected int _lvl;
	// though double, the next two volatile fields work ok, even in JDK 1.4
	// due to the fact that there is never concurrent update of these fields
	// in different threads.
	protected volatile double _bound = Double.NEGATIVE_INFINITY;  // bound cache
	private volatile double _cost = Double.NEGATIVE_INFINITY;  // cost cache

	// statistics book-keeping
	private final static boolean _GATHER_STATS = true;  // compile-time constant
	private final static HashMap _BN2ASizeDistr = new HashMap();  
  // map<Integer nodeidsize, Set<Integer> bestnodes2addsize>
	private final static HashMap _DBBNode0WgtDistr = new HashMap();
	// map<Double nodeids_wgt, Integer num_appearances>
	private static long _numTasksDistributed = 0;
	private static long _numDistributedCalls = 0;
	private static long _avgTimeBetweenDistrCalls = 0;
	private static volatile long _startRunTime = 0;
	private static volatile boolean _startRunTimeSet = false;
	private static volatile long _kmaxInit = 0;
	private static volatile long _lastQueueDistrTime = 0;
	private static long _lastDistrTime = 0;  // is essentially the max of the 
	                                         // thread-local values held below
	
  private static ThreadLocal _lastDistributionTimes = new ThreadLocal() {
    protected Object initialValue() {
      return null;
    }
  };
	
	private static ThreadLocal _dlsObjs = new ThreadLocal() {
		protected Object initialValue() {
			return new DLS(4);  // DLS with 4 threads
		}
	};

	
	/**
	 * comparator between node-sets used in sorting node-sets in the
	 * <CODE>getBestNodeSets2Add()</CODE> method if the appropriate option in the
	 * parameters of the calling program is set. This sorting only makes sense
	 * when children DBBNode0 objects will be "cut" short due to flags set to 
	 * guide the search process.
	 */
	private static NodeSetWeightComparatorBV _nscomtor = 
		new NodeSetWeightComparatorBV();


  /**
   * Sole constructor of a DBBNode0 object. Clients are not expected to
   * create such objects which are instead created dynamically through the
   * B&amp;B process.
   * @param r BoolVector // the set of (graph) node ids to be added
	 * to the nodes of the parent to represent a new partial solution
	 * @param lvl int  // the level at which this DBBNode0 object is
   */
  DBBNode0(BoolVector r, int lvl) {
		_nodeids = r;
		_lvl = lvl;
		if (_lvl==0) _immigrant=true;  // root node is an immigrant too
  }


  /**
   * the main method of the class, that processes the partial solution
   * represented by this object. 
   */
  public Serializable run() {
    DConditionCounterLLCClt cond_counter=null;
		utils.Messenger mger = utils.Messenger.getInstance();
		try {
      boolean foundincumbent = false;
			DBBTree _master = DBBTree.getInstance();
			cond_counter = _master.getDConditionCounterClt();
			setStartTime();
			// step 0.
			// see if worker we're working in, is in a closing state
			boolean wrk_is_closing = PDAsynchBatchTaskExecutorWrk.isClosing();
			if (wrk_is_closing) {  // stop computations here
				return null;  
			}
      // step 1.
      // see if limit has been reached
			int cur_counter = _master.incrementCounter();  // increment this process's 
			                                               // #DBBNode0 objects
			mger.msg("#DBBNode0 objects created by this process="+cur_counter, 3);
			if (cur_counter > _master.getMaxNodesAllowed()) {
				// don't accept any more requests from server
				PDAsynchBatchTaskExecutorWrk.setServerRequestsDisabled(true);
				return null;  // this worker is done
      } 
			//if (cur_counter % 100 == 0) 
			{
				String msg = "#DBBNode0 objs="+cur_counter;
				if (_GATHER_STATS) {
					synchronized (DBBNode0.class) {
						msg += " #TasksDistr=" + _numTasksDistributed +
							     " in "+_numDistributedCalls + " calls, occur every " +
								   _avgTimeBetweenDistrCalls + " msecs.";
						long durInSecs = (System.currentTimeMillis() - getStartTime())/1000;
						if (durInSecs==0) durInSecs=1;
						double avgNumObjsPerSec = (((double)cur_counter)/durInSecs);
						msg += " Avg#ObjsRanPerSecond=" + avgNumObjsPerSec;
						// modify kmax if needed
						double distr_speed = _numTasksDistributed / durInSecs;
						int kmax = _master.getMaxAllowedItersInGBNS2A();
						int kmax_cur = kmax;
						if (_kmaxInit==0) _kmaxInit = kmax;
						if (avgNumObjsPerSec<_LSFAC*distr_speed) {
							kmax = (int) (_LSFAC*kmax);  // throttle down kmax
							if (kmax>_master.getGraphSize()) {
								_master.setMaxAllowedItersInGBNS2A(kmax);
								msg += " kmax reduced to "+kmax;
							} else msg += " kmax="+kmax_cur;
						}
						else if (avgNumObjsPerSec<_RSFAC*distr_speed) {
							kmax = (int) (_RSFAC*kmax);  // throttle up kmax
							if (kmax<_kmaxInit) {
								_master.setMaxAllowedItersInGBNS2A(kmax);
								msg += " kmax increased to "+kmax;							
							} else msg += " kmax="+kmax_cur;
						}
						else msg += " kmax="+kmax;
					}  // end synchronized class
				}
				if (cur_counter % 100 == 0) {
					mger.msg(msg, 1);
					/*
					String wgts_distr = getSolnWgtsHistogram();
					msg = "Solution Weights Histogram: \n";
					msg += wgts_distr;
					mger.msg(msg, 1);
					*/
					/*
					msg="DBBNode0.getBestNodes2Add() Statistics: ";
					Iterator it = _BN2ASizeDistr.keySet().iterator();
					while (it.hasNext()) {
						Integer sz = (Integer) it.next();
						Set bn2aszs = (Set) _BN2ASizeDistr.get(sz);
						msg += "sz="+sz.intValue()+" --> [";
						Iterator it2 = bn2aszs.iterator();
						while (it2.hasNext()) {
							Integer v = (Integer) it2.next();
							msg += v.intValue();
							if (it2.hasNext()) msg += " ";
						}
						msg += "]\n";
					}
					mger.msg(msg, 1);
					*/
				}
				
			}
      // step 2.
      // check for pruning
      double bnd = getBound();
      if (bnd <= _master.getBound() || bnd < _master.getMinKnownBound()) {
        return null; // node is fathomed
      }
      // step 3.
      // add as many nodes as possible in GASP fashion
      Set candidates = null; // Set<BoolVector>
      while (true) {
        candidates = getBestNodeSets2Add();
        if (candidates != null && candidates.size() == 1) {
          //_nodes.addAll( (Set) candidates.iterator().next());
					BoolVector next_cand = (BoolVector) candidates.iterator().next();
					for (int i=next_cand.nextSetBit(0); 
						   i>=0; i=next_cand.nextSetBit(i+1)) {
						_nodeids.set(i);
					}
					_cost = Double.NEGATIVE_INFINITY;  // invalidate cache
        }
        else break;
      }
			if (_GATHER_STATS) {
				synchronized (DBBNode0.class) {
					Double soln_wgt = new Double(getCost());
					Integer num_apps = (Integer) _DBBNode0WgtDistr.get(soln_wgt);
					if (num_apps==null) {
						num_apps = new Integer(0);
					}
					num_apps = new Integer(num_apps.intValue()+1);
					_DBBNode0WgtDistr.put(soln_wgt,num_apps);
				}
			}
			// step 3.5
			// check if node is now leaf
			if (candidates==null || candidates.size()==0) {
				// System.err.println("found leaf node");
				_master.incrementTotLeafNodes();
			}
      // step 4.
      // check for incumbent
      if (getCost()>=_master.getBound()*_master.getLocalSearchExpandFactor()) {
        // itc 2015-02-26: inequality used to be strict (>)
				// itc 2015-03-20: added local-search expansion factor multiplication to
				// broaden cases where local-search kicks in.
        if (getCost()>_master.getBound()) _master.setIncumbent(this);
        foundincumbent = true;
      }
      // branch?
      if (candidates != null && candidates.size()!=0) {  
        // candidates.size() is in fact > 1
        try {
					// if i'm the root, send all children to the network
					if (_lvl==0) {
						TaskObject[] tasks = new TaskObject[candidates.size()];
						Iterator it = candidates.iterator();
						int l=0;
						while (it.hasNext()) {
	            BoolVector ns = (BoolVector) it.next();
							DBBNode0 child = new DBBNode0(ns, _lvl+1);
							child._immigrant = true;
							tasks[l++] = child;
						}
						try {
							if (_GATHER_STATS) {  // update statistics
								synchronized (DBBNode0.class) {
									long time_elapsed = _lastDistrTime==0 ? 0 : 
								             System.currentTimeMillis() - _lastDistrTime;
									_numTasksDistributed += tasks.length;
									_avgTimeBetweenDistrCalls = 
										(time_elapsed + 
										 _avgTimeBetweenDistrCalls*_numDistributedCalls)
										/ ++_numDistributedCalls;
									_lastDistrTime = System.currentTimeMillis();
								}
							}
							try {
								cond_counter.increment(tasks.length);  // notify cond-counter
							}
							catch (Exception e) {
								e.printStackTrace();
								System.exit(-1);
							}
							// the chunk size should probably be divided by the number of 
							// workers the server knows about 
							int chunk_size = tasks.length / 
								               PDAsynchBatchTaskExecutorWrk.getNumThreads();
							PDAsynchBatchTaskExecutorClt.getInstance().
								submitWorkFromSameHostInParallel(tasks,chunk_size);
							return null;
						}
						catch (PDAsynchBatchTaskExecutorNWAException e) {  
              // execute the tasks locally: as the submit method is the
							// submitWorkFromSameHostInParallel(tasks,sz), the tasks that
							// managed to execute will be null, and only the rest need to
							// run locally
							mger.msg("DBBNode0.run(): got children back due to workers' "+
								       "UNAVAILABILITY, will run them locally", 1);
							for (int i=0; i<tasks.length; i++) {
								if (tasks[i]!=null) tasks[i].run();  // run each child locally
							}
							return null;
						}						
					}  // if _lvl==0
          Set children = new TreeSet();  // Set<DBBNode0>
          Iterator it = candidates.iterator();
          int cnt_children = 0;
          while (it.hasNext()) {
            if (cnt_children++ > _master.getMaxChildrenNodesAllowed())
							break;
            BoolVector ns = (BoolVector) it.next();
						BoolVector ns2 = new BoolVector(_nodeids);
						ns2.or(ns);
						DBBNode0 child = new DBBNode0(ns2, _lvl+1);
            // check if child's bound is better than incumbent
            double childbound = child.getBound();
            if (childbound <= _master.getBound() ||
								childbound < _master.getMinKnownBound())  // not good enough
              continue;
            // speed up processing:
            // record new child incumbent if it exists (may be partial soln that
            // can be further augmented in step 3 above when it is processed)
            if (child.getCost() > _master.getBound()) {
							_master.setIncumbent(child);
              foundincumbent = true;  // is it needed here?
            }
            children.add(child);
          }
          int sz = children.size();
          if (sz == 0) {
            if (foundincumbent) _master.reduceTightenUpperBoundLvl();
						return null; // no children
          }
					// send the children to be executed elsewhere, only if it's time
					long ldt;
				  long time_elapsed;
					long time_elapsed_4_queue;
					boolean go_distr=false;
					synchronized (DBBNode0.class) {
						if (_GATHER_STATS) {
							time_elapsed = _lastDistrTime==0 ? 0 : 
								             System.currentTimeMillis() - _lastDistrTime;
						}
						time_elapsed_4_queue = 
							_lastQueueDistrTime == 0 ? 
							  0 : System.currentTimeMillis() - _lastQueueDistrTime;
						go_distr = time_elapsed_4_queue > 
							         10*_MIN_REQ_ELAPSED_TIME_4_DISTRIBUTION;
						if (go_distr) _lastQueueDistrTime = System.currentTimeMillis();
					}
					{  // block used to be synchronized on DBBNode0.class
						// Cilk-style heuristic to keep (other) workers busy:
						// figure out if tasks in worker's executor's queue must "go"
						if (go_distr) {
							int num_workers = 
								PDAsynchBatchTaskExecutorClt.getInstance().getNumWorkers();
							if (num_workers>1) {  // else it doesn't make sense
								TaskObject[] tasks =  // leave only first 100 tasks in worker
									PDAsynchBatchTaskExecutorWrk.getAllTasksAfterPos(100);
								// the tasks are all immigrants since
								// _BND_RATIO_4_EXEC_SUBMISSION is non-positive
								// so we don't have to increase cond_counter at all
								if (tasks!=null && tasks.length>0) {
									int chunk_size = Math.max(tasks.length / num_workers,1);
									try {
										_lastQueueDistrTime = System.currentTimeMillis();  // again
										mger.msg("DBBNode0.run(): SENDING "+tasks.length+
														 " tasks to the network", 0);
										PDAsynchBatchTaskExecutorClt.getInstance().
											submitWorkFromSameHostInParallel(tasks, chunk_size);
									}									
									catch (PDAsynchBatchTaskExecutorNWAException e) {  
			              // execute the tasks locally
										mger.msg("DBBNode0.run(): got tasks back due to workers'"+
								             "UNAVAILABILITY, will run them locally", 1);
										for (int i=0; i<tasks.length; i++) {
											if (tasks[i]!=null) tasks[i].run();  // null test useless
										}
									}
								}
							}
						}
						else if (_lastQueueDistrTime==0) 
							_lastQueueDistrTime = System.currentTimeMillis();
					}
					if (mustKeepLocally()) {  // keep them locally
						// run children, but decide based on size whether to run them 
						// now, or submit them to this worker's executor
						List tasks_2_submit = new ArrayList();  // List<TaskObject>
						List tasks_2_keep = new ArrayList();  // same
						Iterator cit = children.iterator();
						for (int i=0; i<sz; i++) {
							//DBBNode0 ci = (DBBNode0) children.get(i);
							DBBNode0 ci = (DBBNode0) cit.next();
							if (ci.getCost()<=
								  _master.getBound()*_BND_RATIO_4_EXEC_SUBMISSION) {  // submit
								ci._immigrant = true;
								tasks_2_submit.add(ci);
							} else tasks_2_keep.add(ci);
						}
						// first submit, then run the rest
						if (tasks_2_submit.size()>0) {
							boolean run_here=false;
							try {
								mger.msg("DBBNode0.run(): about to submit to local executor "+
									       tasks_2_submit.size()+" tasks.", 2);
								try {
									cond_counter.increment(tasks_2_submit.size());
								}
								catch (Exception e) {
									e.printStackTrace();
									System.exit(-1);
								}
								boolean ok = 
									PDAsynchBatchTaskExecutorWrk.executeBatch(tasks_2_submit);
								if (!ok) {
									run_here=true;
								}
							}
							catch (ParallelException e) {  // worker is in closing state
								run_here=true;
							}
							if (run_here) {
								mger.msg("DBBNode0.run(): failed to submit locally to executor", 
									       0);
								// tasks_2_keep = children;
								tasks_2_keep.clear();
								tasks_2_keep.addAll(children);
							}
						}
						// now run those that must be run now
						for (int i=0; i<tasks_2_keep.size(); i++) {
							DBBNode0 ci = (DBBNode0) tasks_2_keep.get(i);
							ci.run();
						}
					}
					else {  // send (all of them) them elsewhere for execution
						// check again for closing state of the worker
						if (PDAsynchBatchTaskExecutorWrk.isClosing()) {
							return null;
						}
						TaskObject[] tasks = new TaskObject[sz];
						Iterator cit = children.iterator();
						for (int i=0; i<sz; i++) {
							//DBBNode0 ci = (DBBNode0) children.get(i);
							DBBNode0 ci = (DBBNode0) cit.next();
							ci._immigrant = true;
							tasks[i] = ci;
						}
						children.clear();
						try {
							cond_counter.increment(sz);  // notify condition-counter
						}
						catch (Exception e) {
							e.printStackTrace();
							System.exit(-1);
						}
						try {
							if (_GATHER_STATS) {  // update statistics
								synchronized (DBBNode0.class) {
									_numTasksDistributed += tasks.length;
									_avgTimeBetweenDistrCalls = 
										(time_elapsed + 
										 _avgTimeBetweenDistrCalls*_numDistributedCalls)
										/ ++_numDistributedCalls;
									_lastDistrTime = System.currentTimeMillis();
								}
							}
							PDAsynchBatchTaskExecutorClt.getInstance().
								submitWorkFromSameHost(tasks);
						}
						catch (PDAsynchBatchTaskExecutorNWAException e) {  
              // execute the tasks locally: even if the submit method was the
							// submitWorkFromSameHostInParallel(tasks), then the tasks that
							// managed to execute would be null, and only the rest need to
							// run locally
							mger.msg("DBBNode0.run(): got children back due to workers' "+
								       "UNAVAILABILITY, will run them locally", 1);
							for (int i=0; i<tasks.length; i++) {
								if (tasks[i]!=null) tasks[i].run();  // run each child locally
							}
						}
					}
        }
        catch (Exception e) {  // insanity
          e.printStackTrace();
          System.exit( -1);
        }
      }
      else {  // no branching occurs
				// check for closing state here as well
				if (PDAsynchBatchTaskExecutorWrk.isClosing()) {
					return null;
				}
        if (foundincumbent) {
          _master.reduceTightenUpperBoundLvl();
          if (_master.getLocalSearch()) {  // perform a local search
						long start_time = System.currentTimeMillis();
            try {
							//Set nodeids = new IntSet(_nodeids);
							Set nodeids = new IntSet();
							for (int i=_nodeids.nextSetBit(0); 
								   i>=0; i=_nodeids.nextSetBit(i+1)) {
								nodeids.add(new Integer(i));
							}
							_master.incrNumDLSPerformed();
							if (mger.getDebugLvl()>=1) {
								String msg = "DBBNode0.run(): running DLS w/ starting soln=" +
									           nodeids + " w/ size="+nodeids.size();
								mger.msg(msg, 1);
							}
              // now do the local search
              // DLS dls = new DLS();
							DLS dls = (DLS) _dlsObjs.get();
              AllChromosomeMakerIntf movesmaker = 
								_master.getNewLocalSearchMovesMaker();
							if (movesmaker==null)  // use default
								movesmaker = 
									new IntSetN1RXPFirstImprovingGraphAllMovesMakerMT(1);
              // AllChromosomeMakerIntf movesmaker = new
              //    IntSetN2RXPGraphAllMovesMaker(1);
							// IntSetN2RXPGraphAllMovesMaker(1) gives better results on 
							// G_{|V|,p} random graphs
              IntSetNeighborhoodFilterIntf filter = new
                  GRASPPackerIntSetNbrhoodFilter3(1,_master.getGraph());
              FunctionIntf f = new SetWeightEvalFunction(_master.getGraph());
              HashMap dlsparams = new HashMap();
              dlsparams.put("dls.movesmaker", movesmaker);
              dlsparams.put("dls.x0", nodeids);
              //dlsparams.put("dls.numthreads", new Integer(10));  
              // itc: HERE parameterize above asap
              dlsparams.put("dls.maxiters", new Integer(100)); 
              // itc: HERE rm above asap
              int n10 = _master.getGraph().getNumNodes()/10 + 1;
              dlsparams.put("dls.intsetneighborhoodmaxnodestotry", 
								            new Integer(n10));
              dlsparams.put("dls.graph", _master.getGraph());
							dlsparams.put("dls.lock_graph", Boolean.FALSE);
              dlsparams.put("dls.intsetneighborhoodfilter", filter);
              //dlsparams.put("dls.createsetsperlevellimit", new Integer(100));
              dls.setParams(dlsparams);
              PairObjDouble pod = dls.minimize(f);
              Set sn = (Set) pod.getArg();
              if (sn != null && -pod.getDouble() > getCost()) {
								_nodeids.clear();
								_nodeids.setAll(sn);
								_cost = -pod.getDouble();
                // record new incumbent
                _master.setIncumbent(this);
								_master.incrementTotLeafNodes();
								if (_GATHER_STATS) {
									synchronized (DBBNode0.class) {
										Double sol_wgt = new Double(getCost());
										Integer num_apps = (Integer) _DBBNode0WgtDistr.get(sol_wgt);
										if (num_apps==null) {
											num_apps = new Integer(0);
										}
										num_apps = new Integer(num_apps.intValue()+1);
										_DBBNode0WgtDistr.put(sol_wgt,num_apps);
									}
								}
              }
							// itc: HERE rm asap
							if (movesmaker instanceof GRASPPacker1SingleMoveMaker) {
								mger.msg("GRASPPackerSingleMoveMaker: #successes="+
									       GRASPPacker1SingleMoveMaker.getNumImprovements()+
									       " #failures="+
									       GRASPPacker1SingleMoveMaker.getNumFailures(),0);
							}
							// itc: HERE rm up to here asap
							long dur = System.currentTimeMillis()-start_time;
							_master.incrTimeSpentOnDLS(dur);
            }
            catch (Exception e2) {
              e2.printStackTrace();
            }
          }  // end local search
        }  // if foundincumbent
        // done
      }
			return null;
    }
    catch (ParallelException e) {
      e.printStackTrace();
			return null;
    }
		finally {
			if (_immigrant && cond_counter!=null) {
				try {
					// decrement cond_counter
					if (PDAsynchBatchTaskExecutorWrk.isClosing() || 
						  PDAsynchBatchTaskExecutorWrk.getServerRequestsDisabled()) {
						cond_counter.lazyDecrement();
						if (PDAsynchBatchTaskExecutorWrk.getNumTasksInQueue() <=
							  PDAsynchBatchTaskExecutorWrk.getNumThreads()) {  // take poison
							                                                   // pills into 
							                                                   // account
							cond_counter.sendLazyDecrements();
						} 
					} 
					else cond_counter.decrement();
				}
				catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			} 
			else if (_immigrant) {  // insanity
				System.err.println("null cond_counter???");
				System.exit(-1);
			}
		}
  }


	/**
	 * return the node-ids contained in the solution that this object represents
	 * as a <CODE>popt4jlib.BoolVector</CODE> object.
	 * @return BoolVector
	 */
	final BoolVector getNodeIds() {
		return _nodeids;
	}

	
	/**
	 * return the node-ids contained in the solution that this object represents
	 * as a <CODE>java.util.HashSet</CODE>.
	 * @return Set
	 */
	final protected Set getNodeIdsAsSet() {
		Set result = new HashSet();
		for (int i=_nodeids.nextSetBit(0); i>=0; i=_nodeids.nextSetBit(i+1)) {
			result.add(new Integer(i));
		}
		return result;
	}

	
  /**
   * compares this DBB-node with another DBB-node, via the master DBBTree's
   * DBBNodeComparator object for comparing DBB-nodes.
   * @param other Object  // DBBNode0
   * @return int
   */
  public int compareTo(Object other) {
    DBBNode0 o = (DBBNode0) other;
    return DBBTree.getInstance().getDBBNodeComparator().compare(this, o);
  }


	/**
	 * always returns false.
	 * @return boolean
	 */
  public boolean isDone() {
    return false;
  }


	/**
	 * unsupported operation, always throws.
	 * @param obj TaskObject
	 * @throws IllegalArgumentException
	 */
  public void copyFrom(TaskObject obj) throws IllegalArgumentException {
		throw new IllegalArgumentException("not supported");
  }


  /**
   * compares for equality using the object's <CODE>compareTo()</CODE> method.
   * @param other Object expected another BBBNode1 object
   * @return boolean return true iff the compareTo(other) method return 0.
   */
  public boolean equals(Object other) {
    int ct = compareTo(other);
    return (ct==0);
  }


  /**
   * returns the size of the nodes of this DBBNode0.
   * @return int
   */
  public int hashCode() {
		return getNodeIds().cardinality();
	}


	/**
	 * return the sum of weights of the active nodes.
	 * @return double
	 */
	protected double getCost() {
		if (_cost>=0.0) return _cost;  // use cache
		double res = 0.0;
		Graph g = DBBTree.getInstance().getGraph();
		for (int i=_nodeids.nextSetBit(0); i>=0; i=_nodeids.nextSetBit(i+1)) {
			Node ni = g.getNodeUnsynchronized(i);
			Double niwD = ni.getWeightValueUnsynchronized("value");
			double niw = niwD==null ? 1.0 : niwD.doubleValue();
			res += niw;
		}
		_cost = res;  // set cache
		return res;
	}


	/**
	 * return all immediate nbors of this solution's nodes, plus the solution's
	 * nodes themselves.
	 * @return Set // Set&lt;Node&gt;
	 */
  protected Set getForbiddenNodes() {
		Graph g = DBBTree.getInstance().getGraph();
    Set forbidden = new HashSet();
		for (int i=_nodeids.nextSetBit(0); i>=0; i=_nodeids.nextSetBit(i+1)) {
			Node n = g.getNodeUnsynchronized(i);
			forbidden.add(n);
      Set nnbors = n.getNborsUnsynchronized();
      forbidden.addAll(nnbors);			
		}		
    return forbidden;
  }
	
	
	/**
	 * same as getForbiddenNodes() but returns BoolVector with node-ids set.
	 * @return BoolVector
	 */
	private BoolVector getForbiddenNodesAsBoolVector() {
		Graph g = DBBTree.getInstance().getGraph();
		BoolVector forbidden = new BoolVector(_nodeids);  // _nodeids are forbidden
		for (int i=_nodeids.nextSetBit(0); i>=0; i=_nodeids.nextSetBit(i+1)) {
			Node n = g.getNodeUnsynchronized(i);
      Set nnbors = n.getNborsUnsynchronized();
      Iterator it = nnbors.iterator();
			while (it.hasNext()) {
				Node nbor = (Node) it.next();
				forbidden.set(nbor.getId());
			}
		}				
		return forbidden;
	}


  /**
   * compute a max possible number of nodes weights this soln can have.
   * One such bound is 
	 * _nodes.sum_weights + 
	 * (gsz - getForbiddenNodes().size())*max_open_node_weight/2
   * @return double
   */
  protected double getBound() {
    if (_bound>=0) return _bound;  // cache
		Graph g = DBBTree.getInstance().getGraph();
		double res = 0.0;
		for (int i=_nodeids.nextSetBit(0); i>=0; i=_nodeids.nextSetBit(i+1)) {
			Node ni = g.getNodeUnsynchronized(i);
			Double niw = ni.getWeightValueUnsynchronized("value");
			if (niw==null) res += 1.0;  // nodes without weights have weight value 1
			                            // as in the max. independent set problem.
			else res += niw.doubleValue();
		}
    Set forbidden = getForbiddenNodes();
		// due to the lazy-evaluation scheme used for the Graph._sortedNodeArrays
		// data member, it is not safe to call an unsynchronized version of the
		// Graph.getMaxNodeWeight(String, Set) method unless the thread-safe version
		// of Double-Check Locking idiom ("Single-Time Locking per thread" idiom)
		// was implemented; for this reason Graph implements no such unsynch version
		DBBTree master = DBBTree.getInstance();
		Double max_node_weightD = g.getMaxNodeWeight("value",forbidden);
		double mnw = max_node_weightD==null ? 1.0 : max_node_weightD.doubleValue();
    res += (master.getGraphSize()-forbidden.size())*mnw/2.0;  
    // itc 2015-02-11: added the division by 2 above
    _bound = res;
    return res;
  }


  /**
   * return Set&lt;BoolVector&gt; of all maximal nodesets that can be added
	 * together to the current active <CODE>_nodeIds</CODE> set.
   * @return Set // Set&lt;BoolVector&gt;
	 * @throws ParallelException never
   */
  protected Set getBestNodeSets2Add() throws ParallelException {
		DBBTree master = DBBTree.getInstance();
		final int num_nodes = master.getGraphSize();
    final int kmax = master.getMaxAllowedItersInGBNS2A();
		final boolean limit_node_children = 
			master.getMaxChildrenNodesAllowed() < Integer.MAX_VALUE;
    final Set ccands = getBestNodes2Add(_lvl==0);  // Set<Node>
		if (_GATHER_STATS) {
			synchronized (DBBNode0.class) {
				Set distr = 
					(Set) _BN2ASizeDistr.get(new Integer(_nodeids.cardinality()));
				if (distr==null) {
					distr = new TreeSet();
					_BN2ASizeDistr.put(new Integer(_nodeids.cardinality()), distr);
				}  // Set<Integer>
				distr.add(new Integer(ccands.size()));
			}
		}
    Set result;  // Set<BoolVector>
    if (master.getSortBestCandsInGBNS2A()) result = new TreeSet(_nscomtor);
    else result = new HashSet();  // Set<BoolVector>
    List store = new ArrayList();
    Stack temp = new Stack();  // Stack<BoolVector>
    Iterator cands_it = ccands.iterator();
    while (cands_it.hasNext()) {
      BoolVector ci = new BoolVector(num_nodes);
      Node n = (Node) cands_it.next();
      ci.set(n.getId());
      temp.push(ci);
    }
    if (_lvl==0) {
			// if root, return collection of each available node as singleton sets
			// this should speed up parallel processing
      // correct GASP behavior
      result.addAll(temp);
      return result;
    }
    // figure out all the maximal subsets of ccands that are not conflicting
    // as it is, this routine does not guarantee that the nodes are being added
    // in a GASP fashion, as when one node of a set ci is added to _nodeids, the
    // other nodes in ci may no longer be the "optimal" in GASP sense to add to
    // _nodeids.
    int cnt=0;  // used to stop the max subsets creation process from going wild
    while (temp.isEmpty()==false) {
      if (++cnt>=kmax || 
				  PDAsynchBatchTaskExecutorWrk.isClosing() ||
				  PDAsynchBatchTaskExecutorWrk.getServerRequestsDisabled()) break;
      BoolVector t = (BoolVector) temp.pop();
      cands_it = ccands.iterator();
      boolean expanded_t=false;
      while (cands_it.hasNext()) {
        Node n = (Node) cands_it.next();
				if (!limit_node_children && n.getId()<t.lastSetBit()) 
					continue;  // don't use middle ids when the children will not be cut
        if (isFree2Cover(n, t)) {
          BoolVector t2 = new BoolVector(t);
          t2.set(n.getId());
          temp.push(t2);
          expanded_t=true;
        }
      }
      if (expanded_t==false) {
        // make sure you don't insert smth that already exists
        boolean iscovered=false;
        for (int i=0; i<store.size() && !iscovered; i++) {
          BoolVector ti = (BoolVector) store.get(i);
          if (ti.containsAll(t)) iscovered=true;
        }
        if (!iscovered) store.add(t);
      }
    }
    if (temp.isEmpty()==false) {  // broke out because of too many combinations
      boolean cons;
			if (PDAsynchBatchTaskExecutorWrk.isClosing() ||
				  PDAsynchBatchTaskExecutorWrk.getServerRequestsDisabled()) {
				utils.Messenger.getInstance().msg(
					"DBBNode0.getBestNodeSets2Add(): in closing state, consolidating "+
					temp.size()+" sets with a result of size "+result.size(), 1);
			}
      while (temp.isEmpty()==false) {
        BoolVector c1 = (BoolVector) temp.pop();
        cons = true;
        Iterator it = result.iterator();
				// /* faster loop does not do redundant work
				while (it.hasNext()) {
          BoolVector c2 = (BoolVector) it.next();
          if (isFeas(c1,c2)) {
            it.remove();  //used to be result.remove(c2);
						c2.or(c1);
						temp.add(c2);
            cons=false;
            break;
          }
        }
				// */
        if (cons) {
          // make sure you don't insert smth that already exists
          boolean iscovered=false;
          for (int i=0; i<store.size() && !iscovered; i++) {
            BoolVector ti = (BoolVector) store.get(i);
            if (ti.containsAll(c1)) iscovered=true;
          }
          if (!iscovered) result.add(c1);
        }
      }
    }
    result.addAll(store);
    return result;
  }
	
	
	/**
	 * check whether children nodes must be sent for distribution or not, based
	 * on when the current thread sent children before. If it returns false, it 
	 * also updates the time of the last distribution to now. Notice that the 
	 * method also checks whether the client is currently submitting tasks (from
	 * an other thread). If so, then the method returns true so as to increase
	 * throughput.
	 * @return boolean
	 * @throws IOException
	 */
	private static boolean mustKeepLocally() throws IOException {
		long now = System.currentTimeMillis();
		long last_time = getLastDistributionTime();
		if (now - last_time > _MIN_REQ_ELAPSED_TIME_4_DISTRIBUTION) {
			PDAsynchBatchTaskExecutorClt clt = 
				PDAsynchBatchTaskExecutorClt.getInstance();			
			if (!clt.isCurrentlySubmittingWork()) {
				setLastDistributionTimeNow();
				return false;
			}
		}
		return true;
	}


  /**
   * return the Set&lt;Node&gt; that are best node(s) to add given the current
   * active <CODE>_nodeids</CODE> set. This is the set of nodes that are free to
	 * cover, have max. weight (within the fudge factor 
	 * <CODE>DBBNodeBase._ff</CODE>), and have the least weight of "free" NBors() 
	 * (again within same fudge factor).
	 * Alternatively, if the "useGWMIN2criterion" flag is true, the "GWMIN2"
	 * heuristic criterion is utilized, so that the free nodes that are within
	 * <CODE>DBBNodeBase._ff</CODE> times from the maximum value of the quantity
	 * $w_n / \Sum_{v \in N^+_n}w_v$ form the return set.
	 * @param isroot boolean if true then _ff is set to zero, so that all
	 * non-forbidden nodes with min. sum of neighbors-weights are returned.
	 * @throws ParallelException
   * @return Set // Set&lt;Node&gt;
   */
  private Set getBestNodes2Add(boolean isroot) throws ParallelException {
    final DBBTree master = DBBTree.getInstance();
		final int gsz=master.getGraphSize();
    final double perc_extra_nodes2add = master.getAvgPercExtraNodes2Add();
		final double ff = isroot ? 0.0 : DBBNodeBase.getFF();
		final boolean useGWMIN2 = master.getUseGWMIN24BestNodes2Add();
    Set best = new HashSet();
    double bestcost = Double.MAX_VALUE;
    double best_node_cost = Double.NEGATIVE_INFINITY;
    BoolVector forbidden = getForbiddenNodesAsBoolVector();
    for (int i=0; i<gsz; i++) {
      Node ni = master.getGraph().getNodeUnsynchronized(i);
      if (!forbidden.get(i)) {
        Double niwD = ni.getWeightValueUnsynchronized("value");
        double ni_weight = niwD==null ? 1.0 : niwD.doubleValue();
				if (useGWMIN2) {  // ni_weight must be divided by the sum of all 
					                // free neighbors' weights plus its own
					Set nibors = ni.getNborsUnsynchronized();
					double denom = ni_weight;
					Iterator it = nibors.iterator();
					while (it.hasNext()) {
						Node nb = (Node) it.next();
						Double bD = nb.getWeightValueUnsynchronized("value");
						if (!forbidden.get(nb.getId()))
							denom += (bD==null ? 1.0 : bD.doubleValue());
					}
					ni_weight /= denom;
				}
        if (Double.compare(ni_weight, best_node_cost)>0) {
          if (ff>=1.0) best.clear();
          else {  // remove nodes in best that are "too light" compared to ni
            Iterator bit = best.iterator();
            while (bit.hasNext()) {
              Node n = (Node) bit.next();
              Double wnD = n.getWeightValueUnsynchronized("value");
              double nw = wnD==null ? 1.0 : wnD.doubleValue();
							if (useGWMIN2) {  // nw must be divided accordingly
								Set nbors = n.getNborsUnsynchronized();
								double denom = nw;
								Iterator it = nbors.iterator();
								while (it.hasNext()) {
									Node nb = (Node) it.next();
									Double bD = nb.getWeightValueUnsynchronized("value");
									if (!forbidden.get(nb.getId()))
										denom += (bD==null ? 1.0 : bD.doubleValue());
								}
								nw /= denom;
							}
              if (nw < ni_weight*ff) bit.remove();
            }
          }
          best.add(ni);
          best_node_cost = ni_weight;
        }
        if (ni_weight >= ff*best_node_cost && !useGWMIN2) {  
          // is wrong to use "else if" in the above "if" stmt
          // check for "free" nbors
          // below is a valid but slow method to compute "free" nbors
          //Set ni_nnbors = new HashSet(ni.getNNbors());
          //ni_nnbors.removeAll(forbidden);
          //int nisize = ni_nnbors.size();
          Set ni_nbors = ni.getNborsUnsynchronized();
          Iterator nnit = ni_nbors.iterator();
          double nisize = 0.0;
          while (nnit.hasNext()) {
            Node nbor = (Node) nnit.next();
						if (forbidden.get(nbor.getId())) continue;
            Double nwD = nbor.getWeightValueUnsynchronized("value");
            double nw = nwD==null ? 1.0 : nwD.doubleValue();
            nisize += nw;
          }
          if (nisize<bestcost) {  // new best
            if (ff>=1.0) best.clear();
            else {  // remove nodes in best w/ nbors are "heavy" compared to ni
              Iterator bit = best.iterator();
              while (bit.hasNext()) {
                Node n = (Node) bit.next();
                Set n_nbors = n.getNborsUnsynchronized();
                double n_nbors_weight = 0.0;
                Iterator nn_it2 = n_nbors.iterator();
                while (nn_it2.hasNext()) {
                  Node nn = (Node) nn_it2.next();
									if (forbidden.get(nn.getId())) continue;
                  Double nwD = nn.getWeightValueUnsynchronized("value");
                  double nnw = nwD==null ? 1.0 : nwD.doubleValue();
                  n_nbors_weight += nnw;
                }
                if (n_nbors_weight > nisize*(2.0-ff)) bit.remove();
              }
            }
            best.add(ni);
            bestcost = nisize;
          }
          else if (nisize <= bestcost*(2.0-ff)) {  
            // approx. equal to best, add to set
            best.add(ni);
          }
          // else continue;
        }
        // else continue;
      }
    }  // for i=0...gsz-1
    if (perc_extra_nodes2add>0) {
      double num_extra_nodes2add = bestcost*perc_extra_nodes2add;
      int lvl = _lvl;
      if (lvl>0) {
        // add probabilistically some non-best nodes depending on the level
        // of the node, the "goodness" of the node and the user-defined quantity
				final int tid = Thread.currentThread() instanceof IdentifiableIntf ?
				    (int) ((IdentifiableIntf) Thread.currentThread()).getId() : 0;
				Random r = RndUtil.getInstance(tid).getRandom();
        for (int i = 0; i < gsz; i++) {
          Node ni = master.getGraph().getNodeUnsynchronized(i);
          if (forbidden.get(i)) continue;
          Set ni_nbors = ni.getNborsUnsynchronized();
          Iterator nnit = ni_nbors.iterator();
          double nisize = 0;
          while (nnit.hasNext()) {
            Node nbor = (Node) nnit.next();
						Double nwD = nbor.getWeightValueUnsynchronized("value");
						double nw = nwD==null ? 1.0 : nwD.doubleValue();
            if (!forbidden.get(nbor.getId())) nisize += nw;
          }
          double fitness = nisize > 0 ? bestcost / nisize : 1;
          double prob = num_extra_nodes2add * fitness / (gsz * Math.sqrt(lvl));
          double ri = r.nextDouble();  
          // used to be ri = RndUtil.getInstance().getRandom().nextDouble();
          if (ri<prob) {
            best.add(ni);
          }
        }
      }
    }
    return best;
  }


  /**
   * check if node nj can be set to one when the nodes in active are also set.
   * @param nj Node
   * @param active Set // Set&lt;Node&gt;
   * @return boolean true iff nj can be added to active
   */
  private static boolean isFree2Cover(Node nj, Set active) {
    if (active.contains(nj)) return false;
		Set nborsj = nj.getNborsUnsynchronized();  // no modification to take place
		Iterator itj = nborsj.iterator();
		while (itj.hasNext()) {
			Node nnj = (Node) itj.next();
			if (active.contains(nnj)) return false;
		}
    return true;
  }

	
  /**
   * check if node nj can be set to one when the nodes in active are also set.
   * @param nj Node
   * @param active BoolVector
   * @return boolean true iff nj can be added to active
   */
  private static boolean isFree2Cover(Node nj, BoolVector active) {
    if (active.get(nj.getId())) return false;
		Set nborsj = nj.getNborsUnsynchronized();  // no modification to take place
		Iterator itj = nborsj.iterator();
		while (itj.hasNext()) {
			Node nnj = (Node) itj.next();
			if (active.get(nnj.getId())) return false;
		}
    return true;
  }


	/**
	 * check if the nodes parameter can be added to the current active set of
	 * nodes represented by this DBBNode0 object.
	 * @param nodes Set // Set&lt;Node&gt;
	 * @return boolean true iff nodes can be added to the current solution
	 */
  private boolean isFeas(Set nodes) {
    Set allnodes = new HashSet();
		Graph g = DBBTree.getInstance().getGraph();
		for (int i=_nodeids.nextSetBit(0); i>=0; i=_nodeids.nextSetBit(i+1)) {
			Node ni = g.getNodeUnsynchronized(i);
			allnodes.add(ni);
		}
		Iterator it = nodes.iterator();
    while (it.hasNext()) {
      Node n = (Node) it.next();
      if (isFree2Cover(n, allnodes))
        allnodes.add(n);
      else return false;
    }
    return true;
  }


	/**
	 * check if c1 conflicts with c2, in that there exist n1 \in c1 that is a
	 * neighbor of some n2 \in c2.
	 * Notice that the method assumes that both c1 and c2 are currently feasible,
	 * ie the calls isFeas(c1) and isFeas(c2) must return true.
	 * @param c1 BoolVector
	 * @param c2 BoolVector
	 * @return boolean false iff c1 U nbors(c1) contain any of c2.
	 */
	private static boolean isFeas(BoolVector c1, BoolVector c2) {
		if (c1.cardinality()==0 || c2.cardinality()==0) return true;
		Graph g = DBBTree.getInstance().getGraph();
		for (int i=c1.nextSetBit(0); i>=0; i=c1.nextSetBit(i+1)) {
			if (c2.get(i)) continue;
			Node n1 = (Node) g.getNode(i);
			Set nbors_n1 = n1.getNborsUnsynchronized();
			Iterator itnbor1 = nbors_n1.iterator();
			while (itnbor1.hasNext()) {
				Node nbor1 = (Node) itnbor1.next();
				if (c2.get(nbor1.getId())) return false;
			}
		}
		return true;
	}

	
  private static long getLastDistributionTime() {
    Long p = (Long) _lastDistributionTimes.get();
    if (p==null) {
      p = new Long(0);
      _lastDistributionTimes.set(p);
    }
    return p.longValue();
  }
	private static void setLastDistributionTimeNow() {
		_lastDistributionTimes.set(new Long(System.currentTimeMillis()));
	}
	
	
	private static void setStartTime() {
		if (_startRunTimeSet) return;  // relies on _startRunTime* being volatile
		synchronized (DBBNode0.class) {
			_startRunTime = System.currentTimeMillis();
			_startRunTimeSet = true;
		}
	}
	private long getStartTime() {
		return _startRunTime;
	}


  /* debug routine */
  private String printNodes() {
    String res = "[";
    for (int i=_nodeids.nextSetBit(0); i>=0; i=_nodeids.nextSetBit(i+1)) {
			res += i;
      if (_nodeids.nextSetBit(i+1)>=0) res+= ", ";
    }
		res += "]";
    return res;
  }
	

	// debug routine
	private static synchronized String getSolnWgtsHistogram() {
		String result = "1   5   10"+
			              "   15   20"+
			              "   25   30"+
			              "   35   40"+
			              "   45   50"+
			              "   55   60"+
			              "   65   70"+
			              "   75   80"+
			              "   85   90"+
			              "   95  100\n";
		int min_num_apps = Integer.MAX_VALUE;
		int max_num_apps = 0;
		Iterator it = _DBBNode0WgtDistr.keySet().iterator();
		while (it.hasNext()) {
			Double wgt = (Double) it.next();
			if (Double.compare(wgt.doubleValue(),0.0)<=0) continue;
			Integer num_apps = (Integer) _DBBNode0WgtDistr.get(wgt);
			if (num_apps.intValue()>max_num_apps) max_num_apps = num_apps.intValue();
			if (num_apps.intValue()<min_num_apps) min_num_apps = num_apps.intValue();
		}
		int[] arr = new int[101];
		it = _DBBNode0WgtDistr.keySet().iterator();
		while (it.hasNext()) {
			Double wgt = (Double) it.next();
			Integer num_apps = (Integer) _DBBNode0WgtDistr.get(wgt);
			int scaled_val = 10*(num_apps-min_num_apps)/(max_num_apps-min_num_apps);
			if (scaled_val==0) scaled_val=1;
			int pos = (int) Math.floor(wgt);
			if (pos>100) pos=100;
			arr[pos]=scaled_val;
		}
		for (int i=0; i<10; i++) {
			for (int j=1; j<=100; j++) {
				if (arr[j]>=i+1) result += "*"; 
				else result += " ";
			}
			result += "\n";
		}
		return result;
	}
}

