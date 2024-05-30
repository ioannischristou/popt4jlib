package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Server class allows clients or workers to connect to this process and submit
 * TaskObjects for processing to any of the available workers in the network of
 * connected workers. A worker process connects to this process on default port
 * 7890 and by doing so declares itself available for processing an array of
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
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBatchTaskExecutorSrv {
  private HashMap _workers;  // map<Socket s, PDBTEW[2]Listener listener>
  private HashSet _working;  // Set<PDBTEW[2]Listener listener>
  private int _workersPort = 7890;  // default port
  private int _clientsPort = 7891;  // default port
  protected static final int _NUM_ATTEMPTS = 10;  // num attempts to iterate over
                                                // available worker connections
                                                // to try to find an idle one.
	protected static final int _NUM_REPEAT_ATTEMPTS = 2;  // num attempts to iterate
	                                                    // over other known srvrs
  private Vector _otherKnownServers;  // Vector<PDBatchTaskExecutorClt>


  /**
   * sole public constructor.
   * @param wport int the port workers (PDBatchTaskExecutorWrk) connect to.
   * @param cport int the port clients (PDBatchTaskExecutorClt) connect to.
   */
  public PDBatchTaskExecutorSrv(int wport, int cport) {
    _workers = new HashMap();
    _working = new HashSet();
    _workersPort = wport;
    _clientsPort = cport;
    _otherKnownServers = new Vector();
  }


  /**
   * invoke as:
   * <CODE>
	 * java -cp &lt;classpath&gt; parallel.distributed.PDBatchTaskExecutorSrv 
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
    PDBatchTaskExecutorSrv server = new PDBatchTaskExecutorSrv(wport, cport);
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


  protected void run() throws IOException {
    WThread workersListeningThread = new WThread(_workersPort);
    workersListeningThread.start();
    CThread clientsListeningThread = new CThread(_clientsPort);
    clientsListeningThread.start();
  }


	/**
	 * get the port listening for worker connections.
	 * @return int
	 */
	protected int getWorkersPort() {
		return _workersPort;
	}
	
	
	/**
	 * get the port listening for client connections.
	 * @return int
	 */
	protected int getClientsPort() {
		return _clientsPort;
	}
	
	
	/**
	 * return the <CODE>_workers</CODE> hash-table of the currently known 
	 * connected workers to this server.
	 * @return HashMap // map&lt;Socket s, PDBTEW[2]Listener listener&gt;
	 */
	protected HashMap getWorkers() {
		return _workers;
	}
	
	
	/**
	 * return the current number of workers connected to this server.
	 * @return int
	 */
	protected synchronized int getNumWorkers() {
		return _workers.size();
	}
	
	
	/**
	 * return the <CODE>_working</CODE> hash-set of the workers that are currently
	 * known to this server to be busy.
	 * @return HashSet  // Set&lt;PDBTEW[2]Listener listener&gt;
	 */
	protected HashSet getWorking() {
		return _working;
	}
	
	
  /**
   * only called from <CODE>main(args)</CODE> at startup.
   * @param host String
   * @param port int
   */
  protected synchronized void addOtherServer(String host, int port) {
    PDBatchTaskExecutorClt othersrv = new PDBatchTaskExecutorClt(host, port);
    _otherKnownServers.addElement(othersrv);
  }
	
	
	/**
	 * return the <CODE>_otherKnownServers</CODE> vector of the other known 
	 * server clients to this server.
	 * @return Vector  // Vector&lt;PDBatchTaskExecutorClt&gt;
	 */
	protected Vector getOtherKnownServers() {
		return _otherKnownServers;
	}


  protected TaskObjectsExecutionResults submitWork(Vector originating_clients, 
		                                               TaskObject[] tasks) 
		throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
    Set workers2rm = new HashSet();  // Set<Socket s> for 
		                                 // (Socket s, PDBTEWListener t) pair
    PDBTEWListener t = null;
		utils.Messenger mger = utils.Messenger.getInstance();
    mger.msg("PDBatchTaskExecutorSrv.submitWork(tasks): "+
			       "finding an available worker connection",2);
    // 1. find a worker (via Round-Robin)
    synchronized (this) {
      int count = 0;
      while (++count<_NUM_ATTEMPTS) {
        workers2rm.clear();
        Iterator sit = _workers.keySet().iterator();
        while (sit.hasNext()) {
          Socket s = (Socket) sit.next();
          t = (PDBTEWListener) _workers.get(s);
          boolean is_avail = t.getAvailability() && !_working.contains(t);
          // the _working set is needed so that submitWork() is guaranteed not
          // to choose twice the same worker before the worker is done with the
          // first request.
          if (is_avail) {
            count = _NUM_ATTEMPTS;  // break out of the top-level while-loop too
            _working.add(t);
            break;
          }
          else {
            if (t.isConnectionLost()) workers2rm.add(s);  // used to be add(t)
            t = null; // reset to null
          }
        }
        // remove any "lost connections" worker listeners
        Iterator it = workers2rm.iterator();
        while (it.hasNext()) _workers.remove(it.next());
      }
    }  // synchronized (this)
    if (t==null) {  // failed to find an available thread
      mger.msg("PDBatchTaskExecutorSrv.submitWork(tasks): "+
				       "no available threads...",1);
      boolean didit = false;
      // no synchronization is needed for the following block of code:
			// try for a number of times, to find a known server and sumbit the work
			// if a server is found, tried, and throws exception, allow another try
      boolean cont_other_srv_attempts = true;
			for (int n=0; n<_NUM_REPEAT_ATTEMPTS && cont_other_srv_attempts; n++) {
				cont_other_srv_attempts = false;
        for (int i=0; i<_otherKnownServers.size(); i++) {
          mger.msg("PDBatchTaskExecutorSrv.submitWork(tasks): trying "+(i+1)+
                   " out of "+_otherKnownServers.size()+" other servers",2);
          try {
            PDBatchTaskExecutorClt clienti = (PDBatchTaskExecutorClt)
                _otherKnownServers.elementAt(i);
            String clientipaddress_port = 
							clienti.getHostIPAddress() + "_" + clienti.getPort();
            if (contains(originating_clients,clientipaddress_port)) {
              mger.msg("PDBatchTaskExecutorSrv.submitWork(tasks): "+
                       "tasks have been created or forwarded from server "+
								       "being checked",1);
              continue;
            }
            mger.msg("PDBatchTaskExecutorSrv.submitWork(tasks): "+
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
    mger.msg("PDBatchTaskExecutorSrv.submitWork(tasks):"+
			       " found an available worker",2);
    // 2. submit tasks and get back results
    TaskObjectsExecutionRequest req = 
			new TaskObjectsExecutionRequest(originating_clients, tasks);
    mger.msg("PDBatchTaskExecutorSrv.submitWork(tasks): "+
			       "created the TaskObjectsExecutionRequest to send",2);
    RRObject res = submitWork(req, t);
    mger.msg("PDBatchTaskExecutorSrv.submitWork(tasks): "+
			       "finished running submitWork(req,ois,oos)",2);
    synchronized (this) {
      _working.remove(t);  // declare worker's availability again
    }
    if (res instanceof TaskObjectsExecutionResults)
      return (TaskObjectsExecutionResults) res;
    else {
      throw new PDBatchTaskExecutorException(
				"PDBatchTaskExecutorSrv.submitWork(tasks): "+
				"worker failed to process tasks.");
    }
  }


  protected TaskObjectsExecutionResults submitWork(
		TaskObjectsExecutionRequest req, PDBTEWListener t)
      throws IOException, PDBatchTaskExecutorException {
    utils.Messenger mger = utils.Messenger.getInstance();
		mger.msg("PDBatchTaskExecutorSrv.submitWork(req,t): sending request",2);
    TaskObjectsExecutionResults res = t.runObject(req);
    mger.msg("PDBatchTaskExecutorSrv.submitWork(req,t): response received",2);
    return res;
  }


  /**
   * adds a new worker to the network.
   * @param s Socket
   */
  protected synchronized void addNewWorkerConnection(Socket s) {
    try {
      PDBTEWListener lt = new PDBTEWListener(this, s);
      _workers.put(s, lt);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * adds a new client to the network. The difference between clients and
   * workers is that "clients" submit jobs to the network, but don't want to
   * "get" any job to do themselves. Workers may also submit jobs to the network
   * but are also available to run tasks themselves as well.
   * @param s Socket
   */
  private synchronized void addNewClientConnection(Socket s) {
    try {
      PDBTECListenerThread lt = new PDBTECListenerThread(this, s);
      lt.start();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }


  static void usage() {
    System.err.println("usage: java -cp <classpath> "+
			                 "parallel.distributed.PDBatchTaskExecutorSrv "+
			                 "[workersport] [clientsport] "+
			                 "[other_srv_ipaddress,other_srv_port ]*");
  }


  static boolean contains(Vector clients, String cname) {
    if (clients==null) return false;
    for (int i=0; i<clients.size(); i++) {
      String ci = (String) clients.elementAt(i);
      if (ci!=null && ci.equals(cname)) return true;
    }
    return false;
  }


  /**
   * auxiliary inner class used for listening for incoming worker connections
   * and creating new PDBTEWListener objects to handle each connection.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class WThread extends Thread {
    int _port;

    private WThread(int port) {
      _port = port;
    }


    public void run() {
			utils.Messenger mger = utils.Messenger.getInstance();
      try {
        ServerSocket ss = new ServerSocket(_port);
        mger.msg("Srv: Now Accepting Worker Connections",0);
        while (true) {
          try {
            Socket s = ss.accept();
            mger.msg("Srv: New Worker Added to the Network",0);
            addNewWorkerConnection(s);
            mger.msg("Srv: finished adding new worker connection to _workers",
							       0);
          }
          catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * auxiliary inner class used for listening for incoming client connections
   * and creating new PDBTECListenerThread threads to handle each connection.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class CThread extends Thread {
    private int _port;

    private CThread(int port) {
      _port = port;
    }

    public void run() {
			utils.Messenger mger = utils.Messenger.getInstance();
      try {
        ServerSocket ss = new ServerSocket(_port);
        mger.msg("Srv: Now Accepting Client Connections",0);
        while (true) {
          try {
            Socket s = ss.accept();
            mger.msg("Srv: New Client Added to the Network",0);
            addNewClientConnection(s);
            mger.msg("Srv: finished adding new client connection",0);
          }
          catch (Exception e) {
            mger.msg("Srv.CThread.run(): caught exception '"+e+
							       "' that will be ignored.",0);
          }
        }
      }
      catch (IOException e) {
        mger.msg("Srv.CThread.run() caught IOException '"+e+
					       "' while creating ServerSocket, thread exits.",0);
      }
    }
  }


  /**
   * auxiliary inner class. Not part of the public API.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class PDBTECListenerThread extends Thread {
    private Socket _s;
    private ObjectInputStream _ois;
    private ObjectOutputStream _oos;
    private PDBatchTaskExecutorSrv _srv;
    private boolean _isAvail = true;

    private PDBTECListenerThread(PDBatchTaskExecutorSrv srv, Socket s) 
      throws IOException {
      _srv = srv;
      _s = s;
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
    }


    public void run() {
			utils.Messenger mger = utils.Messenger.getInstance();
      while (true) {
        try {
          mger.msg("PDBTECListenerThread.run(): waiting to read an RRObject...",
						       2);
          // 1. read from socket input
          RRObject obj =  (RRObject) _ois.readObject();  
          // obj is an TaskObjectsExecutionRequest
          mger.msg("PDBTECListenerThread.run(): RRObject read",2);
          // 2. take appropriate action
          try {
            obj.runProtocol(_srv, _ois, _oos);
          }
          catch (PDBatchTaskExecutorException e) {  // give it a second chance
						mger.msg("PDBTECListenerThread.run(): calling obj.runProtocol() "+
							       "issued PDBatchTaskExecutorException, try one more time.", 
							       1);
						secondChance(obj);
          }
					catch (IOException e) {  // worker somehow failed, 
						                       // give srv one more shot, then notify client
						mger.msg("PDBTECListenerThread.run(): calling obj.runProtocol() "+
							       "issued IOException, will try one more time.", 1);
						secondChance(obj);
					}
        }
        catch (Exception e) {  // client closed connection
          // e.printStackTrace();
          try {
            _ois.close();
            _oos.close();
            _s.close();
          }
          catch (Exception e2) {
            e2.printStackTrace();
          }
          mger.msg("PDBatchTaskExecutorSrv:Client Network Connection Closed",0);
          return;  // bye bye
        }
      }  // while true
    }
		
		
		private void secondChance(RRObject obj) 
			throws ClassNotFoundException, IOException {
			try {
				obj.runProtocol(_srv, _ois, _oos);
			}
			catch (PDBatchTaskExecutorException e2) {
				utils.Messenger.getInstance().msg(
					"PDBTECListenerThread.run(): sending NoWorkerAvailableResponse() "+
					"to client...",2);
				// e.printStackTrace();
				_oos.reset();  // force object to be written anew
				_oos.writeObject(new NoWorkerAvailableResponse(
					                     ((TaskObjectsExecutionRequest) obj)._tasks));
				_oos.flush();							
			}
			catch (IOException e2) {
				utils.Messenger.getInstance().msg(
					"PDBTECListenerThread.run(): sending FailedReply to client...",2);
				// e.printStackTrace();
				_oos.writeObject(new FailedReply());  // no need for _oos.reset()
				_oos.flush();														
			}
		}
  }


  /**
   * auxiliary inner class.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class PDBTEWListener {
    private Socket _s;
    private ObjectInputStream _ois;
    private ObjectOutputStream _oos;
    private PDBatchTaskExecutorSrv _srv;
    private boolean _isAvail = true;
		private boolean _isPrevRunSuccess=true;
		private TaskObject[] _prevFailedBatch=null;

    PDBTEWListener(PDBatchTaskExecutorSrv srv, Socket s) 
			throws IOException {
      _srv = srv;
      _s = s;
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
    }


    private TaskObjectsExecutionResults runObject(TaskObjectsExecutionRequest o) 
			throws IOException, PDBatchTaskExecutorException {
      Object res = null;
      try {
        setAvailability(false);
				_oos.reset();  // force object to be written anew
        _oos.writeObject(o);
        _oos.flush();
        res = _ois.readObject();
        setAvailability(true);
      }
      catch (IOException e) {  // worker closed connection
        processException(e);
        throw e;
      }
      catch (ClassNotFoundException e) {
        processException(e);
        throw new IOException("stream has failed");
      }
      if (res instanceof TaskObjectsExecutionResults) {
        _isPrevRunSuccess=true;
				return (TaskObjectsExecutionResults) res;
			}
      else {
				PDBatchTaskExecutorException e = 
					new PDBatchTaskExecutorException("worker failed to run tasks");
				if (_isPrevRunSuccess==false && !sameAsPrevFailedJob(o._tasks)) { 
					processException(e);  // twice a loser, kick worker out
				}
				_isPrevRunSuccess=false;
				_prevFailedBatch = o._tasks;
				throw e;
			}
    }


    private synchronized boolean getAvailability() {
      boolean res = _isAvail && _s!=null && _s.isClosed()==false;
      if (false) {  // used to be if (res) but _s.sendUrgentData(0); call will
				// always eventually cause the other end to close the socket?!?
        // last test using OOB sending of data
        try {
          _s.sendUrgentData(0);
          res = true;
        }
        catch (IOException e) {
          utils.Messenger.getInstance().msg("Socket has been closed",0);
          res = false;
          _isAvail = false;  // declare availability to false as well
          // try graceful exit
          try {
            _s.shutdownOutput();
            _s.close(); // Now we can close the Socket
          }
          catch (IOException e2) {
            // silently ignore
          }
        }
      }
      return res;
    }
    private synchronized void setAvailability(boolean v) { _isAvail = v; }


    protected boolean isConnectionLost() {
      return _s==null || _s.isClosed();
    }


    private void processException(Exception e) {
      // e.printStackTrace();
      try {
        _ois.close();
        _oos.close();
        _s.close();
      }
      catch (Exception e2) {
        // e2.printStackTrace();
      }
      finally {
	      synchronized (_srv) {
		      _workers.remove(_s);
			    utils.Messenger.getInstance().msg(
						"PDBatchTaskExecutorSrv: Worker Network Connection Closed",0);
				}
        _ois = null;
        _oos = null;
        _s = null;
      }
    }
		
		
		/**
		 * compare the input argument with the batch tasks from the previous failed
		 * batch job.
		 * @param tasks TaskObject[]
		 * @return true if the two batch jobs' tasks have the same byte array 
		 * representation task-for-task.
		 */
		private boolean sameAsPrevFailedJob(TaskObject[] tasks) {
			if (_prevFailedBatch==null) return false;
			if (_prevFailedBatch.length!=tasks.length) return false;
			for (int i=0; i<tasks.length; i++) {
				if (!sameBytes(tasks[i],_prevFailedBatch[i])) return false;
			}
			return true;
		}
		
		
		/**
		 * compare the byte-array representation of two TaskObjects.
		 * @param f TaskObject
		 * @param s TaskObject
		 * @return true if the two arguments have the same byte-array
		 */
		private boolean sameBytes(TaskObject f, TaskObject s) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = null;
			try {
				out = new ObjectOutputStream(bos);
				out.writeObject(f);
				byte[] fs = bos.toByteArray();
				out.writeObject(s);
				byte[] ss = bos.toByteArray();
				if (fs.length!=ss.length) return false;
				for (int i=0; i<fs.length; i++) {
					if (fs[i]!=ss[i]) return false;
				}
				return true;
			}
			catch (IOException e) {
				e.printStackTrace();  // not serializable?
				return false;
			}
			finally {
				if (out!=null) {
					try {
						out.close();
					}
					catch (IOException e) {
						// ignore
					}
					try {
						bos.close();
					}
					catch (IOException e) {
						// ignore
					}
				}
			}
		}
  }

}

