package parallel.distributed;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * class encapsulates the request from a client to a server for the number of 
 * connected workers to the server. Not part of the public API despite its 
 * public status.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBTNumWorkersQueryCmd extends PDBTExecCmd {
  public void runProtocol(PDBatchTaskExecutorSrv srv,
                          ObjectInputStream ois,
                          ObjectOutputStream oos) {
		// no-op
	}
	
}
