package graph;

import parallel.ParallelException;
import parallel.TaskObject;
import parallel.distributed.PDBTExecInitedClt;
import popt4jlib.SparseVectorIntf;
import utils.PairIntInt;
import utils.Messenger;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


/**
 * A parallel/distributed version of the "Shortest Path Calculation by Forward-
 * Search" algorithm described on page 308 in Bertsekas &amp; Tsitsiklis, 
 * "Parallel and Distributed Computation: Numerical Methods", Prentice-Hall,
 * 1989 (1st ed). The algorithm works on connected graphs containing nonnegative
 * edge weights.
 * This method should provide good performance when asked to re-optimize a 
 * shortest path computation after some nodes have been removed (assuming we
 * have the previous shortest distances from the source to the destination, and
 * we use it for the next estimates h, by reversing the direction of the graph,
 * and asking for a path on the reverse pair (destination-to-source); in such a 
 * case, the previous values d[j] are under-estimates of the shortest distance 
 * from node j to the new destination (previous source)). This is 
 * what the method <CODE>getKDisjointShortestPaths(s,t,k)</CODE> does after it 
 * computes the first (best) path from s to t. Notice that in the latter method,
 * the reason we alternate between Dijkstra's method and the Forward-Search 
 * method, is simply that Forward Search does not guarantee that the labels it 
 * returns are actually shortest path distances for any node other than the 
 * destination node, and therefore are not guaranteed under-estimates for the
 * distances of nodes to the new destination in the next problem iteration.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DSPPFinderFwdSrch {
	private HashMap _graphestimates = new HashMap();  // map<PairIntInt st,
	                                                  //     double[] h>
	private final Graph _g;
	private final String _graphfilename;
	private final String _pdbthost;
	private final int _pdbtport;
	private final int _minDistrSz;
	private PDBTExecInitedClt _clt;  // cannot be made final
	
	private static long _localOpsCnt=0;
	private static final int _MIN_MULT_FACTOR=4;  // #tasks required per iteration
	                                              // to send tasks distributed
	

	/**
	 * sole public constructor.
	 * @param g Graph
	 * @param graphfilename String the filename describing the graph
	 * @param pdbthost String the ip address of the 
	 * <CODE>parallel.distributed.PDBatchTaskExecutorSrv</CODE> that will accept
	 * the various tasks the algorithm will create
	 * @param pdbtport int the port where the 
	 * <CODE>parallel.distributed.PDBatchTaskExecutorSrv</CODE> server listens.
	 * @param distr_min_size int the minimum size the list L in the execution of
	 * the algorithm should have at any point for it to be distributed.
	 */
	public DSPPFinderFwdSrch(Graph g, String graphfilename,
		                       String pdbthost, int pdbtport, int distr_min_size) {
		_g = g;
		_graphfilename = graphfilename;
		_pdbthost = pdbthost;
		_pdbtport = pdbtport;
		int mds = Integer.MAX_VALUE;
		if (_pdbthost!=null) {
			try {
				_clt = new PDBTExecInitedClt(_pdbthost, _pdbtport);
        // LoadGraphCmd forces the workers to load the graph immediately
				_clt.submitInitCmd(new LoadGraphCmd(graphfilename));  
				mds=distr_min_size;  // set this size to +Inf, so that there will only
				                     // be a single task in each algorithm iteration and
				                     // it will be executed locally
			}
			catch (Exception e) {  // failed, resort to local computations only
				e.printStackTrace();
				_clt=null;
				mds = Integer.MAX_VALUE;
			}
		}
		else {
			_clt=null;
		}
		_minDistrSz = mds;
	}
	
	
	/**
	 * compute the cost of the shortest path between two nodes in the graph passed
	 * in this object as first argument in its constructor. The path will consist
	 * of forward edges in the directed graph only, and the cost is the sum of all
	 * weights of the edges in the path. Only non-negative arc weights are assumed
	 * to exist in the graph. Will return 
	 * <CODE>Double.POSITIVE_INFINITY</CODE> if there is no such path. This method
	 * is thread-safe and may be invoked concurrently with other threads invoking
	 * the same method with different start and destination nodes, or different
	 * under-estimates.
	 * @param s Node start node
	 * @param t Node target node
	 * @param h double[] valid under-estimates of the distance of each node with
	 * id (in [0,..._g.getNumNodes()-1]) i to the target node t. Such a valid 
	 * underestimate is h[i]=0 for all i, and will be assumed if h is null
	 * @return double
	 * @throws IllegalArgumentException if s or t are null
	 */
	public double getShortestPath(Node s, Node t, double[] h) {
		if (s==null | t==null)
			throw new IllegalArgumentException("null source or target node argument");
		utils.Messenger mger = utils.Messenger.getInstance();
		double[] d = new double[_g.getNumNodes()];
		for (int i=0; i<d.length; i++) {
			if (i==s.getId()) d[i]=0.0;
			else d[i]=Double.POSITIVE_INFINITY;
		}
		if (h==null) {
			h = new double[_g.getNumNodes()];  // init. to zero by default
		}
		HashSet l = new HashSet();  // Set<Integer i> is the set L
		l.add(new Integer(s.getId()));
		while (l.size()>0) {
			//mger.msg("working on L="+toString(l),2);  // itc: HERE rm asap
			//mger.msg("d="+toString(d),2);  // itc: HERE rm asap
			//mger.msg("h="+toString(h),2);  // itc: HERE rm asap
			TaskObject last_task=null;
			ArrayList tasksA = new ArrayList();
			ArrayList ids = new ArrayList();
			int lsize=l.size();
			final boolean dcond = lsize>=_MIN_MULT_FACTOR*_minDistrSz;
			Iterator lit = l.iterator();
			while (lit.hasNext()) {
				Integer li = (Integer) lit.next();
				ids.add(li);
				if (ids.size()>=_minDistrSz && dcond) {  // ensure parallelism
					tasksA.add(new DescendantNodesLabelUpdateTask(_graphfilename,d,h,
						                                            ids, t.getId()));
					ids = new ArrayList();
				}
			}
			l.clear();  // reset list L
			if (ids.size()>=_minDistrSz/2 && dcond) {  // send last package over net
				tasksA.add(new DescendantNodesLabelUpdateTask(_graphfilename,d,h,
						                                            ids, t.getId()));
				last_task=null;
			} else if (ids.size()>0) {  // run locally
				// create a separate array dn to pass to the task to run locally
				double[] dn = new double[d.length];
				for (int i=0; i<d.length; i++) dn[i]=d[i];
				last_task = new DescendantNodesLabelUpdateTask(_graphfilename,dn,h,
				                                               ids,t.getId());
			}
			try {
				if (tasksA.size()>0) {
					DescendantNodesLabelUpdateTask[] tasks = 
						(DescendantNodesLabelUpdateTask[]) tasksA.toArray(
						  new DescendantNodesLabelUpdateTask[0]);
					Object[] results = _clt.submitWorkFromSameHost(tasks, 1);  // grain=1
					for (int i=0; i<results.length; i++) {
						if (results[i]!=null) {
							SparseVectorIntf di = (SparseVectorIntf) results[i];
							for (int j=0; j<di.getNumNonZeros(); j++) {
								int posij = di.getIthNonZeroPos(j);
								double vij = di.getCoord(posij);
								if (vij<d[posij]) {
									d[posij]=vij;
									if (posij!=t.getId()) {
										Integer posijI = new Integer(posij);
										l.add(posijI);
									}
								}
							}
						}
						else {
							mger.msg("null response for "+i+"-th task", 2);
							System.exit(-1);  // insanity
						}
					}  // for i in results
				}
				// it is important that local task runs last, so as not to modify d[]
				// before the tasksA have "left the building". In fact, the call
				// last_task.run() may run at any time, but the rest of the loops
				// modifying d[] may not.
				if (last_task!=null) {  // run locally
					SparseVectorIntf di = (SparseVectorIntf) last_task.run();
					for (int j=0; j<di.getNumNonZeros(); j++) {
						int posij = di.getIthNonZeroPos(j);
						double vij = di.getCoord(posij);
						//System.err.println("SV["+posij+"]="+vij);  // itc: HERE rm asap
						if (vij<d[posij]) {
							d[posij]=vij;
							//System.err.println("d["+posij+"]="+vij);  // itc: HERE rm asap
							if (posij!=t.getId()) {
								Integer posijI = new Integer(posij);
								l.add(posijI);
								//System.err.println("adding "+posij+" into List L");
							}
						}
					}
				}  // if last_task!=null								
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1); // itc: HERE rm asap
			}
		}
		synchronized (this) {
			_graphestimates.put(new PairIntInt(s.getId(),t.getId()), d);
		}
		return d[t.getId()];
	}
	
	
	/**
	 * find the k totally disjoint shortest paths from s to t (or as many as they
	 * exist), having no arcs in common with each other.
	 * @param s Node
	 * @param t Node
	 * @param k int
	 * @return List  // List&lt;List&lt;Integer nodeid&gt; &gt; list of up to k
	 * lists containing the paths of node-ids starting from s and leading to t. 
	 * The last element of each path-list will be the actual cost of the path.
	 * The list will be empty if no such path exists
	 * @throws IllegalArgumentException if s or t are null or k&lt;1
	 */
	public synchronized List getKDisjointShortestPaths(Node s, Node t, int k) {
		if (s==null || t==null)
			throw new IllegalArgumentException("null s or t");
		if (k<=0)
			throw new IllegalArgumentException("#paths requested must be >= 1");
		List result = new ArrayList();  // List<List<Integer nodeid> >
		double[] h = null;
		GraphEdgeSetRemovalExecCmd cmd=null;
		Messenger mger = Messenger.getInstance();
		for (int i=0; i<k; i++) {
			double val=Double.POSITIVE_INFINITY;
			if (i%2==0) {  // use Dijkstra's all-shortest-paths label setting method 
				try {
					double[] spps = _g.getAllShortestPaths(s);
					_graphestimates.put(new PairIntInt(s.getId(),t.getId()), spps);
					val = spps[t.getId()];
				}
				catch (GraphException e) {
					e.printStackTrace();
					throw new Error("failed?");
				}
			}
			else val = getShortestPath(s, t, h);
			mger.msg("DSPPFinderFwdSrch.getKDisjointShortestPaths():found "+
				       "shortest-path #"+(i+1)+"/"+k+" with val="+val,2);
			if (Double.isInfinite(val)) break;
			List path = getLastFoundShortestPath(s, t);
			result.add(path);
			h = getBestPathEstimates(s.getId(),t.getId());  // have latest estimates
			// remove the edges used in the path from the graph
			List edgeids2rm = new ArrayList();  // List<Integer linkid>
			for (int j=0; j<path.size()-1; j++) {
				Node nj = _g.getNodeUnsynchronized(((Integer)path.get(j)).intValue());
				Set outlinks = nj.getOutLinks();
				Iterator outit = outlinks.iterator();
				while (outit.hasNext()) {
					Integer lidI = (Integer) outit.next();
					Link l = _g.getLink(lidI.intValue());
					int nextnodeid = l.getEnd();
					if (nextnodeid==((Integer)path.get(j+1)).intValue()) {  // found it
						edgeids2rm.add(lidI);
						try {
							_g.setInfiniteLinkWeight(lidI.intValue());
						}
						catch (ParallelException e) {
							e.printStackTrace();
							throw new IllegalStateException("while setting max-weight to "+
								                              "edges, another thread has "+
								                              "read-access to graph?");
						}
						break;  // assume there is a single edge connecting any two nodes
						        // in the same direction
					}  // if edge is the one in the last path
				}  // while iterate over nj out-links
			}  // for j in last path found
			try {
				_g.reverseLinksDirection();
				// start and end node are now reverse
				Node tmp=s;
				s=t;
				t=tmp;
			}
			catch (ParallelException e) {
				e.printStackTrace();
			}
			// remove edges from worker nodes if running distributed
			if (_clt!=null && i<k-1) {
				if (cmd!=null) {  
					// since the tasks may be sent to new workers that have not 
					// executed the previous cmd, it's necessary that the new 
					// cmd sent out, removes all edges that have been followed on
					// all previous paths till now
					edgeids2rm.addAll(cmd.getEdgeIds2Rm());
				}
				cmd = 
					new GraphEdgeSetRemovalExecCmd(_graphfilename, edgeids2rm);
				try {
					_clt.submitCmd(cmd);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new IllegalStateException("submitting edges-removal "+
						                              "request to server failed");
				}
			}  // if _clt!=null && i<k-1
			path.add(new Double(val));  // add as last element of list the path-cost
		}  // for i 0 to k
		return result;
	}
	
	
	/**
	 * get the actual shortest path from source to destination, as computed by 
	 * a previous call to <CODE>getShortestPath(s,t,h)</CODE>.
	 * @param s Node
	 * @param t Node
	 * @return List  // List&lt;Integer nodeid&gt; starting with s.getId(), and
	 * ending with t.getId() if a shortest path exists
	 * @throws IllegalArgumentException if s or t are null
	 * @throws IllegalStateException if no path exists or if no previous call to
	 * <CODE>getShortestPath()</CODE> has been made.
	 */
	public synchronized List getLastFoundShortestPath(Node s, Node t) {
		if (s==null || t==null) 
			throw new IllegalArgumentException("null argument passed in");
		double[] d = getBestPathEstimates(s.getId(),t.getId());
		if (d==null || Double.isInfinite(d[t.getId()])) 
			throw new IllegalStateException("no path has been found from "+
				                              s.getId()+" to "+t.getId());
		List result = new ArrayList();  // List<Integer>
		result.add(new Integer(t.getId()));
		Node n = t;
		double ncost = d[t.getId()];
		int sid = s.getId();
		int count=0;
		while (n.getId()!=sid) {
			++count;
			Set inlinkids = n.getInLinks();
			Iterator init = inlinkids.iterator();
			boolean found=false;
			while (init.hasNext()) {
				int lid = ((Integer) init.next()).intValue();
				Link l = _g.getLink(lid);
				int n_s_id = l.getStart();
				double dnsidplw = d[n_s_id]+l.getWeight();
				if (Double.compare(dnsidplw,ncost)<=0) {
					result.add(new Integer(n_s_id));
					ncost = d[n_s_id];
					n = _g.getNodeUnsynchronized(n_s_id);
					found=true;
					break;
				}
			}
			if (!found) {  // insanity
				throw new IllegalStateException("cannot compute path from "+n.getId()+
					                              " with d[nid]="+ncost+
					                              " (result.size()="+result.size()+")");
			}
		}
		// reverse result
		Collections.reverse(result);
		return result;
	}
	
	
	/**
	 * get the best path estimates from the last run having as source id and 
	 * destination id the given arguments.
	 * @param srcid int
	 * @param destid int
	 * @return double[] may be null
	 */
	public synchronized double[] getBestPathEstimates(int srcid, int destid) {
		return (double[]) _graphestimates.get(new PairIntInt(srcid, destid));
	}
	
	
	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; graph.DSPPFinderFwdSrch 
	 * &lt;graphfilename&gt; 
	 * &lt;startnodeid&gt;
	 * &lt;endnodeid&gt;
	 * [num_disjoint_paths2find(1)]
	 * [run_distributed?(false)]
	 * [pdbthost(localhost)] [pdbtport(7891)] [distr_min_size(Integer.MAX_VALUE)]
	 * </CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		String filename = args[0];
		Graph g=null;
		long st=System.currentTimeMillis();
		try {
			g = GraphCacheMgr.getGraphNoRef(filename);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		long fdur = System.currentTimeMillis()-st;
		System.err.println("Reading Graph from file took "+fdur+" msecs");
		int s = Integer.parseInt(args[1]);
		Node sn = g.getNode(s);
		int t = Integer.parseInt(args[2]);
		Node tn = g.getNode(t);
		int k=1;
		if (args.length>3) {
			k = Integer.parseInt(args[3]);
		}
		boolean run_distr = false;
		if (args.length>4) {
			String rd = args[4];
			if (rd.toLowerCase().startsWith("t")) 
				run_distr=true;
		}
		String pdbthost=run_distr ? "localhost" : null;
		if (args.length>5) pdbthost=args[5];
		int pdbtport=7891;
		if (args.length>6) pdbtport = Integer.parseInt(args[6]);
		int distr_min_size = Integer.MAX_VALUE;
		if (args.length>7) 
			distr_min_size = Integer.parseInt(args[7]);
		DSPPFinderFwdSrch spp = new DSPPFinderFwdSrch(g, filename, 
			                                            pdbthost, pdbtport, 
			                                            distr_min_size);
		long start = System.currentTimeMillis();
		List paths = spp.getKDisjointShortestPaths(sn, tn, k);
		long dur = System.currentTimeMillis()-start;
		for (int i=0;i<paths.size(); i++) {
			List pk = (List)paths.get(i);
			Double val = (Double)pk.get(pk.size()-1);
			System.out.println("path-"+i+" consists of "+toString(pk)+
				                 " and has val="+val.doubleValue());
		}
		System.out.println("time to getKDisjointShortestPaths()="+dur+" msecs.");
		if (!run_distr) {
			System.out.println("total #operations="+getLocalOpsCounter());
		}
		try {
			if (spp._clt!=null) 
				spp._clt.terminateConnection();
		}
		catch (Exception e) {
			e.printStackTrace();  // no-op
		}
	}
	
	
	// following two methods are only useful when running in non-distributed-mode
	static synchronized void incrLocalOpsCounter(long num) {
		_localOpsCnt += num;
	}
	static synchronized long getLocalOpsCounter() { return _localOpsCnt; }
	
	
	static String toString(List l) {
		String res="[";
		for (int i=0; i<l.size()-1; i++) {
			Object oi = l.get(i);
			res += oi.toString();
			if (i<l.size()-2) res += ",";
		}
		res += "]";
		return res;
	}
		
}


