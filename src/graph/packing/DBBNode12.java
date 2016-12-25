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
 * set problem). Not part of the public API. This class extends 
 * <CODE>DBBNode1</CODE> and its real difference is that it decides on when
 * to send children nodes for distribution based on the time the ancestors of 
 * each DBBNode12 object started execution on the same JVM.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DBBNode12 extends DBBNode1 {

	private static final long _NUM_MILLIS_EXPECTED_2_RUN_ON_CURRENT = 30000L;  // 30 secs
	private int _lvl;  // node's current level
	private int _startLvl;  // the starting level from which to start counting
	                        // to see if current level implies distributing children
	private int _NUM_LVLS_2_RUN_ON_CURRENT = 2;  // how many levels to 
	                                              // run on the current thread 
	                                              // before submitting children
	                                              // to run in other JVMs; number
	                                              // is dynamically adapted
	private long _startExecTime = 0;  // indicates when node started execution


  /**
   * Sole constructor of a DBBNode12 object. Clients are not expected to
   * create such objects which are instead created dynamically through the
   * B&amp;B process.
   * @param r Set // Set&lt;Integer&gt; the set of (graph) node ids to be added
	 * to the nodes of the parent to represent a new partial solution
	 * @param lvl int the depth of the node (level) in the tree
	 * @param startlvl int the level of the tree at which execution on current 
	 * thread started
	 * @param startExecTime long the time the first ancestor of this node started
	 * execution on this JVM. The value is overridden by the actual start time of
	 * execution if this node executes on a different JVM than the one it was born
	 * (or, more generally, if lvl==startLvl.)
   */
  DBBNode12(Set r, int lvl, int startlvl, long startExecTime) {
		super(r, lvl);
		_startLvl = startlvl;
		_startExecTime = startExecTime;
  }


  /**
   * the main method of the class, that processes the partial solution
   * represented by this object. This method is heavily based on the 
	 * <CODE>BBNode1</CODE> class, adapted for distributed cluster parallel
	 * computing.
   */
  public Serializable run() {
	
		// figure out this node's _NUM_LVLS_2_RUN_ON_CURRENT
		long now = System.currentTimeMillis();
		if (now - _startExecTime < 0.8*_NUM_MILLIS_EXPECTED_2_RUN_ON_CURRENT) {
			++_NUM_LVLS_2_RUN_ON_CURRENT;
		}
		else if (now - _startExecTime > 1.2*_NUM_MILLIS_EXPECTED_2_RUN_ON_CURRENT) {
			if (_NUM_LVLS_2_RUN_ON_CURRENT>1) --_NUM_LVLS_2_RUN_ON_CURRENT;
		}					
		if (_lvl==_startLvl) {  // override start execution time
			_startExecTime = now;
		}
		
    DConditionCounterLLCClt cond_counter=null;
		utils.Messenger mger = utils.Messenger.getInstance();
		try {
			//int setsz = 0;
			//if (_nodeids!=null) setsz = _nodeids.size();
			//mger.msg("Running node w/ size="+setsz+
			//	       " on lvl="+_lvl+" _startLvl="+_startLvl+
			//	       " _NUM_LVLS_2_RUN_ON_CURRENT="+_NUM_LVLS_2_RUN_ON_CURRENT, 1);
			//if (_lvl % 10 == 0) // itc: HERE rm asap
			//	utils.Messenger.getInstance().msg("running node on lvl="+_lvl+" startLvl="+_startLvl+" solsz="+setsz,0);
      boolean foundincumbent = false;
			DBBTree _master = DBBTree.getInstance();
			cond_counter = _master.getDConditionCounterClt();
			// step 0.
			// see if worker we're working in, is in a closing state
			boolean wrk_is_closing = PDAsynchBatchTaskExecutorWrk.isClosing();
			if (wrk_is_closing) {  // stop computations here
				return null;  
			}
      // step 1.
      // see if limit has been reached
			int cur_counter = _master.incrementCounter();  // increment this process's #DBBNode12 objects
			if (_lvl % 10==0)
				mger.msg("#DBBNode12 objects created by this process="+cur_counter, 1);
			if (cur_counter > _master.getMaxNodesAllowed()) {
				return null; // stop computations
      }
      // step 2.
      // check for pruning
      double bnd = getBound();
      if (bnd <= _master.getBound() || bnd < _master.getMinKnownBound()) {
        return null; // node is fathomed
      }
      // step 3.
      // add as many nodes as possible in GASP fashion
      Set candidates = null; // Set<Set<Node> >
      while (true) {
        candidates = getBestNodeSets2Add();
        if (candidates != null && candidates.size() == 1) {
          //_nodes.addAll( (Set) candidates.iterator().next());
					Set next_cand = (Set) candidates.iterator().next();  // Set<Node>
					Iterator it = next_cand.iterator();
					while (it.hasNext()) {
						Node n = (Node) it.next();
						getNodeIds().add(new Integer(n.getId()));
					}
        }
        else break;
      }
			// step 3.5
			// check if node is now leaf
			if (candidates==null || candidates.size()==0) {
				// System.err.println("found leaf node");
				_master.incrementTotLeafNodes();
			}
      // step 4.
      // check for incumbent
      if (getCost() >= _master.getBound()*_master.getLocalSearchExpandFactor()) {
        // itc 2015-02-26: inequality used to be strict (>)
				// itc 2015-03-20: added local-search expansion factor multiplication to
				// broaden cases where local-search kicks in.
        if (getCost()>_master.getBound()) _master.setIncumbent(this);
        foundincumbent = true;
      }
      // branch?
      if (candidates != null && candidates.size()!=0) {  // candidates.size() is in fact > 1
        try {
          List children = new ArrayList();
          Iterator it = candidates.iterator();
          int cnt_children = 0;
					DBBNode12 child_to_keep = null;
          while (it.hasNext()) {
            if (cnt_children++ > _master.getMaxChildrenNodesAllowed())
							break;
            Set ns = (Set) it.next();
						Set ns2 = new HashSet(_nodeids);
						Iterator it2 = ns.iterator();
						while (it2.hasNext()) {
							ns2.add(new Integer(((Node) it2.next()).getId()));
						}
						DBBNode12 child=null;
						if (child_to_keep==null) {
							child_to_keep = new DBBNode12(ns2, _lvl+1, _startLvl, _startExecTime);
							child_to_keep._NUM_LVLS_2_RUN_ON_CURRENT = _NUM_LVLS_2_RUN_ON_CURRENT;
							double childbound = child_to_keep.getBound();
							if (childbound <= _master.getBound() || 
								  childbound < _master.getMinKnownBound())
								child_to_keep=null;  // try next for 1st child
							continue;
						}  // 1st child
						else {
							child = new DBBNode12(ns2, _lvl+1, _startLvl, _startExecTime);
							child._NUM_LVLS_2_RUN_ON_CURRENT = _NUM_LVLS_2_RUN_ON_CURRENT;
						}
            // check if child's bound is better than incumbent
            double childbound = child.getBound();
            if (childbound <= _master.getBound() ||
								childbound < _master.getMinKnownBound())  // not good enough
              continue;
            // speed up processing:
            // record new child incumbent if it exists (may be a partial soln
            // that can be further augmented in step 3 above when it is processed)
            if (child.getCost() > _master.getBound()) {
							_master.setIncumbent(child);
              foundincumbent = true;  // is it needed here?
            }
            children.add(child);
          }
          int sz = children.size();
          if (sz == 0) {
            if (foundincumbent) _master.reduceTightenUpperBoundLvl();
            if (child_to_keep!=null) {
							child_to_keep.run();
						}
						return null; // no children
          }
					// send the children to be executed elsewhere, only if current lvl
					// says so.
					if (_lvl<_startLvl+_NUM_LVLS_2_RUN_ON_CURRENT) {  // keep them locally
						child_to_keep.run();  // run recorded child to keep
						// run rest of children
						for (int i=0; i<sz; i++) {
							DBBNode12 ci = (DBBNode12) children.get(i);
							ci.run();
						}
					}
					else {  // send (some of them) them elsewhere for execution
						// if the children are at limit size, send over the network only 
						// half of them, keeping the rest locally.
						TaskObject[] tasks=null;
						if (sz>=_master.getMaxChildrenNodesAllowed()-1) {
							int sz2 = sz/2;
							tasks = new TaskObject[sz2];
							int cnt=0;
							for (int i=sz-1; cnt<sz2; i--) {
								DBBNode12 ci = (DBBNode12) children.remove(i);
								ci._startLvl = ci._lvl;  // set its start level
								tasks[cnt++] = ci;
							}
						}
						else {
							tasks = new TaskObject[sz];
							for (int i=0; i<sz; i++) {
								DBBNode12 ci = (DBBNode12) children.get(i);
								ci._startLvl = ci._lvl;  // set its start level
								tasks[i] = ci;
							}
							children.clear();
						}
						sz = tasks.length;
						try {
							cond_counter.increment(sz);  // notify condition-counter
						}
						catch (Exception e) {
							e.printStackTrace();
							System.exit(-1);
						}
						try {
							PDAsynchBatchTaskExecutorClt.getInstance().submitWorkFromSameHost(tasks);
						}
						catch (PDAsynchBatchTaskExecutorNWAException e) {  // execute the tasks locally
							mger.msg("DBBNode12.run(): got children back due to workers' UNAVAILABILITY, will run them locally", 1);
							for (int i=0; i<tasks.length; i++) {
								// first seriously increment how many levels to keep descendants locally
								((DBBNode12) tasks[i])._NUM_LVLS_2_RUN_ON_CURRENT = 5*_NUM_LVLS_2_RUN_ON_CURRENT;
								// run each child
								tasks[i].run();
							}
						}
						// run locally the child to keep, after having sent the rest 
						// for distributed computation
						child_to_keep.run();
						// also run any remaining children
						for (int i=0; i<children.size(); i++) {
							DBBNode12 ci = (DBBNode12) children.get(i);
							ci.run();
						}
					}
        }
        catch (Exception e) {  // insanity
          e.printStackTrace();
          System.exit( -1);
        }
      }
      else {  // no branching occurs
        if (foundincumbent) {
          _master.reduceTightenUpperBoundLvl();
          if (_master.getLocalSearch()) {  // perform a local search
						long start_time = System.currentTimeMillis();
            try {
							Set nodeids = new IntSet(_nodeids);
							_master.incrNumDLSPerformed();
              // now do the local search
              DLS dls = new DLS();
              AllChromosomeMakerIntf movesmaker = _master.getNewLocalSearchMovesMaker();
							if (movesmaker==null)  // use default
								movesmaker = new IntSetN1RXPFirstImprovingGraphAllMovesMakerMT(1);
              // AllChromosomeMakerIntf movesmaker = new
              //    IntSetN2RXPGraphAllMovesMaker(1);
							// IntSetN2RXPGraphAllMovesMaker(1) gives better results on G_{|V|,p} random graphs
              IntSetNeighborhoodFilterIntf filter = new
                  GRASPPackerIntSetNbrhoodFilter3(1,_master.getGraph());
              FunctionIntf f = new SetWeightEvalFunction(_master.getGraph());
              HashMap dlsparams = new HashMap();
              dlsparams.put("dls.movesmaker", movesmaker);
              dlsparams.put("dls.x0", nodeids);
              dlsparams.put("dls.numthreads", new Integer(10));  // itc: HERE parameterize asap
              dlsparams.put("dls.maxiters", new Integer(100)); // itc: HERE rm asap
              int n10 = _master.getGraph().getNumNodes()/10 + 1;
              dlsparams.put("dls.intsetneighborhoodmaxnodestotry", new Integer(n10));
              dlsparams.put("dls.graph", _master.getGraph());
							dlsparams.put("dls.lock_graph", Boolean.FALSE);
              dlsparams.put("dls.intsetneighborhoodfilter", filter);
              //dlsparams.put("dls.createsetsperlevellimit", new Integer(100));
              dls.setParams(dlsparams);
              PairObjDouble pod = dls.minimize(f);
              Set sn = (Set) pod.getArg();
              if (sn != null && -pod.getDouble() > getCost()) {
								_nodeids.clear();
								_nodeids.addAll(sn);
                // record new incumbent
                _master.setIncumbent(this);
								_master.incrementTotLeafNodes();
              }
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
			//mger.msg("Done with node "+
			//	       " on lvl="+_lvl+" _startLvl="+_startLvl+
			//	       " _NUM_LVLS_2_RUN_ON_CURRENT="+_NUM_LVLS_2_RUN_ON_CURRENT, 1);
			if (_lvl==_startLvl && cond_counter!=null) {
				try {
					cond_counter.decrement();
				}
				catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			} 
			else if (_lvl==_startLvl) {  // insanity
				System.err.println("null cond_counter???");
				System.exit(-1);
			}
		}
  }


 /**
  * compares this DBB-node with another DBB-node, via the master DBBTree's
  * DBBNodeComparator object for comparing DBB-nodes.
  * @param other Object
  * @return int
  */
 public int compareTo(Object other) {
    DBBNode12 o = (DBBNode12) other;
    return DBBTree.getInstance().getDBBNodeComparator().compare(this, o);
  }

 
  /* debug routine */
  private String printNodes() {
    String res = "[";
    /*Iterator it = getNodes().iterator();
    while (it.hasNext()) {
      Node n = (Node) it.next();
      res += n.getId();
      if (it.hasNext()) res+= ", ";
    }
		*/
		Iterator it = getNodeIds().iterator();
    while (it.hasNext()) {
      Integer nid = (Integer) it.next();
			res += nid.toString();
      if (it.hasNext()) res+= ", ";
    }
		res += "]";
    return res;
  }
}

