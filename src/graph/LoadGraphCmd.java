package graph;

import parallel.distributed.OKReplyRequestedPDBTExecWrkInitCmd;
import parallel.distributed.PDBatchTaskExecutorSrv;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * forces hard loading of the graph onto worker's memory (if not already 
 * present).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LoadGraphCmd extends OKReplyRequestedPDBTExecWrkInitCmd {
	private String _graphfilename;
	
	public LoadGraphCmd(String filename) {
		_graphfilename = filename;
	}
	
	
	public void runProtocol(PDBatchTaskExecutorSrv srv, 
		                      ObjectInputStream ois, ObjectOutputStream oos) {
		GraphCacheMgr.getGraphNoRef(_graphfilename);
	}
}

