package parallel.distributed;

import graph.Graph;
import graph.Node;
import parallel.BoundedBufferArray;
import parallel.TaskObject;
import utils.DataMgr;
import utils.Messenger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


/**
 * class implements the basic functionality of the Apache AirFlow system, ie 
 * allows for the distributed execution of commands on any worker in a cluster,
 * according to the precedence order constraints set forth by a directed acyclic
 * graph (DAG) specified for the tasks at hand. It utilizes basic classes found
 * in this package, such as <CODE>DMsgPassingCoordinator[Clt|Srv]</CODE> and 
 * <CODE>PDAsynchBatchTaskExecutor[Clt|Srv]</CODE>, and the classes in the 
 * <CODE>graph</CODE> package to denote the DAG.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2022</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DDAGTasksScheduler {
	
	/**
	 * before invoking this program, a <CODE>DMsgPassingCoordinatorSrv</CODE> 
	 * and a <CODE>PDAsynchBatchTaskExecutorSrv</CODE> must be running on their 
	 * own JVMs. 
	 * <p>Invoke this program as follows:
	 * <CODE>
	 * java -cp &lt;classpath&gt; parallel.distributed.DDAGTasksScheduler
	 * &lt;graph_file&gt; &lt;commands_file&gt; 
	 * [msgpassingsrvhost(localhost)] [msgpassingsrvport(7894)] 
	 * [pdasynchbtesrvhost(localhost)] [pdasynchbtesrvport(7981)]
	 * </CODE>
	 * where the &lt;graph_file&gt; is the name of a file that can be passed as 
	 * argument to the methods <CODE>utils.DataMgr.readGraphFromFile()</CODE> and
	 * the &lt;commands_file&gt; second argument is a file that can be read
	 * using the <CODE>utils.DataMgr.readPropsFromFile()</CODE> that will return
	 * a map with keys being integers that correspond one-to-one to the node ids
	 * of the graph described in the first argument.
	 * <p>Notice that unless all servers and workers are running on the same 
	 * machine, the &lt;msgpassingsrvhost&gt; variable cannot be "localhost" as 
	 * this would mean that when a task is executed in a worker, it will try to
	 * communicate to a <CODE>DMsgPassingCoordinatorSrv</CODE> object that it will
	 * expect to find running in the worker, which won't be the case. Instead, one
	 * should use the actual IP address of the machine where the msg-passing 
	 * server is running.
	 * @param args String[] must have length at least 2
	 */
	public static void main(String[] args) {
		try {
			// 1. read data
			final Graph g = DataMgr.readGraphFromFile(args[0]);
			final HashMap tasks_map = DataMgr.readPropsFromFile(args[1]);
			final String msgpashost = args.length > 2 ? args[2] : "localhost";
			final int msgpasport = args.length > 3 ? Integer.parseInt(args[3]) : 7894;
			final String pdabtehost = args.length > 4 ? args[4] : "localhost";
			final int pdabteport = args.length > 5 ? Integer.parseInt(args[5]) : 7981;
			
			// 1.5 sanity check: same number of nodes as tasks
			final int n = g.getNumNodes();
			if (n!=tasks_map.size())
				throw new Error("Graph g #nodes ("+n+
					              ") doesn't match #tasks ("+tasks_map.size()+")");
			// 1.5 sanity check: no cycles in graph
			for (int i=0; i<n; i++) {
				final Node ni = g.getNodeUnsynchronized(i);
				double[] costs = g.getAllShortestPaths(ni);  // sequential Dijkstra
				Set incoming_links = ni.getInLinks();
				Iterator lit = incoming_links.iterator();
				while (lit.hasNext()) {
					Integer lid = (Integer) lit.next();
					int st_id = g.getLink(lid.intValue()).getStart();
					if (Double.compare(costs[st_id], Double.POSITIVE_INFINITY)<0)
						throw new Error("Graph g contains cycle...");
				}
			}
			
			// 2. create and connect clients to cluster
			PDAsynchBatchTaskExecutorClt.setHostPort(pdabtehost, pdabteport);
			final PDAsynchBatchTaskExecutorClt apdbteclt = 
				PDAsynchBatchTaskExecutorClt.getInstance();
			
			final DMsgPassingCoordinatorClt msgpassclt = 
				new DMsgPassingCoordinatorClt(msgpashost, msgpasport, 
					                            "DDAGTasksScheduler");
			
			final FinishedJobCollector fjc = new FinishedJobCollector(msgpassclt, n);
			Thread finished_job_collector_thread = new Thread(fjc);
			finished_job_collector_thread.start();
			
			// 3. traverse the graph and send commands for execution when they are 
			//    ready.
			
			BoundedBufferArray buffer = new BoundedBufferArray(n);
			for (int i=0; i<n; i++) {
				final Node ni = g.getNodeUnsynchronized(i);
				if (ni.getInLinks().size()==0) buffer.addElement(ni);
			}

			// main loop!
			while (buffer.size()>0) {
				final Node node = (Node) buffer.remove();
				Set incoming_linkids = node.getInLinks();
				Iterator lit = incoming_linkids.iterator();
				boolean node_ok = true;
				while (lit.hasNext()) {
					Integer lid = (Integer) lit.next();
					int start_nid = g.getLink(lid.intValue()).getStart();
					if (!fjc.isDone(start_nid)) {  // nope, node is not ready yet
						if (!buffer.contains(node))
							buffer.addElement(node);  // add at the "end of the queue"
						node_ok = false;
						Thread.sleep(1);  // sleep for a while before trying next in buffer
						break;
					}
				}
				if (node_ok) {  // ok to submit to cluster for processing
					TaskObject[] tasks = new TaskObject[1];
					final int nid = node.getId();
					final String cmd = 
						(String) tasks_map.get(new Integer(nid).toString());
					tasks[0] = new DAGTask(nid, cmd, msgpashost, msgpasport);
					apdbteclt.submitWorkFromSameHost(tasks);
					// add the descendants to the buffer if they are not already in
					Set outlinks = node.getOutLinks();
					Iterator olit = outlinks.iterator();
					while (olit.hasNext()) {
						Integer lid = (Integer) olit.next();
						int endnodeid = g.getLink(lid).getEnd();
						Node endnode = g.getNodeUnsynchronized(endnodeid);
						if (!buffer.contains(endnode)) buffer.addElement(endnode);
					}
				}
			}
			
			// 4. wait for finishedjobcollector to finish too!
			finished_job_collector_thread.join();
			PDAsynchBatchTaskExecutorClt.disconnect();
			
			// 5. DONE
			System.out.println("DDAGTasksScheduler: ALL tasks completed");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}


/**
 * auxiliary class NOT part of the public API. The class is a runnable object
 * that a separate thread is running that enters an event-loop that listens for
 * incoming data sent by the workers in the cluster upon successfully running 
 * their task. The incoming data is just the node-id of the task they ran. Upon
 * receiving note that the last of the nodes has completed execution, the 
 * <CODE>run()</CODE> method of this object returns and the thread executing it
 * ends. 
 * The class is part of the <CODE>parallel.distributed.DDAGTasksScheduler</CODE>
 * class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2022</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class FinishedJobCollector implements Runnable {
	private final DMsgPassingCoordinatorClt _msgpassclt;
	
	private boolean[] _doneJobs = null;
	
	FinishedJobCollector(DMsgPassingCoordinatorClt clt, int n) {
		_msgpassclt = clt;
		_doneJobs = new boolean[n];  // all inited to false
	}
	
	
	public void run() {
		final Messenger mger = utils.Messenger.getInstance();
		mger.setShowTimeStamp();
		while(true) {
			if (isDone()) return;
			try {
				Integer done_job_id = (Integer) _msgpassclt.recvData(0);
				if (done_job_id < 0) {
					mger.msg("job w/ id="+(-done_job_id-1)+" failed",0);
					System.exit(-1);
				}
				synchronized(this) {
					_doneJobs[done_job_id.intValue()] = true;
				}
			}
			catch (Exception e) {
				mger.msg("Communication with DMsgPassingCoordinator failed",0);
				System.exit(-1);
			}
		}
	}

	
	public synchronized boolean isDone(int jobid) {
		return _doneJobs[jobid];
	}
	
	
	private boolean isDone() {
		for (int i=0; i<_doneJobs.length; i++) 
			if (!_doneJobs[i]) return false;
		return true;
	}
}


/**
 * auxiliary class NOT part of the public API. The class is a wrapper, and it 
 * implements in its <CODE>run()</CODE> method the execution of the relevant 
 * command of the task (passed in as a command-line string that gets executed)
 * and immediately afterwards, sends as data to the msg-passing coordinator 
 * server of this run the id of the task (node) that was just executed.
 * The class is part of the <CODE>parallel.distributed.DDAGTasksScheduler</CODE>
 * class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2022</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DAGTask implements TaskObject {
	private int _nodeid;
	private String _cmd;
	private String _msgpasshost;
	private int _msgpassport;

	
	public DAGTask(int nodeid, String cmd, 
		             String msgpasshost, int msgpassport) {
		_nodeid = nodeid;
		_cmd = cmd;
		_msgpasshost = msgpasshost;
		_msgpassport = msgpassport;
	}
	
	
	public Serializable run() {
		final Messenger mger = utils.Messenger.getInstance();
		mger.setShowTimeStamp();
		try {
			mger.msg("starting job for nodeid="+_nodeid, 0);
			Process p = Runtime.getRuntime().exec(_cmd);
			BufferedReader reader = 
				new BufferedReader(new InputStreamReader(p.getInputStream()));
			String commandOutput = "";
			while (commandOutput != null) {
        commandOutput = reader.readLine();
				if (commandOutput!=null)
					System.out.println(commandOutput);  // print the cmd's output on the
						                                  // worker's output
			}
			p.waitFor();  // wait until command is finished
			mger.msg("job for nodeid="+_nodeid+" completed", 0);
			// command executed, let job collector know
			final DMsgPassingCoordinatorClt msgpassclt = 
				new DMsgPassingCoordinatorClt(_msgpasshost, _msgpassport, 
					                            "DDAGTasksScheduler");
			try {
				msgpassclt.sendData(_nodeid+1,              // myid 
					                  0,                      // toid
														new Integer(_nodeid));  // data
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		catch (Exception e) {
			// command failed, let job collector know about this as well
			mger.msg("job for nodeid="+_nodeid+" failed, with exception "+
				       e.getMessage(), 0);
			// command executed, let job collector know
			try {
				final DMsgPassingCoordinatorClt msgpassclt = 
					new DMsgPassingCoordinatorClt(_msgpasshost, _msgpassport, 
						                            "DDAGTasksScheduler");
				msgpassclt.sendData(_nodeid+1,              // myid 
					                  0,                      // toid
														new Integer(-_nodeid-1));  // negative id means fail
			}
			catch (Exception e2) {
				e2.printStackTrace();
			}			
		}
		return null;
	}
	
	
	/**
	 * always throws unsupported operation exception.
	 * @return boolean
	 * @throws UnsupportedOperationException
	 */
	public boolean isDone() {
		throw new UnsupportedOperationException("not supported");
	}
	
	
	/**
	 * always throws unsupported operation exception.
	 * @param other 
	 * @throws UnsupportedOperationException
	 */
	public void copyFrom(TaskObject other) {
		throw new UnsupportedOperationException("not supported");		
	}
	
}
