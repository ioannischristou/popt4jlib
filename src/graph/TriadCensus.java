/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package graph;

import popt4jlib.BoolVector;
import java.io.Serializable;
import java.util.*;
import parallel.*;
import utils.DataMgr;

/**
 * This class implements the TriadCensus Algorithm for counting all cardinality
 * 3 sub-graphs in large graphs in parallel. The algorithm is described in 
 * NK Ahmed, J Neville, R Rossi, NG Duffield, TL Willke, "Graphlet Decomposition:
 * Framework, Algorithms and Applications", 2015 (invited to Knowl. Inf. Sys. 
 * from IEEE ICDM). The implementation is standard shared-memory implementation
 * using multiple threads of a ParallelBatchTaskExecutor.
 * There is observed speedup but hardly linear in the number of threads used, 
 * apparently due to the locking required for the results updates.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class TriadCensus {
	final private Graph _g;
	final int _numThreads;
	
	public TriadCensus(Graph g, int num_threads) {
		_g = g;
		_numThreads = num_threads;
	}
	
	
	/**
	 * computes all 3-graphlet frequencies according to the Triad Census algorithm.
	 * @return int[] where result[0] is the g_3_1 graphlet number of 
	 * occurrences (triangle patterns), result[1] is the g_3_2 graphlet number of
	 * occurrences (2-star patterns), result[2] is the g_3_3 graphlet number of
	 * occurrences (3-node-1-edge patterns), and result[3] is the g_3_4 graphlet
	 * number of occurrences (all-node-independent patterns).
	 */
	public long[] getGraphletFrequencies() throws ParallelException {
		long result[] = new long[4];
		final int num_nodes = _g.getNumNodes();
		/*
		int[] x = new int[num_nodes];
		Lock[] node_locks = new Lock[num_nodes];
		for (int i=0; i<num_nodes; i++) {
			node_locks[i] = new Lock();
		}
		*/
		List tasks = new ArrayList();  // List<TCTask>
		for (int i=0; i<_g.getNumArcs(); i++) {
			Link li = _g.getLink(i);
			TCTask tci = new TCTask(li,
				                      //x,node_locks,
				                      result);
			tasks.add(tci);
		}
		ParallelBatchTaskExecutor pbte = ParallelBatchTaskExecutor.newParallelBatchTaskExecutor(_numThreads);
		pbte.executeBatch(tasks);
		result[0] /= 3;
		result[1] /= 2;
		result[3] = c(num_nodes, 3) - result[0] - result[1] - result[2];
		return result;
	}
	
	
	/**
	 * invoke as 
	 * <CODE>java -cp &lt;classpath&gt; graph.TriadCensus &lt;graphfilename&gt; [num_threads(1)]</CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		try {
			Graph g = DataMgr.readGraphFromFile2(args[0]);
			int num_threads = 1;
			if (args.length>1) num_threads = Integer.parseInt(args[1]);
			long start = System.currentTimeMillis();
			TriadCensus tc = new TriadCensus(g,num_threads);
			long[] result = tc.getGraphletFrequencies();
			long dur = System.currentTimeMillis()-start;
			for (int i=0; i<result.length; i++) {
				System.out.println("result["+i+"]="+result[i]);
			}
			System.out.println("Done in "+dur+" msecs using "+num_threads+" threads.");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
	public static void test_c(String[] args) {
		int n = Integer.parseInt(args[0]);
		int k = Integer.parseInt(args[1]);
		System.out.println("C("+n+","+k+")="+c(n,k));
	}
	
	
	private static long c(int n, int k) {
		java.math.BigInteger result = new java.math.BigInteger("1");
		for (int i=k+1; i<=n; i++) {
			result = result.multiply(java.math.BigInteger.valueOf(i));
		}
		int nk=n-k;
		for (int d=2;d<=nk;d++) result = result.divide(java.math.BigInteger.valueOf(d));
		return result.longValue();
	}
	

	/**
	 * this nested inner class implements the main TriadConsensus task described
	 * in the parallel loop of the algorithm. It is important to notice that
	 * the shared array variable X described in the paper must NOT be shared
	 * but instead should be a local variable to the parallel threads of execution.
	 */
	private class TCTask implements TaskObject {
		private final Link _li;
		//private final int[] _x;
		//private final Lock[] _locks;
		private final long[] _result;
		
		public TCTask(Link li, 
			            //int[] x, Lock[] locks, 
			            long[] result) {
			_li = li;
			//_x = x;
			//_locks = locks;
			_result = result;
		}
		public Serializable run() {
			//if (_li.getId()%10000==0) System.out.println("Running edge "+_li.getId()+" (/"+_g.getNumArcs()+")");
			final int nn = _g.getNumNodes();
			BoolVector star_u = new BoolVector(nn);
			BoolVector star_v = new BoolVector(nn);
			BoolVector tri_e = new BoolVector(nn);
			BoolVector x = new BoolVector(nn);  // turn x into a local variable
			int u_id = _li.getStart();
			int v_id = _li.getEnd();
			Node u = _g.getNodeUnsynchronized(u_id);
			Node v = _g.getNodeUnsynchronized(v_id);
			Set u_nbors = u.getNborsUnsynchronized();
			BoolVector un_plus_vn = new BoolVector(nn);
			Iterator it_u = u_nbors.iterator();
			while (it_u.hasNext()) {
				Node w = (Node) it_u.next();
				int w_id = w.getId();
				un_plus_vn.set(w_id);
				if (w_id==v_id) continue;
				star_u.set(w_id);
				//_locks[w_id].getLock();
				//_x[w_id]=1;
				//_locks[w_id].releaseLock();
				x.set(w_id);
			}
			Set v_nbors = v.getNborsUnsynchronized();
			Iterator it_v = v_nbors.iterator();
			while (it_v.hasNext()) {
				Node w = (Node) it_v.next();
				int w_id = w.getId();
				un_plus_vn.set(w_id);
				if (w_id==u_id) continue;
				//_locks[w_id].getLock();
				if (x.get(w_id)) { //if (_x[w_id]==1) {
					tri_e.set(w_id);
					star_u.unset(w_id);
				} else {
					star_v.set(w_id);
				}
				//_locks[w_id].releaseLock();
			}
			// _result updates done inside global lock
			synchronized (TCTask.class) {
				_result[0] += tri_e.cardinality();
				_result[1] += star_u.cardinality()+star_v.cardinality();
				_result[2] += nn - un_plus_vn.cardinality();
			}
			// finally, reset X for u_nbors
			/*
			it_u = u_nbors.iterator();
			while (it_u.hasNext()) {
				Node w = (Node) it_u.next();
				int w_id = w.getId();
				_locks[w_id].getLock();
				_x[w_id] = 0;
				_locks[w_id].releaseLock();
			}
		  */
			// done
			return null;
		}
		public boolean isDone() {
			return true;
		}
		public void copyFrom(TaskObject o) throws IllegalArgumentException {
			throw new IllegalArgumentException("N/A");
		}
	}
	
}
