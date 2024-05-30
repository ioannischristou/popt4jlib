package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Server class has same functionality as <CODE>PDBatchTaskExecutorSrv</CODE> 
 * but is faster due to the fact that it knows at any point all "free" workers
 * under its "wing". It allows clients or workers to connect to this process and 
 * submit TaskObjects for processing to any of the available workers in the 
 * network of connected workers. 
 * A worker process connects to this process on default port 7890 and by doing 
 * so declares itself available for processing an array of 
 * <CODE>TaskObject</CODE> objects, encapsulated in a 
 * <CODE>TaskObjectsExecutionRequest</CODE>.
 * The server may also become itself client to other servers in the network,
 * and if this is the case, then, whenever another client submits a request,
 * if the workers connected to this server are all busy, it will try submitting
 * the request to each of the other servers to which it is a client (unless
 * the other server is also the client that originated or forwarded the request)
 * until it gets a response.
 * Notice that in this implementation, if a worker fails twice in a sequence to 
 * run two different batch jobs, it is removed from the pool of available 
 * workers, and the connection to it is closed. For details see the method
 * <CODE>PDBTEWListener.runObject(TaskObjectsExecutionRequest req)</CODE>.
 * In fact, here are the full Computing Policies:
 * If a worker connection is lost during processing a batch of tasks, the batch
 * will be re-submitted once more to the next available worker, as soon as such
 * a worker becomes available. Similarly, if a worker fails to process a batch
 * of tasks and returns a <CODE>FailedReply</CODE> object back to this server,
 * the server will attempt one more time to re-submit the batch to another 
 * worker as soon as such a worker becomes available. In case a worker fails 
 * to process two different batches of jobs in sequence, the server drops its
 * connection from this "loser" worker. If the same batch of jobs fails to be 
 * executed by two different workers, the server sends back to the client that
 * submitted the job, a <CODE>FailedReply</CODE> to indicate the job cannot be 
 * successfully completed.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBatchTaskExecutorSrvFaster extends PDBatchTaskExecutorSrv {
	private HashMap _free;     // Map<Socket s, PDBTEWListener listener>


  /**
   * sole public constructor.
   * @param wport int the port workers (PDBatchTaskExecutorWrk) connect to.
   * @param cport int the port clients (PDBatchTaskExecutorClt) connect to.
   */
  public PDBatchTaskExecutorSrvFaster(int wport, int cport) {
		super(wport, cport);
		_free = new HashMap();  // socket s -> PDBTEWListener wl
  }


  /**
   * invoke as:
   * <CODE>
	 * java -cp &lt;claspath&gt; parallel.distributed.PDBatchTaskExecutorSrvFaster 
	 * [workers_port(7890)] [clients_port(7891)] 
	 * [other_server_ip_address otherserver_ip_port]* 
	 * </CODE>.
   * @param args String[]
   */
  public static void main(String[] args) {
    int wport = 7890;  // default port
    int cport = 7891;
    if (args.length>0) {
      try {
        wport = Integer.parseInt(args[0]);
      }
      catch (Exception e) {
        e.printStackTrace();
        usage();
        System.exit(-1);
      }
      if (args.length>1) {
        try {
          cport = Integer.parseInt(args[1]);
        }
        catch (Exception e) {
          e.printStackTrace();
          usage();
          System.exit(-1);
        }
      }
    }
    PDBatchTaskExecutorSrvFaster server = 
			new PDBatchTaskExecutorSrvFaster(wport, cport);
    if (args.length>2) {
      try {
        for (int i = 2; i < args.length; i += 2) {
          String other_host_name = args[i];
          int other_host_port = Integer.parseInt(args[i + 1]);
          server.addOtherServer(other_host_name, other_host_port);
        }
      }
      catch (Exception e) {
        e.printStackTrace();
        usage();
        System.exit(-1);
      }
    }
    try {
      server.run();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.err.println("Server exits due to exception.");
    }
  }


  protected TaskObjectsExecutionResults submitWork(Vector originating_clients, 
		                                               TaskObject[] tasks) 
		throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
    Set workers2rm = new HashSet();  // Set<Socket s> for 
		                                 // (Socket s, PDBTEWListener t) pair
		Socket s = null;
    PDBTEWListener t = null;
		utils.Messenger mger = utils.Messenger.getInstance();
    mger.msg("PDBatchTaskExecutorSrvFaster.submitWork(tasks): "+
			       "finding a free worker connection",2);
    // 1. find a worker (via Round-Robin)
    synchronized (this) {
      int count = 0;
      while (++count<_NUM_ATTEMPTS) {
        workers2rm.clear();
        Iterator sit = _free.keySet().iterator();
        while (sit.hasNext()) {
					s = (Socket) sit.next();
          t = (PDBTEWListener) _free.get(s);
					if (t.isConnectionLost()) {  // bad t
						workers2rm.add(s);
						sit.remove();
						t = null;
						continue;
					}
					else {
						count = _NUM_ATTEMPTS;
						getWorking().add(t);
						sit.remove();
						break;  // found t
					}
        }
        // remove any "lost connections" worker listeners
        Iterator it = workers2rm.iterator();
        while (it.hasNext()) {
					getWorkers().remove(it.next());
				}
      }
    }  // synchronized (this)
    if (t==null) {  // failed to find an available thread
      mger.msg("PDBatchTaskExecutorSrvFaster.submitWork(clients, tasks): "+
				       "no available threads...",1);
      boolean didit = false;
      // no synchronization is needed for the following block of code:
			// try for a number of times, to find a known server and sumbit the work
			// if a server is found, tried, and throws exception, allow another try
      boolean cont_other_srv_attempts = true;
			for (int n=0; n<_NUM_REPEAT_ATTEMPTS && cont_other_srv_attempts; n++) {
				cont_other_srv_attempts = false;
				final Vector otherServers = getOtherKnownServers();
        for (int i=0; i<otherServers.size(); i++) {
          mger.msg("PDBatchTaskExecutorSrvFaster.submitWork(cs,tasks): trying "+
						       (i+1)+
                   " out of "+otherServers.size()+" other servers",2);
          try {
            PDBatchTaskExecutorClt clienti = (PDBatchTaskExecutorClt)
                otherServers.elementAt(i);
            String clientipaddress_port = 
							clienti.getHostIPAddress() + "_" + clienti.getPort();
            if (contains(originating_clients,clientipaddress_port)) {
              mger.msg("PDBatchTaskExecutorSrvFaster.submitWork(cs, tasks): "+
                       "tasks have been created or forwarded from server "+
								       "being checked",1);
              continue;
            }
            mger.msg("PDBatchTaskExecutorSrvFaster.submitWork(cs, tasks): "+
                     "forwarding tasks to: "+clientipaddress_port,1);
            originating_clients.addElement(clientipaddress_port);
            Object[] results = clienti.submitWork(originating_clients, tasks);
            TaskObjectsExecutionResults res = 
							new TaskObjectsExecutionResults(results);
            didit = true;  // not needed
            return res;
          }
          catch (Exception e) {  // failed to get results, try next known srv
            cont_other_srv_attempts=true;
						e.printStackTrace();
          }
        }
      }
      if (!didit)  // failed completely
        throw new PDBatchTaskExecutorException("no available worker or known "+
					                                     "srv could undertake work");
    }
    mger.msg("PDBatchTaskExecutorSrvFaster.submitWork(clients, tasks):"+
			       " found an available worker",2);
    // 2. submit tasks and get back results
    TaskObjectsExecutionRequest req = 
			new TaskObjectsExecutionRequest(originating_clients, tasks);
    mger.msg("PDBatchTaskExecutorSrv.submitWork(tasks): "+
			       "created the TaskObjectsExecutionRequest to send",2);
    RRObject res = submitWork(req, t);
    mger.msg("PDBatchTaskExecutorSrvFaster.submitWork(cs, tasks): "+
			       "finished running submitWork(req,ois,oos)",2);
    synchronized (this) {
      getWorking().remove(t);  // declare worker's availability again
			_free.put(s, t);
    }
    if (res instanceof TaskObjectsExecutionResults)
      return (TaskObjectsExecutionResults) res;
    else {
      throw new PDBatchTaskExecutorException(
				"PDBatchTaskExecutorSrvFaster.submitWork(cs, tasks): "+
				"worker failed to process tasks.");
    }
  }

	
  /**
   * adds a new worker to the network.
   * @param s Socket
   */
  protected synchronized void addNewWorkerConnection(Socket s) {
    try {
      PDBTEWListener lt = new PDBTEWListener(this, s);
      getWorkers().put(s, lt);
			_free.put(s, lt);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

}