/**
 * helper auxiliary class for DSPPFinderFwdSrch is not part of the public API. 
 * Objects of this class are created by the "master" class DSPPFinderFwdSrch, 
 * and they are light-weight enough so as to be possible to be quickly 
 * transferred between JVMs in a parallel/distributed version of the 
 * forward-search method for the Shortest Path problem.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class DescendantNodesLabelUpdateTask implements TaskObject {
	private double[] _d;  // double[num_nodes]
	private double[] _h;  // double[num_nodes]
	private List _ids;  // List<Integer> the ids of the nodes to "expand"
	private int _t;  // the id of the destination node
	private String _graphfilename;
	private boolean _isDone=false;
	
	
	/**
	 * sole constructor.
	 * @param filename the Graph filename which is needed to load a graph from 
	 * disk if it's not already loaded
	 * @param d double[] the current labels of the nodes
	 * @param h double[] a valid under-estimate of the node distances from dest
	 * @param ids List  // List&lt;Integer&gt; the ids of the node being tested
	 * @param dest int the destination node id
	 */
	public DescendantNodesLabelUpdateTask(String filename, 
		                                    double[] d, double[] h, 
																				List ids, int dest) {
		_graphfilename = filename;
		_d = d;
		_h = h;
		_ids = ids;
		_t = dest;
	}
	
	
	/**
	 * computes those (out-going) neighbors of nodes in _ids that can be reached 
	 * from source node with a shorter path than currently estimated, and return 
	 * those values in a sparse vector.
	 * @return Serializable  // SparseVectorIntf
	 */
	public Serializable run() {
		SparseVectorIntf v = 
			new popt4jlib.DblArray1SparseVector(_d.length, Double.POSITIVE_INFINITY);
		Graph g = GraphCacheMgr.getGraphNoRef(_graphfilename);
		long num_ops = 0;
		for (int i=0; i<_ids.size(); i++) {
			int li = ((Integer)_ids.get(i)).intValue();
			Node ni = g.getNodeUnsynchronized(li);
			Set outlinkids = ni.getOutLinks();
			Iterator it = outlinkids.iterator();
			while (it.hasNext()) {
				Integer outlinkid = (Integer) it.next();
				Link l = g.getLink(outlinkid.intValue());
				int j = l.getEnd();
				double aij = l.getWeight();
				++num_ops;
				double vij = _d[li]+aij;
				if (vij<Math.min(_d[j],_d[_t]-_h[j])) {
					// _d[j]=_d[li]+aij;
					if (vij<v.getCoord(j)) {
						try {
							v.setCoord(j, vij);
						}
						catch (ParallelException e) {
							e.printStackTrace();
							// can't get here
						}
					}
				}
			}
		}
		DSPPFinderFwdSrch.incrLocalOpsCounter(num_ops);
		synchronized(this) {
			_isDone=true;
		}
	  return v;
	}
	
	
	/**
	 * always throws.
	 * @param other TaskObject
	 * @throws IllegalArgumentException 
	 */
  public void copyFrom(TaskObject other) throws IllegalArgumentException {
    throw new IllegalArgumentException("not supported");
  }


	/**
	 * returns true only after the <CODE>run()</CODE> method completes execution.
	 * @return boolean
	 */
  public synchronized boolean isDone() {
    return _isDone;
  }	
}
