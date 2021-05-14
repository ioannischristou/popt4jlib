package graph;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import parallel.distributed.PDBTExecCmd;
import java.util.List;
import parallel.ParallelException;
import parallel.distributed.PDBatchTaskExecutorSrv;

/**
 * auxilary class (not part of the public API despite the class status) 
 * encapsulates request to modify an existing graph by removing a set of links
 * from this graph. Used when running the <CODE>DSPPFinderFwdSrch</CODE> 
 * algorithm in a distributed computing environment.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class GraphEdgeSetRemovalExecCmd extends PDBTExecCmd {
	private List _linkids2rm;  // List<Integer linkid>
	private String _graphfilename;
	
	/**
	 * sole constructor.
	 * @param graphfilename String
	 * @param linkids2rm List  // List&lt;Integer linkid&gt;
	 */
	public GraphEdgeSetRemovalExecCmd(String graphfilename, List linkids2rm) {
		_graphfilename=graphfilename;
		_linkids2rm = linkids2rm;
	}
	
	
	/**
	 * return the _linkids2rm data member.
	 * @return List  // List&lt;Integer linkid&gt;
	 */
	public List getEdgeIds2Rm() {
		return _linkids2rm;
	}
	
	
	/**
	 * sets all link weights for the links specified in constructor to +Infinity.
	 * ALSO, reverses the direction of all arcs in the graph, so that the values
	 * d[j] found in the last call to 
	 * <CODE>DSPPFinderFwdSrch.getShortestPath(s,t,h)</CODE> can be properly used
	 * as h[] values for the next call to this method.
	 * @param srv PDBatchTaskExecutorSrv unused
	 * @param ois ObjectInputStream unused
	 * @param oos ObjectOutputStream unused
	 */
	public void runProtocol(PDBatchTaskExecutorSrv srv, 
		                      ObjectInputStream ois, 
													ObjectOutputStream oos) {
		Graph g = GraphCacheMgr.getGraphNoRef(_graphfilename);
		utils.Messenger.getInstance().msg(
			"GraphEdgeSetRemovalExecCmd.runProtocol(): Removing a total of "+
			_linkids2rm.size()+" edges from graph", 0);
		for (int i=0; i<_linkids2rm.size(); i++) {
			int lid = ((Integer)_linkids2rm.get(i)).intValue();
			try {
				g.setInfiniteLinkWeight(lid);
			}
			catch (ParallelException e) {
				e.printStackTrace();
			}
		}
		try {
			g.reverseLinksDirection();
		} 
		catch (ParallelException e) {
			e.printStackTrace();
		}

	}
}
