package parallel.distributed;

import parallel.TaskObject;
import java.io.*;


/**
 * command for <CODE>PDBTExecInited[Clt|Srv|Wrk]</CODE> objects. Such commands
 * are sent by clients, to servers, that must then forward them to every 
 * connected worker, who in turn ask their thread-pool to execute the object's
 * <CODE>run()</CODE> method on each of its threads sequentially and atomically.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public abstract class PDBTExecOnAllThreadsCmd extends PDBTExecCmd 
                                              implements TaskObject {

	/**
	 * no-op as the method should never be executed.
	 * @param srv
	 * @param ois
	 * @param oos 
	 */
	public void runProtocol(PDBatchTaskExecutorSrv srv, 
		                      ObjectInputStream ois, ObjectOutputStream oos) {
		// no-op
	}
}
