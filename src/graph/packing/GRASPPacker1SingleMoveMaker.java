package graph.packing;

import popt4jlib.*;
import graph.*;
import parallel.*;
import java.util.*;
import popt4jlib.LocalSearch.DLS;
import popt4jlib.LocalSearch.IntSetNeighborhoodFilterIntf;
import utils.IntSet;
import utils.PairObjDouble;


/**
 * class is an implementation of the <CODE>AllChromosomeMakerClonableIntf</CODE> 
 * interface for use with the Maximum Weighted Independent Set (MWIS) problem 
 * solver <CODE>DBBGASPPacker</CODE>. It implements a simple approach to local-
 * search: it removes the top-n nodes (those having maximum connectivity to 
 * other nodes) from the current solution, and then calls the 
 * <CODE>GRASPPacker1.pack()</CODE> method to produce a new solution that it 
 * returns as the single "move" to make. In case the packing produced a better
 * solution, the R2XP local search strategy is also kicked off to see if it can
 * further improve on the solution.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class GRASPPacker1SingleMoveMaker 
  implements AllChromosomeMakerClonableIntf {
	
	private final int _n;  // number of top nodes to remove from solution
	
	// statistics gathering per JVM
	private static int _numImprovements=0;  
	private static int _numFailures=0;
	
	
	/**
	 * no-arg constructor sets the number of top-nodes to remove from initial 
	 * solution (<CODE>_n</CODE>) to 5.
	 */
	public GRASPPacker1SingleMoveMaker() {
		_n = 5;
	}
	
	
	/**
	 * constructor sets the number of top-nodes to remove from initial solution.
	 * @param n int
	 */
	public GRASPPacker1SingleMoveMaker(int n) {
		_n = n;
	}
	

	/**
	 * main method for the class, removes the specified number of top-nodes from
	 * the solution provided in the 1st argument, and then calls the 
	 * <CODE>GRASPPacker1.pack()</CODE> method to create the new solution. If the
	 * result is success (improves upon the initial solution), then the R2PX local
	 * search kicks in to try to further improve the already improved solution.
	 * @param chromosome Object // Set&lt;Integer&gt;
	 * @param params HashMap // Map&lt;String key, Object val&gt;
	 * @return Vector // Vector&lt;Set&lt;Integer&gt;&gt; with a single solution
	 * @throws OptimizerException 
	 */
	public Vector createAllChromosomes(Object chromosome, HashMap params) 
		throws OptimizerException {
    if (chromosome==null) throw new OptimizerException(
				"GRASPPacker1SingleMoveMaker.createAllChromosomes(): "+
				"null chromosome");
		Vector result = new Vector();  // Vector<Set<Integer> >
		Graph g = (Graph) params.get("dls.graph");
		Set nodeids = (Set) chromosome;
		Set init_nodes = new TreeSet(new NodeComparator4());  // heavier comes first
		Iterator nids_it = nodeids.iterator();
		while (nids_it.hasNext()) {
			Integer nid = (Integer) nids_it.next();
			init_nodes.add(g.getNodeUnsynchronized(nid.intValue()));
		}
		// remove up to _n nodes from init_nodes
		Iterator in_it = init_nodes.iterator();
		for (int i=0; i<_n && in_it.hasNext(); i++) {
			in_it.next();
			in_it.remove();
		}
		GRASPPacker1 packer = null;
		try {
			packer = new GRASPPacker1(g);
		}
		catch (ParallelException e) {
			// can never get here
		}
		try {
			packer.setAlphaFactor(0.8);  // set a low alpha for more candidates
			Set new_nodes = packer.pack(init_nodes);
			Set rs = new HashSet();  // Set<Integer>
			SetWeightEvalFunction f = new SetWeightEvalFunction(g);  // itc: HERE rm asap
			double init_val = f.eval(nodeids, null);  // itc: HERE rm asap
			Iterator nn_it = new_nodes.iterator();
			while (nn_it.hasNext()) {
				rs.add(new Integer(((Node)nn_it.next()).getId()));
			}
			result.add(rs);
			double new_val = f.eval(rs, null);
			if (true) {
				if (new_val<init_val) {
					synchronized (GRASPPacker1SingleMoveMaker.class) {
						++_numImprovements;
					}
					// go for the extra R2XP local search too!
          // convert s to Set<Integer>
          Set nodeids2 = new IntSet();
          Iterator iter = new_nodes.iterator();
          while (iter.hasNext()) {
            Node n = (Node) iter.next();
            Integer nid = new Integer(n.getId());
            nodeids2.add(nid);
          }
          // now do the local search
          DLS dls = new DLS();
          AllChromosomeMakerIntf movesmaker = new IntSetN2RXPGraphAllMovesMaker(1);
          IntSetNeighborhoodFilterIntf filter = new GRASPPackerIntSetNbrhoodFilter2(1);
          HashMap dlsparams = new HashMap();
          dlsparams.put("dls.movesmaker",movesmaker);
          dlsparams.put("dls.x0", nodeids2);
          dlsparams.put("dls.numthreads", new Integer(4));
          dlsparams.put("dls.maxiters", new Integer(10));   // itc: HERE rm asap
          dlsparams.put("dls.graph", g);
          dlsparams.put("dls.intsetneighborhoodfilter", filter);
          //dlsparams.put("dls.createsetsperlevellimit", new Integer(100));
          dls.setParams(dlsparams);
          PairObjDouble pod = dls.minimize(f);
          Set sn = (Set) pod.getArg();
          if (sn!=null) {
            new_nodes.clear();
            Iterator sniter = sn.iterator();
            while (sniter.hasNext()) {
              Integer id = (Integer) sniter.next();
              Node n = g.getNodeUnsynchronized(id.intValue());
              new_nodes.add(n);
            }
          }
					double new_val2 = pod.getDouble();
					if (new_val2 < new_val) {
						result.clear();
						rs.clear();
						Iterator nn_it2 = new_nodes.iterator();
						while (nn_it2.hasNext()) {
							rs.add(new Integer(((Node)nn_it2.next()).getId()));
						}
						result.add(rs);
					}
					// end local search
				} else {
					synchronized (GRASPPacker1SingleMoveMaker.class) { 
						++_numFailures;
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new OptimizerException("createAllChromosomes() failed");
		}
		return result;
	}
	
	
	/**
	 * return a new <CODE>GRASPPacker1SingleMoveMaker</CODE> instance, with the
	 * right <CODE>_n</CODE> value.
	 * @return GRASPPacker1SingleMoveMaker
	 */
	public AllChromosomeMakerClonableIntf newInstance() {
		return new GRASPPacker1SingleMoveMaker(_n);
	}
	
	
	public static synchronized int getNumImprovements() { 
		return _numImprovements; 
	}
	public static synchronized int getNumFailures() { 
		return _numFailures; 
	}

}

