package parallel.distributed;

import java.io.*;

/**
 * no-op command for <CODE>PDBTExecInited[Clt|Srv|Wrk]</CODE> objects. Such 
 * commands are sent by clients, to servers, that must then forward them to 
 * every connected worker to be executed. This command is a no-op, and its only
 * purpose is to verify to the client submitting it, that all workers currently 
 * in the network have finished running the first initialization command.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBTExecNoOpCmd extends PDBTExecCmd {
	
	/**
	 * no-op.
	 * @param srv PDBatchTaskExecutor unused
	 * @param ois ObjectInputStream unused
	 * @param oos ObjectOutputStream unused
	 */
	public void runProtocol(PDBatchTaskExecutorSrv srv, 
		                      ObjectInputStream ois, ObjectOutputStream oos) {
		// no-op
	}
	
	/**
	 * return the String "PDBTExecNoOpCmd".
	 * @return String
	 */
	public String toString() {
		return "PDBTExecNoOpCmd";
	}
}
