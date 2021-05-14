package parallel.distributed;

/**
 * command for <CODE>PDBTExecInited[Clt|Srv|Wrk]</CODE> objects. Such commands
 * are sent by clients, to servers, that must then forward them to every 
 * connected worker to be executed.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public abstract class PDBTExecCmd extends RRObject {
	
}
