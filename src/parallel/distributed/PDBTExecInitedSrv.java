package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * class extends the PDBatchTaskExecutorSrv class to allow a load-balancing 
 * distributed computing architecture, whereby a server object of this class can 
 * have many clients connected to it, but no other known servers connected to it 
 * at all. Several workers of type <CODE>PDBTExecInitedWrk</CODE> (only) can be 
 * connected to this server. Each such worker, before starting its threads, will 
 * have to be initialized by executing an "initialization-command" object that 
 * will arrive first via any client to this server, even though each client has 
 * the obligation to send first an initialization command. The server will 
 * always be forwarding the initialization command to each worker upon 
 * connecting to it (the initialization command to be used will be the first one 
 * arriving from any client to this server; any other init-cmds from other 
 * clients, will be silently ignored). If the initialization command is an 
 * <CODE>OKReplyRequestedPDBTExecWrkInitCmd</CODE> object, the server will wait
 * until each worker finishes its initialization, and sends back to the server
 * an <CODE>OKReply</CODE>. In this case, the client will also wait until there 
 * is at least one initialized worker. The clients must always be 
 * <CODE>PDBTExecInitedClt</CODE> objects only.
 * Notice that in case workers connect to this server before any client 
 * connects to it, the workers will have to wait indefinitely until one client 
 * connects and sends its initialization command which will then be forwarded to 
 * all waiting workers and all new ones coming afterwards.
 * Notice also that this server achieves load-balancing among connected workers
 * by dividing equally the load of the multiple TaskObject objects submitted in 
 * each client request via a <CODE>TaskObjectsParallelExecutionRequest</CODE> 
 * request, among many <CODE>WrkSubmissionThread</CODE> threads (defined in 
 * RRObject.java) that simultaneously attempt to submit their load to the 
 * available workers; as faster workers finish first they become available for 
 * picking up the work of the waiting threads, and thus load balancing among the
 * workers is achieved.
 * Computing Policies:
 * If a worker connection is lost during processing a batch of tasks, the batch
 * will be re-submitted once more to the next available worker, as soon as such
 * a worker becomes available. Similarly, if a worker fails to process a batch
 * of tasks and returns a <CODE>FailedReply</CODE> object back to this server,
 * the server will attempt one more time to re-submit the batch to another 
 * worker as soon as such a worker becomes available. In case a worker fails 
 * to process two different batches of jobs in sequence, the server drops its
 * connection from this "loser" worker. If the same batch of jobs fails to be 
 * executed by two different workers, the server sends back to the client a 
 * <CODE>FailedReply</CODE> indicating the job cannot be successfully completed.
 * <p>Notes:
 * <ul>
 * <li>2021-05-14: added functionality to query the current number of connected
 * workers on this server.
 * <li>2023-12-05: method <CODE>addNewWorkerConnection(s)</CODE> is now 
 * protected and does not throw <CODE>IOException</CODE>. Also, previously
 * private methods <CODE>submitWork()</CODE> are now protected.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBTExecInitedSrv extends PDBatchTaskExecutorSrv { 
	private final static int _CHECK_PERIOD_MSECS=10000;  // 10 seconds
	private RRObject _initCmd=null;  // the init. cmd to be sent to all workers
	                                 // attached to this server before any other
	                                 // job.
	private PDBTExecCmd _cmd=null;  // the current cmd to be sent to all workers
	                                // attached to this server.

	
  /**
   * sole public constructor.
   * @param wport int the port workers (PDBTExecInitedWrk) connect to.
   * @param cport int the port for the client (PDBTExecInitedClt) to connect to.
   */
  public PDBTExecInitedSrv(int wport, int cport) {
		super(wport, cport);
  }


  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; parallel.distributed.PDBTExecInitedSrv 
	 * [workers_port(7890)] [clients_port(7891)] [debuglvl(0)]</CODE>.
   * @param args String[]
   */
  public static void main(String[] args) {
    int wport = 7890;  // default port
    int cport = 7891;
		int dbglvl = 0;
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
	      if (args.length>2) {
		      try {
			      dbglvl = Integer.parseInt(args[2]);
				  }
					catch (Exception e) {
						e.printStackTrace();
						usage();
						System.exit(-1);
					}
				}
      }
    }
		utils.Messenger.getInstance().setDebugLevel(dbglvl);
    PDBTExecInitedSrv server = new PDBTExecInitedSrv(wport, cport);
    try {
      server.run();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.err.println("Server exits due to exception.");
    }
  }


  protected void run() throws IOException {
    W2Thread workersListeningThread = new W2Thread(getWorkersPort());
    workersListeningThread.start();
    C2Thread clientListeningThread = new C2Thread(getClientsPort());
    clientListeningThread.start();
  }

	
  protected TaskObjectsExecutionResults submitWork(Vector originating_clients, 
		                                               TaskObject[] tasks) 
		throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
    Set workers2rm = new HashSet();  // Set<Socket s> for 
		                                 // (Socket s, PDBTEW2Listener t) pair
    PDBTEW2Listener t = null;
		utils.Messenger mger = utils.Messenger.getInstance();
    mger.msg("PDBTExecInitedSrv.submitWork(clts,tasks): "+
			       "finding an available worker connection among "+
			       getNumWorkers()+" known workers",1);
    // 1. find a worker (via Round-Robin)
    synchronized (this) {
			boolean cont = true;
      while (cont) {
        workers2rm.clear();
        Iterator sit = getWorkers().keySet().iterator();
        while (sit.hasNext()) {
          Socket s = (Socket) sit.next();
          t = (PDBTEW2Listener) getWorkers().get(s);
          boolean is_avail = t.getAvailability() && !getWorking().contains(t);
          // the _working set is needed so that submitWork() is guaranteed not
          // to choose twice the same worker before the worker is done with the
          // first request.
          if (is_avail) {
            cont = false;  // break out of the top-level while-loop too
            getWorking().add(t);
            break;
          }
          else {
            if (t.isConnectionLost()) workers2rm.add(s);  // used to be add(t)
            t = null;  // reset to null
          }
        }
        // remove any "lost connections" worker listeners
        Iterator it = workers2rm.iterator();
        while (it.hasNext()) getWorkers().remove(it.next());
				if (t==null) {
					// before trying again to find an available worker, wait a while
					try {
						wait(_CHECK_PERIOD_MSECS);  // wait out the workers for some time
					}
					catch (InterruptedException e) {
						// e.printStackTrace();
						Thread.currentThread().interrupt();  // recommended
					}
				}
      }
    }  // synchronized (this)
    if (t==null) {  // failed to find an available thread
      mger.msg("PDBTExecInitedSrv.submitWork(clt,tasks): "+
				       "no available threads...",1);
      throw new PDBatchTaskExecutorException(
				"no available worker or known srv to undertake work");
    }
    mger.msg("PDBTExecInitedSrv.submitWork(clt,tasks): found "+
			       "an available worker",1);
    // 2. submit tasks and get back results
    TaskObjectsExecutionRequest req = 
			new TaskObjectsExecutionRequest(originating_clients, tasks);
    mger.msg("PDBTExecInitedSrv.submitWork(clt,tasks): created the "+
			       "TaskObjectsExecutionRequest to send",1);
    RRObject res = submitWork(req, t);
    mger.msg("PDBTExecInitedSrv.submitWork(tasks): finished running "+
			       "submitWork(req,ois,oos)",1);
    synchronized (this) {
      getWorking().remove(t);  // declare worker's availability again
			notifyAll();  // declare I'm done
    }
    if (res instanceof TaskObjectsExecutionResults)
      return (TaskObjectsExecutionResults) res;
    else {
      throw new PDBatchTaskExecutorException(
				"PDBTExecInitedSrv.submitWork(tasks): worker failed to process tasks.");
    }
  }


	/**
	 * simply calls the method <CODE>t.runObject(req)</CODE> wrapped in some debug
	 * messages, and returns the result of the call.
	 * @param req TaskObjectsExecutionRequest
	 * @param t PDBTEW2Listener
	 * @return TaskObjectsExecutionResults
	 * @throws IOException
	 * @throws PDBatchTaskExecutorException 
	 */
  protected TaskObjectsExecutionResults submitWork(
		                                      TaskObjectsExecutionRequest req, 
		                                      PDBTEW2Listener t)
      throws IOException, PDBatchTaskExecutorException {
    utils.Messenger.getInstance().msg("PDBTExecInitedSrv.submitWork(req,t): "+
			                                "sending request",1);
    TaskObjectsExecutionResults res = t.runObject(req);
    utils.Messenger.getInstance().msg("PDBTExecInitedSrv.submitWork(req,t): "+
			                                "response received",1);
    return res;
  }

	
	/**
	 * sets the initial command for all workers to execute, and notifies any 
	 * worker-threads waiting for such command to be sent to the workers. Only the
	 * cmd received first applies; subsequent invocations are "lost" (no-ops).
	 * @param obj RRObject
	 * @return boolean false iff the init-cmd has already been set and is not an
	 * <CODE>OKReplyRequestedPDBTExecWrkInitCmd</CODE> object but the argument obj
	 * is.
	 */
	private synchronized boolean setInitCmd(RRObject obj) {
		if (_initCmd==null) {
			_initCmd = obj;
			notifyAll();
			return true;
		}
		return (_initCmd instanceof OKReplyRequestedPDBTExecWrkInitCmd) ||
			      !(obj instanceof OKReplyRequestedPDBTExecWrkInitCmd);
	}
	
	
	/**
	 * sets the command to be forwarded to all connected workers. Each worker
	 * listener will first forward this command when the time comes to execute
	 * another command, when its method 
	 * <CODE>PDBTExecInitedSrv.PDBTEW2Listener.runObject(RRobject)</CODE> is 
	 * executed next. The worker receiving such commands executes them on the main
	 * thread receiving objects through the wire, not in its thread-pool.
	 * Notice that if this method is called often enough 
	 * (by the same or different clients), it's possible that different workers
	 * will execute different commands. To see this, consider the following 
	 * scenario: a client sets a command c1, and then sends some tasks t1 to be 
	 * executed which the server forwards to worker w1; the worker w1 first 
	 * executes the command c1 of the client, then the tasks t1 sent. Meanwhile, 
	 * another client sends another command c2, and then another set of tasks t2 
	 * to be executed to the same server. The server has now changed its command
	 * to c2, and then decides to send the tasks to worker w2. Worker w2 will now
	 * execute only c2 (c1 is "lost" to this worker), and will then execute t2.
	 * At this point, w1 has executed c1, and w2 has executed c2. If another 
	 * client submits tasks t3 which the server forwards to w1, then w1 will also
	 * execute c2 first, and then execute t3, and will be "in synch" with w2 as
	 * far as the latest commands are concerned.
	 * @param obj PDBTExecCmd
	 */
	private synchronized void setCmd(PDBTExecCmd obj) {
		_cmd = obj;
	}
	
	
	/**
	 * get the latest command set by a client to be forwarded to workers.
	 * @return PDBTExecCmc
	 */
	private synchronized PDBTExecCmd getCmd() {
		return _cmd;
	}
	

  /**
   * adds a new worker to the network. Workers are server programs that connect
	 * to a <CODE>PDBTExecInitedSrv</CODE> and listen for commands (as 
	 * <CODE>RRObject</CODE> objects) to arrive through the wire, which they then 
	 * execute. This method will block until the server receives an initialization
	 * command from a client; the method will further block if the initialization
	 * command received first is a <CODE>OKReplyRequestedPDBTExecWrkInitCmd</CODE>
	 * in which case the method will not return until the worker in the other end
	 * of the socket has been fully initialized and sends back an 
	 * <CODE>OKReply</CODE>.
   * @param s Socket
   */
  protected synchronized void addNewWorkerConnection(Socket s) {
    try {
			PDBTEW2Listener lt = new PDBTEW2Listener(s);
			lt.initWorker();
			getWorkers().put(s, lt);
			notifyAll();  // notify client-threads waiting for workers to be inited
		}
		catch (IOException e) {
			e.printStackTrace();
		}
  }


  /**
   * adds a new client to the network. The difference between clients and
   * workers is that "clients" submit jobs to the network, but don't want to
   * "get" any job to do themselves. Workers may also submit jobs to the network
   * but are mainly available to run tasks themselves as well, and in fact this
	 * is their main purpose.
   * @param s Socket
	 * @throws IOException
   */
  private synchronized void addNewClientConnection(Socket s) 
		throws IOException {
		PDBTEC2ListenerThread lt = new PDBTEC2ListenerThread(s);
    lt.start();
  }


  static void usage() {
    System.err.println("usage: java -cp <classpath> "+
			                 "parallel.distributed.PDBTExecInitedSrv "+
			                 "[workersport(7890)] [clientsport(7891)] [debuglvl(0)]");
  }


  /**
   * auxiliary inner class used for listening for incoming worker connections
   * and creating new PDBTEW2Listener objects to handle each connection. Not 
	 * part of the public API.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011-2017</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class W2Thread extends Thread {
    int _port;

    private W2Thread(int port) {
      _port = port;
    }


		/**
		 * creates a server socket listening on port specified in the object 
		 * constructor, and then enters an infinite loop waiting for incoming
		 * socket connection requests representing a worker process attempting to
		 * connect to this server, which it handles via the enclosing server's
		 * <CODE>addNewWorkerConnection(s)</CODE> method.
		 */
    public void run() {
			utils.Messenger mger = utils.Messenger.getInstance();
      try {
        ServerSocket ss = new ServerSocket(_port);
        mger.msg("Srv: Now Accepting Worker Connections",0);
        while (true) {
          try {
            Socket s = ss.accept();
            mger.msg("Srv: Incoming New Worker Connection to the Network",1);
						mger.msg("Srv: Thread may have to wait if an init_cmd has not "+
							       "yet arrived from the client",1);
            addNewWorkerConnection(s);
            mger.msg("Srv: finished adding new worker connection "+
							       "to the _workers",1);
          }
          catch (Exception e) {
						mger.msg("PDBTExecInitedSrv.W2Thread.run(): "+
							       "An error occured while adding new worker connection", 0);
            // e.printStackTrace();
          }
        }
      }
      catch (IOException e) {
        // e.printStackTrace();
				mger.msg("PDBTExecInitedSrv.W2Thread.run(): "+
					       "Failed to create Server Socket, Server exiting.", 0);
				System.exit(-1);
      }
    }
  }


  /**
   * auxiliary inner class used for listening for incoming client connections
   * and creating new <CODE>PDBTEC2ListenerThread</CODE> threads to handle each 
	 * connection. Not part of the public API.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011-2017</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class C2Thread extends Thread {
    private int _port;

    private C2Thread(int port) {
      _port = port;
    }

		
		/**
		 * creates a server socket listening on the port specified in the parameter 
		 * of the constructor, and waits for incoming clients connections
		 * which it handles by invoking the <CODE>addNewClientConnection(s)</CODE>
		 * method of the enclosing server.
		 */
    public void run() {
			utils.Messenger mger = utils.Messenger.getInstance();
      try {
        ServerSocket ss = new ServerSocket(_port);
        mger.msg("Srv: Now Accepting Client Connections",0);
        while (true) {
          try {
            Socket s = ss.accept();
            mger.msg("Srv: Client Added to the Network",1);
            addNewClientConnection(s);
            mger.msg("Srv: finished adding client connection",1);
          }
          catch (Exception e) {
            // e.printStackTrace();
						mger.msg("Srv: Client Connection failed...",0);
          }
        }
      }
      catch (IOException e) {
        // e.printStackTrace();
				mger.msg("PDBTExecInitedSrv.C2Thread.run(): "+
					       "Failed to create Server Socket, Server exiting.", 0);
				System.exit(-1);
      }
    }
  }


  /**
   * auxiliary inner class. Not part of the public API.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011-2017</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class PDBTEC2ListenerThread extends Thread {
    private Socket _s;
    private ObjectInputStream _ois;
    private ObjectOutputStream _oos;
    private boolean _isAvail = true;

    private PDBTEC2ListenerThread(Socket s) throws IOException {
      _s = s;
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
    }


		/**
		 * reads from the input stream the initialization command sent to it, sends
		 * back to the client an <CODE>OKReply</CODE> "ACK" msg (immediately, unless
		 * the init-cmd is an <CODE>OKReplyRequestedPDBTExecWrkInitCmd</CODE> and is
		 * first to arrive to the server, in which case server first waits until at 
		 * least one worker has been properly initialized) and then enters an 
		 * infinite loop waiting to read from the input stream <CODE>RRObject</CODE> 
		 * objects that should really be of type 
		 * <CODE>TaskObjectsExecutionRequest</CODE> on which it executes its method
		 * <CODE>obj.runProtocol(_srv,_ois,_oos)</CODE>. Alternatively, the objects 
		 * received within the loop may be <CODE>PDBTExecCmd</CODE>, in which case 
		 * the method <CODE>setCmd(obj)</CODE> is executed, and an OKReply sent back 
		 * to the client. Notice that if the client sends an init-cmd that requests
		 * workers to reply, but the server has already set (from another client) an 
		 * init-cmd that doesn't require replies from the workers, the connection
		 * to the client will be lost, as this is considered an inconsistent state.
		 * In addition, the object could be <CODE>PDBTNumWorkersQueryCmd</CODE> in
		 * which case, this server will send back an Integer object describing the 
		 * total number of connected workers to it.
		 */
    public void run() {
			// first, read from socket the worker-initialization object that will
			// be broadcast for execution to every worker connecting to this server.
			utils.Messenger mger = utils.Messenger.getInstance();
			try {
				mger.msg(
					"PDBTEC2ListenerThread: waiting to read the init_cmd from client", 1);
				RRObject initCmd = (RRObject) _ois.readObject();
				mger.msg(
					"PDBTEC2ListenerThread: done reading the init_cmd from client", 1);
				boolean set = setInitCmd(initCmd);  // initCmd will be set but
				                                    // only if this is 1st client
				if (!set) 
					throw new IllegalStateException(
						"init-cmd already set is not OKReplyRequestedPDBTExecWrkInitCmd");
				mger.msg("PDBTEC2ListenerThread: done setting the init_cmd", 2);
				if (initCmd instanceof OKReplyRequestedPDBTExecWrkInitCmd) {
					// wait until at least one worker is initialized
					mger.msg("PDBTEC2ListenerThread: waiting "+
						       "for at least 1 Wrk to initialize", 2);
					synchronized (PDBTExecInitedSrv.this) {
						while (getWorkers().size()==0) {  // wait for at least 1 worker
							try {
								PDBTExecInitedSrv.this.wait();
							}
							catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
					}
				}
				// send back to the clt an "ACK" message
				_oos.writeObject(new OKReply());  // no need to call _oos.reset() first
				_oos.flush();
				mger.msg("PDBTEC2ListenerThread: sent OKReply to client", 2);
			}
			catch (Exception e) {  // client closed connection
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
					mger.msg("PDBTEC2ListenerThread.run(): in initialization, "+
						       "received Exception '"+e+"'.Client connection closed; "+
						       "thread exits.",0);
					return;
				}
			}
			// main loop
      while (true) {
        try {
          mger.msg(
						"PDBTEC2ListenerThread.run(): waiting to read an RRObject...",2);
          // 1. read from socket input
          RRObject obj =  (RRObject) _ois.readObject();
          mger.msg("PDBTEC2ListenerThread.run(): RRObject read",2);
          // 2. take appropriate action
          try {
						if (obj instanceof PDBTNumWorkersQueryCmd) {
							final int num_workers = getNumWorkers();
							mger.msg("PDBTEC2ListenerThread: got #workers query command", 2);
							_oos.writeObject(new Integer(num_workers));
							_oos.flush();
							mger.msg("PDBTEC2ListenerThread: done w/ #workers query", 2);
							continue;
						}
						if (obj instanceof PDBTExecCmd) {
							setCmd((PDBTExecCmd)obj);
							mger.msg("PDBTEC2ListenerThread: done setting PDBTExecCmd", 2);
							// send back to the clt an "ACK" message
							_oos.writeObject(new OKReply());  // no need to call _oos.reset()
							_oos.flush();
							mger.msg("PDBTEC2ListenerThread: done sending OKReply "+
								       "to client", 2);
							continue;
						}
            obj.runProtocol(PDBTExecInitedSrv.this, _ois, _oos);
          }
          catch (PDBatchTaskExecutorException e) {
						mger.msg("PDBTEC2ListenerThread.run(): calling obj.runProtocol() "+
							       "issued PDBatchTaskExecutorException, will try one more "+
							       "time.", 1);
						secondChance(obj);
          }
					catch (IOException e) {  
            // worker somehow failed, give srv one more shot, then notify client
						mger.msg("PDBTEC2ListenerThread.run(): calling obj.runProtocol() "+
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
            // e2.printStackTrace();
          }
          mger.msg("PDBTExecInitedSrv: Client Network Connection Closed "+
						       "(exception '"+e+"' caught)",0);
					break;  // terminate the thread as client lost connection
        }
      }  // while true
    }
		
		
		private void secondChance(RRObject obj) throws ClassNotFoundException, 
			                                             IOException {
			try {
				obj.runProtocol(PDBTExecInitedSrv.this, _ois, _oos);
			}
			catch (PDBatchTaskExecutorException e) {
				utils.Messenger.getInstance().msg(
					"PDBTEC2ListenerThread.run(): sending NoWorkerAvailableResponse() "+
					"to client...",1);
				// e.printStackTrace();
				_oos.reset();  // force objects to be written anew
				_oos.writeObject(new NoWorkerAvailableResponse(
					                     ((TaskObjectsExecutionRequest) obj)._tasks));
				_oos.flush();							
			}
			catch (IOException e2) {
				utils.Messenger.getInstance().msg(
					"PDBTEC2ListenerThread.run(): sending FailedReply to client...",1);
				// e2.printStackTrace();
				_oos.writeObject(new FailedReply());  // _oos.reset() not needed here
				_oos.flush();														
			}
		}

  }


  /**
   * auxiliary inner class. Not part of the public API.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011-2017</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class PDBTEW2Listener {
    private Socket _s;
    private ObjectInputStream _ois;
    private ObjectOutputStream _oos;
    private boolean _isAvail = true;
		private boolean _OK2SendOOB = false;  // redundant initialization
		private boolean _isPrevRunSuccess=true;
		private TaskObject[] _prevFailedBatch=null;
		private PDBTExecCmd _lastCmdExecuted=null;  // any cmd sent by a client to
		                                            // be executed on all connected
		                                            // workers that was actually run
		                                            // note: not same as _initCmd

    private PDBTEW2Listener(Socket s) throws IOException {
      _s = s;
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
    }

		
		/**
		 * no need to synchronize any block in this method, as it is already 
		 * synchronized on _srv since it's only called by the enclosing server's 
		 * <CODE>addNewWorkerConnection(s)</CODE> method.
		 * @throws IOException 
		 */
		private void initWorker() throws IOException {
			// wait until the _initCmd is ready, and send it over the socket
			while (_initCmd==null) {
				try {
					utils.Messenger.getInstance().msg(
						"PDBTExecInitedSrv.PDBTEW2Listener.initWorker(): "+
						"W2Thread waiting on server to obtain init_cmd from client...", 0);
					PDBTExecInitedSrv.this.wait();  // the thread calling this method 
					                                // is already synchronized on _srv.
				}
				catch (InterruptedException e) {
					// e.printStackTrace();
					Thread.currentThread().interrupt();
				}
			}
			_oos.reset();  // force object to be written anew
			_oos.writeObject(_initCmd);
			_oos.flush();
			if (_initCmd instanceof OKReplyRequestedPDBTExecWrkInitCmd) {
				try {
					Object res = _ois.readObject();
					if (!(res instanceof OKReply)) {
						throw new IllegalStateException("Wrk: initialization failed");
					}
				}
				catch (ClassNotFoundException e) {
					throw new IllegalStateException("Wrk: failed to initialize");					
				}
			}
			utils.Messenger.getInstance().msg(
						"PDBTExecInitedSrv.PDBTEW2Listener.initWorker(): done.", 0);
		}

		
    private TaskObjectsExecutionResults runObject(TaskObjectsExecutionRequest o) 
			throws IOException, PDBatchTaskExecutorException {
      Object res = null;
      try {
        setAvailability(false);
				PDBTExecCmd cmd = getCmd();
				if (cmd!=null && cmd!=_lastCmdExecuted) {  // send the new cmd to worker
					_oos.reset();  // force writing object anew
					_oos.writeObject(cmd);
					_oos.flush();
					res = _ois.readObject();
					if (!(res instanceof OKReply)) {
						PDBatchTaskExecutorException e = 
							new PDBatchTaskExecutorException(
								    "worker failed to run PDBTExecCmd");
						processException(e);  // kick worker out
						_isPrevRunSuccess=false;
						_prevFailedBatch = o._tasks;
						throw e;
					}
					_lastCmdExecuted = cmd;
				}
				_oos.reset();  // force writing object anew
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


		/**
		 * there is an issue with this method: if it is called often enough, the 
		 * <CODE>_s.sendUrgentData(0);</CODE> method that it invokes, will force 
		 * the <CODE>_s</CODE> socket to close on the other end, at least on Windows 
		 * systems. This behavior is due to the fact that OOB data handling is 
		 * problematic since there are conflicting specifications in TCP. Therefore, 
		 * it is required that the method is not called with high frequency (see the 
		 * <CODE>PDBTExecInitedSrv._CHECK_PERIOD_MSECS</CODE> flag in this 
		 * file.)
		 * @return true iff the worker is available to accept work according to all 
		 * evidence.
		 */
    private synchronized boolean getAvailability() {
      boolean res = _isAvail && _s!=null && _s.isClosed()==false;
      if (res && _OK2SendOOB) {  // work-around the OOB data issue  
        // last test using OOB sending of data
        try {
					_OK2SendOOB = false;  // indicates should not send OOB data 
					                      // until set to true
          _s.sendUrgentData(0);  // unfortunately, if this method is called 
					                       // often enough, it will cause the socket to 
					                       // close???
          res = true;
        }
        catch (IOException e) {
					// e.printStackTrace();
          utils.Messenger.getInstance().msg(
						"PDBTExecInitedSrv.getAvailability(): Socket has been closed",0);
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
    private synchronized void setAvailability(boolean v) { 
			_isAvail = v;
			// by commenting out line below, sending OOB data is disabled,
			// as it is not possible in Windows to reliably send repeated OOB data
			// and not have the client at some point throw SocketException (connection
			// reset by peer).			
			// if (v) _OK2SendOOB = true;
		}


    private boolean isConnectionLost() {
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
	      synchronized (PDBTExecInitedSrv.this) {
		      getWorkers().remove(_s);
			    utils.Messenger.getInstance().msg(
						"PDBTExecInitedSrv: Worker Network Connection Closed",0);
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
				e.printStackTrace();
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
  }  // class PDBTExecInitedSrv.PDBTEW2Listener 

}  // class PDBTExecInitedSrv

