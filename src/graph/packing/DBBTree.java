package graph.packing;

import graph.*;
import parallel.*;
import parallel.distributed.*;
import utils.*;
import popt4jlib.AllChromosomeMakerClonableIntf;
import popt4jlib.ObserverIntf;
import popt4jlib.SubjectIntf;
import popt4jlib.BoolVector;
import java.util.*;
import java.io.*;


/**
 * singleton class represents the Branch&amp;Bound Tree of the distributed
 * version of the BB-GASP method for MWIS. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class DBBTree implements ObserverIntf {

  private Graph _g;
  private int _gsz;  // cache
  private Set _incumbent;
	private boolean _sortBestCandsInGBNS2A = false;
  private boolean _localSearch = false;
	private AllChromosomeMakerClonableIntf _localSearchMovesMaker = null;
	private double _expandLocalSearchF = 1.0;
  private double _bound;
  private DBBNodeBase _root2;
  private int _maxNodesAllowed=Integer.MAX_VALUE;  // per process
  private int _counter=0;
  private int _tightenUpperBoundLvl=Integer.MAX_VALUE;  // obtain a tighter upper bound after this level
  private int _maxChildrenNodesAllowed=Integer.MAX_VALUE;
  private int _maxItersInGBNS2A=100000;
  private double _avgPercExtraNodes2Add=0.0;
	private boolean _useGWMIN24BestNodes2Add=false;
	private double _minKnownBound = Double.NEGATIVE_INFINITY;
  private DBBNodeComparatorIntf _bbnodecomp = null;
	private long _seed=0;  // when initializing on worker machines, must initialize the RndUtil first with same seed everywhere
	private int _totLeafNodes=0;  // counts total leaf nodes created (representing a maximal solution)
	private long _numDLSPerformed=0;  // counts total number of local-searches done in this tree
	private long _timeSpentOnDLS = 0;  // counts total time spend on local-searches in this tree

	private transient DConditionCounterLLCClt _condCounterClt;

	private static DBBTree _instance = null;


	/**
	 * get the unique instance of this class in any particular JVM. A call to the
	 * static method <CODE>DBBTree.init()</CODE> must preceed this call.
	 * @return DBBTree
	 */
	public static synchronized DBBTree getInstance() {
		return _instance;
	}
	
	
	/**
	 * two-argument version of the first method that any client of this class 
	 * must call to initialize the structure and possibly the workers in the 
	 * network that will execute the B&amp;B method in an asynchronous distributed
	 * manner. This method is better than the many-argument version in that the 
	 * paramsfile that will be read from each worker (residing in their own file
	 * system) may specify different accumulator-hosts/ports, to reflect the fact 
	 * that some workers should connect for notifications not directly to the 
	 * <CODE>parallel.distributed.DAccumulatorSrv</CODE> server that's responsible 
	 * for accumulating results from the processes, but instead to a 
	 * <CODE>parallel.distributed.BCastSrv</CODE> that will be broadcasting any 
	 * results from the accumulator server, to any process connected to it. 
	 * Notice that for this to happen, the small 
	 * <CODE>parallel.distributed.BCastSrv2DAccumulatorBridge</CODE> program needs
	 * to be running to connect the two mentioned servers.
	 * @param graphfile String
	 * @param paramsfile String
	 * @param sendInitCmd boolean
	 */
	public static synchronized void init(String graphfile, String paramsfile,
		                                   boolean sendInitCmd) {
		if (_instance==null) {
			Graph g=null;
			HashMap params;
			try {
				g = DataMgr.readGraphFromFile2(graphfile);
				params = DataMgr.readPropsFromFile(paramsfile);
				double initbound=0.0;
				_instance = new DBBTree(g, initbound);
				// set-up distributed component clients
				String pdahost = "localhost";
				if (params.containsKey("pdahost")) 
					pdahost = (String) params.get("pdahost");
				int pdaport = 7981;
				if (params.containsKey("pdaport")) 
					pdaport = ((Integer) params.get("pdaport")).intValue();
				//int dbglvl = utils.Messenger.getInstance().getDebugLvl();
				//long seed = RndUtil.getInstance().getSeed();
				PDAsynchBatchTaskExecutorClt.setHostPort(pdahost, pdaport);
				if (sendInitCmd) {  // send the pda init-cmd
					PDAsynchBatchTaskExecutorClt.getInstance().sendInitCmd(
						new PDAsynchInitDBBTreeCmd(graphfile, paramsfile));  
          // used to be PDAInitDBBTreeCmd
					PDAsynchBatchTaskExecutorClt.getInstance().awaitServerWorkers();					
				} 
				String cchost = "localhost";
				if (params.containsKey("cchost")) cchost = (String)params.get("cchost");
				int ccport = 7899;
				if (params.containsKey("ccport")) 
					ccport = ((Integer) params.get("ccport")).intValue();
				String ccname = "DCondCntCoord_"+cchost+"_"+ccport;
				String acchost = "localhost";
				if (params.containsKey("acchost")) 
					acchost = (String) params.get("acchost");
				int accport = 7900;
				if (params.containsKey("accport")) 
					accport = ((Integer) params.get("accport")).intValue();
				String accnotificationshost = "localhost";
				if (params.containsKey("accnotificationshost")) 
					accnotificationshost = (String) params.get("accnotificationshost");				
				int accnotificationsport = 9900;
				if (params.containsKey("accnotificationsport")) 
					accnotificationsport = ((Integer) params.get("accnotificationsport")).intValue();				
				DAccumulatorClt.setHostPort(acchost, accport, 
					                          accnotificationshost, accnotificationsport);
				DAccumulatorClt.registerListener(_instance, 
					                               DAccumulatorNotificationType._MAX);
				_instance._condCounterClt = 
					new DConditionCounterLLCClt(cchost, ccport, ccname);
				Boolean localSearchB = (Boolean) params.get("localsearch");
				boolean localsearch = false;
				if (localSearchB!=null) localsearch = localSearchB.booleanValue();
				AllChromosomeMakerClonableIntf localsearchtype = 
					(AllChromosomeMakerClonableIntf) params.get("localsearchtype");
				_instance.setLocalSearch(localsearch);
				_instance.setLocalSearchType(localsearchtype);
				Double ffD = (Double) params.get("ff");
				double ff = 0.85;
				if (ffD!=null) ff = ffD.doubleValue();
				if (!Double.isNaN(ff)) {
					DBBNodeBase.setFF(ff);
					DBBNodeBase.disallowFFChanges();
				}
				Integer tlvlI = (Integer) params.get("tightenboundlevel");
				int tightenboundlevel = Integer.MAX_VALUE;
				if (tlvlI!=null && tlvlI.intValue()>=1) 
					tightenboundlevel = tlvlI.intValue();
				_instance.setTightenUpperBoundLvl(tightenboundlevel);
				int maxitersinGBNS2A = 100000;
				Integer kmaxI = (Integer) params.get("maxitersinGBNS2A");
				if (kmaxI!=null && kmaxI.intValue()>0)
					maxitersinGBNS2A = kmaxI.intValue();
				_instance.setMaxAllowedItersInGBNS2A(maxitersinGBNS2A);
				Boolean sortmaxsubsetsB = (Boolean) params.get("sortmaxsubsets");
				boolean sortmaxsubsets = false;
				if (sortmaxsubsetsB!=null)
					sortmaxsubsets = sortmaxsubsetsB.booleanValue();
				_instance.setSortBestCandsInGBNS2A(sortmaxsubsets);
				double avgpercextranodes2add=0.0;
				Double avgpercextranodes2addD = 
					(Double) params.get("avgpercextranodes2add");
				if (avgpercextranodes2addD!=null)
					avgpercextranodes2add = avgpercextranodes2addD.doubleValue();
				_instance.setAvgPercExtraNodes2Add(avgpercextranodes2add);
				Boolean useGWMIN24BN2AB = (Boolean) params.get("useGWMIN2criterion");
				boolean useGWMIN2criterion = false;
				if (useGWMIN24BN2AB!=null)
					useGWMIN2criterion = useGWMIN24BN2AB.booleanValue();
				_instance.setUseGWMIN24BestNodes2Add(useGWMIN2criterion);
				Double expandlocalsearchfactorD = 
					(Double) params.get("expandlocalsearchfactor");
				double expandlocalsearchfactor = 1.0;
				if (expandlocalsearchfactorD!=null)
					expandlocalsearchfactor = expandlocalsearchfactorD.doubleValue();
				_instance.setLocalSearchExpandFactor(expandlocalsearchfactor);
				double minknownbound = Double.NEGATIVE_INFINITY;
				Double minknownboundD = (Double) params.get("minknownbound");
				if (minknownboundD!=null) 
					minknownbound = minknownboundD.doubleValue();
				_instance.setMinKnownBound(minknownbound);
				int maxnodechildren = Integer.MAX_VALUE;
				Integer maxchildrenI = (Integer) params.get("maxnodechildren");
				if (maxchildrenI != null && maxchildrenI.intValue() > 0)
					maxnodechildren = maxchildrenI.intValue();
				_instance.setMaxChildrenNodesAllowed(maxnodechildren);
				DBBNodeComparatorIntf dbbnodecomparator = 
					(DBBNodeComparatorIntf) params.get("dbbnodecomparator");
				if (dbbnodecomparator == null) 
					dbbnodecomparator = new DefDBBNodeComparator();
				_instance.setDBBNodeComparator(dbbnodecomparator);
				int maxnodesallowed = Integer.MAX_VALUE;
				Integer mnaI = (Integer) params.get("maxnodesallowed");
				if (mnaI!=null && mnaI.intValue()>0)
					maxnodesallowed = mnaI.intValue();
				_instance.setMaxNodesAllowed(maxnodesallowed);			
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}			
		}
	}


	/**
	 * the first method that any client of this class must call, to initialize
	 * the structure, and possibly the workers in the network of 
	 * <CODE>parallel.distributed.PDAsynchBatchTaskExecutorWrk</CODE> servers
	 * that will execute the B&amp;B method in an asynchronous distributed manner.
	 * This is a 27-argument method.
	 * @param graphfile String must be network-accessible, or else each worker
	 * node that will participate must have a copy of the graph-file in the exact
	 * same absolute path in its own file system
	 * @param g Graph will be null when executing on worker nodes
	 * @param initbound double
	 * @param pdahost String
	 * @param pdaport int
	 * @param cchost String
	 * @param ccport int
	 * @param acchost String
	 * @param accport int
	 * @param accnotificationshost String
	 * @param accnotificationsport int
	 * @param localsearch boolean
	 * @param localsearchtype AllChromosomeMakerClonableIntf
	 * @param ff double
	 * @param tightenboundlevel int
	 * @param maxitersinGBNS2A int
	 * @param sortmaxsubsets boolean
	 * @param avgpercextranodes2add double
	 * @param useGWMIN2criterion boolean
	 * @param expandlocalsearchfactor double
	 * @param minknownbound double
	 * @param maxnodechildren int
	 * @param dbbnodecomparator DBBNodeComparatorIntf
	 * @param seed long
	 * @param sendInitCmd boolean
	 * @param maxnodesallowed int
	 * @param dbglvl int debug level of the "default" (global, ie all) class-name
	 */
	public static synchronized void init(String graphfile, Graph g, double initbound,
		                                   String pdahost, int pdaport,
																			 String cchost, int ccport,
																			 String acchost, int accport,
																			 String accnotificationshost, int accnotificationsport,
																			 boolean localsearch,
																			 AllChromosomeMakerClonableIntf localsearchtype,
																			 double ff,
																			 int tightenboundlevel,
																			 int maxitersinGBNS2A,
																			 boolean sortmaxsubsets,
																			 double avgpercextranodes2add,
																			 boolean useGWMIN2criterion,
																			 double expandlocalsearchfactor,
																			 double minknownbound,
																			 int maxnodechildren,
																			 DBBNodeComparatorIntf dbbnodecomparator,
																			 long seed,
																			 boolean sendInitCmd,
																			 int maxnodesallowed,
																			 int dbglvl) {
		if (_instance==null) {
			if (g==null) {  // only load graph from file if it's not already passed-in
				try {
					g = DataMgr.readGraphFromFile2(graphfile);
				}
				catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
			_instance = new DBBTree(g, initbound);
			try {
				// set-up the distributed components clients
				PDAsynchBatchTaskExecutorClt.setHostPort(pdahost, pdaport);
				if (sendInitCmd) {  // send the pda init-cmd
					// insanity
					throw new Error("sendInitCmd should be false");
					/*
					PDAsynchBatchTaskExecutorClt.getInstance().sendInitCmd(
						new PDAInitDBBTreeCmd(graphfile, initbound,
							                    pdahost,pdaport,cchost,ccport,
							                    acchost,accport,
							                    accnotificationshost,accnotificationsport,
								                  localsearch, localsearchtype,
																	ff, tightenboundlevel,
																	maxitersinGBNS2A, sortmaxsubsets,
																	avgpercextranodes2add, useGWMIN2criterion,
																  expandlocalsearchfactor, minknownbound,
																	maxnodechildren, dbbnodecomparator, seed, 
							                    maxnodesallowed, dbglvl));
					PDAsynchBatchTaskExecutorClt.getInstance().awaitServerWorkers();
					*/
				} else {
			    Messenger.getInstance().setDebugLevel(dbglvl);  // set debug-level of the process
					RndUtil.getInstance().setSeed(seed);  // set random-number generator of the process
				}
				String ccname = "DCondCntCoord_"+cchost+"_"+ccport;
				DAccumulatorClt.setHostPort(acchost, accport, accnotificationshost, accnotificationsport);
				DAccumulatorClt.registerListener(_instance, DAccumulatorNotificationType._MAX);
				_instance._condCounterClt = new DConditionCounterLLCClt(cchost, ccport, ccname);
				_instance.setLocalSearch(localsearch);
				_instance.setLocalSearchType(localsearchtype);
				if (!Double.isNaN(ff)) {
					DBBNodeBase.setFF(ff);
					DBBNodeBase.disallowFFChanges();
				}
				_instance.setTightenUpperBoundLvl(tightenboundlevel);
				_instance.setMaxAllowedItersInGBNS2A(maxitersinGBNS2A);
				_instance.setSortBestCandsInGBNS2A(sortmaxsubsets);
				_instance.setAvgPercExtraNodes2Add(avgpercextranodes2add);
				_instance.setUseGWMIN24BestNodes2Add(useGWMIN2criterion);
				_instance.setLocalSearchExpandFactor(expandlocalsearchfactor);
				_instance.setMinKnownBound(minknownbound);
				_instance.setMaxChildrenNodesAllowed(maxnodechildren);
				_instance.setDBBNodeComparator(dbbnodecomparator);
				_instance.setMaxNodesAllowed(maxnodesallowed);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}


  private DBBTree(Graph g, double initbound) {
    if (g==null) throw new IllegalArgumentException("null graph passed in BBTree ctor");
		_g = g;
    // elements of the argument
    _gsz = _g.getNumNodes();
    _bound = initbound;
		//_root2 = new DBBNode12(new HashSet(),0,0,0);
		//_root2 = new DBBNode1(new HashSet(),0);  // this stmt forces DBBNode1 class
		                                           // to be used for MWIS
		_root2 = new DBBNode0(new BoolVector(_gsz),0);  // this stmt forces the 
		                                                // DBBNode0 class to be 
		                                                // used for MWIS
  }


  void run() throws InterruptedException, IOException, ClassNotFoundException,
		                ParallelException, PDAsynchBatchTaskExecutorException {
		// kick-start condition-counter
		_condCounterClt.increment();
		// send _root2 to the PDAsynchBatchTaskExecutorSrv via a Clt.
		TaskObject[] tasks = new TaskObject[1];
		tasks[0] = _root2;
		PDAsynchBatchTaskExecutorClt.getInstance().submitWorkFromSameHost(tasks);

		// and now must wait for the process to end via the DConditionCounterClt
		// connected to the corresponding Srv.
		_condCounterClt.await();

		// finally, get the solution from the accumulator!
		IntSet sol = (IntSet) DAccumulatorClt.getArgMax();
		// print value
		double w=0;
		Iterator it = sol.iterator();
		if (_incumbent==null) _incumbent = new HashSet();
		else _incumbent.clear();
		while (it.hasNext()) {
			Integer nI = (Integer) it.next();
			_incumbent.add(nI);
			int nid = nI.intValue();
			Double nwD = _g.getNodeUnsynchronized(nid).getWeightValueUnsynchronized("value");
			w += nwD.doubleValue();
		}
		DAccumulatorClt.disconnect();
    System.out.println("Soln found of size "+sol.size()+" and weight "+w);
  }


  Graph getGraph() { return _g; }
  int getGraphSize() { return _gsz; }

  void setMaxNodesAllowed(int n) {
    _maxNodesAllowed = n;
  }
  int getMaxNodesAllowed() { return _maxNodesAllowed; }

  double getAvgPercExtraNodes2Add() { return _avgPercExtraNodes2Add; }
  void setAvgPercExtraNodes2Add(double p) { _avgPercExtraNodes2Add = p; }
	boolean getUseGWMIN24BestNodes2Add() { return _useGWMIN24BestNodes2Add; }
	void setUseGWMIN24BestNodes2Add(boolean v) { _useGWMIN24BestNodes2Add=v; }
	void setLocalSearchExpandFactor(double v) { _expandLocalSearchF = v; }
	double getLocalSearchExpandFactor() { return _expandLocalSearchF; }


  synchronized DBBNodeComparatorIntf getDBBNodeComparator() {
    // synchronized to keep FindBugs happy
    if (_bbnodecomp == null) {
      _bbnodecomp = new DefDBBNodeComparator();
    }
    return _bbnodecomp;
  }
  synchronized void setDBBNodeComparator(DBBNodeComparatorIntf c) {
    if (_bbnodecomp!=null) return;  // can only set once
    _bbnodecomp = c;
  }

	synchronized void incrementTotLeafNodes() {
		++_totLeafNodes;
	}
	synchronized int getTotLeafNodes() {
		return _totLeafNodes;
	}

	DConditionCounterLLCClt getDConditionCounterClt() { return _condCounterClt; }

  synchronized void setMaxAllowedItersInGBNS2A(int iters) {
		_maxItersInGBNS2A=iters; 
	}
  synchronized int getMaxAllowedItersInGBNS2A() { return _maxItersInGBNS2A; }

  void setMaxChildrenNodesAllowed(int n) { _maxChildrenNodesAllowed=n; }
  int getMaxChildrenNodesAllowed() { return _maxChildrenNodesAllowed; }

	void setSortBestCandsInGBNS2A(boolean v) {
		_sortBestCandsInGBNS2A = v;
	}
	boolean getSortBestCandsInGBNS2A() {
		return _sortBestCandsInGBNS2A;
	}
	void setMinKnownBound(double v) { _minKnownBound = v; }
	double getMinKnownBound() { return _minKnownBound; }
  synchronized void setTightenUpperBoundLvl(int lvl) {
    _tightenUpperBoundLvl=lvl;
  }
  synchronized int getTightenUpperBoundLvl() { return _tightenUpperBoundLvl; }
  synchronized void reduceTightenUpperBoundLvl() {
    if (_tightenUpperBoundLvl>2 && _tightenUpperBoundLvl!=Integer.MAX_VALUE)
      _tightenUpperBoundLvl = (int) (_tightenUpperBoundLvl*0.8);
  }

  void setLocalSearch(boolean v) { _localSearch = v; }
  boolean getLocalSearch() { return _localSearch; }
	void setLocalSearchType(AllChromosomeMakerClonableIntf maker) {
		_localSearchMovesMaker = maker;
	}
	AllChromosomeMakerClonableIntf getNewLocalSearchMovesMaker() {
		return _localSearchMovesMaker==null ?
						null : _localSearchMovesMaker.newInstance();
	}
	/**
	 * get the total number of local-searches performed in this BB-Tree.
	 * @return long
	 */
	synchronized long getNumDLSPerformed() {
		return _numDLSPerformed;
	}
	/**
	 * stats book-keeping of DLS's performed.
	 */
	synchronized void incrNumDLSPerformed() { ++_numDLSPerformed; }
	synchronized void incrTimeSpentOnDLS(long dur) { _timeSpentOnDLS += dur; }
	synchronized long getTimeSpentOnDLS() { return _timeSpentOnDLS; }
  synchronized int incrementCounter() { return ++_counter; }
  synchronized int getCounter() { return _counter; }  // used to be unsynchronized


  synchronized void setIncumbent(DBBNodeBase n) {
		double ncost = n.getCost();
    if (ncost>_bound) {
      //_incumbent = n;
			_incumbent = new HashSet(n.getNodeIdsAsSet());
      _bound = ncost;
      System.err.println("new soln found w/ val=" + _bound);
			// send solution to accumulator
			IntSet sol = new IntSet(_incumbent);
			try {
				DAccumulatorClt.addArgDblPair(sol, ncost);
			}
			catch (Exception e) {
				utils.Messenger.getInstance().msg("DBBTree.setIncumbent(): DAccumulatorClt.addArgDblPair() FAILED, will exit...", 0);
				e.printStackTrace();
				System.exit(-1);
			}
    }
  }
/*
  public void setSolution(Set sol) {
    _incumbent = new HashSet(sol);
    _bound = sol.size();
  }
*/
  synchronized int[] getSolution() {  // synchronized to keep FindBugs happy...
    int inds[] = new int[_gsz];
    Iterator it = _incumbent.iterator();
    while (it.hasNext()) {
      Integer ni = (Integer) it.next();
      inds[ni.intValue()]=1;
    }
    return inds;
  }

	
  synchronized double getBound() { return _bound; }

	
	/**
	 * updates the current bound. This is a call-back method executed from the 
	 * <CODE>DAccumulatorClt.AsynchUpdateThread.notifyObservers()</CODE> which
	 * is called whenever a new incumbent is sent to the DAccumulatorSrv. It is
	 * made public because it's an interface method.
	 * @param subject 
	 */
	public void notifyChange(SubjectIntf subject) {
		double bound = ((Double) subject.getIncumbent()).doubleValue();
		utils.Messenger.getInstance().msg("DBBTree.notifyChange(): "+
			                                "notified of new bound "+bound, 1);
		synchronized (this) {
			_bound = bound;
		}
	}
}

