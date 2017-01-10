package parallel.distributed;

import parallel.*;
import java.io.*;
import java.net.*;
import java.util.*;


/**
 * class implements the "Client" for networks of 
 * <CODE>PDAsynchBatchTaskExecutorWrk</CODE> workers attached to 
 * <CODE>PDAsynchBatchTaskExecutorSrv</CODE> servers (managers really), as a 
 * Singleton.
 * Connects to a host server represented by the 
 * <CODE>PDAsynchBatchTaskExecutorSrv</CODE> object, to a specific host/port IP 
 * address, during first use, and remains connected to that server for the 
 * duration of its useful life; when the user-code no longer needs the client 
 * services, must invoke <CODE>disconnect()</CODE> method to free up client and 
 * server resources.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDAsynchBatchTaskExecutorClt {
  private static String _host="localhost";  // default host
  private static int _port = 7981;  // default client port
	private Socket _s = null;
	private ObjectInputStream _ois = null;
	private ObjectOutputStream _oos = null;
	private boolean _initCmdStateInvalidated = false;
	
	private static PDAsynchBatchTaskExecutorClt _instance = null;  // singleton 

	
	/**
	 * get the single instance living in a JVM.
	 * @return PDAsynchBatchTaskExecutorClt
	 * @throws IOException 
	 */
	public static synchronized PDAsynchBatchTaskExecutorClt getInstance() throws IOException {
		if (_instance==null) _instance = new PDAsynchBatchTaskExecutorClt();
		return _instance;
	}
	
  /**
   * private no-arg constructor, will attempt to connect to the host/port given
	 * as defaults, or as specified before by a call to <CODE>setHostPort()</CODE>
	 * method.
	 * @throws IOException if connection fails.
   */
  private PDAsynchBatchTaskExecutorClt() throws IOException {
      _s = new Socket(_host, _port);
      _oos = new ObjectOutputStream(_s.getOutputStream());
      _oos.flush();
      _ois = new ObjectInputStream(_s.getInputStream());
  }


  /**
   * provide explicitly the server host/port connection parameters. Method must
	 * be called before the first call to <CODE>getInstance()</CODE>.
   * @param hostipaddress String
   * @param port int
	 * @throws IllegalStateException if the <CODE>getInstance()</CODE> method
	 * has been already been invoked.
   */
  public static synchronized void setHostPort(String hostipaddress, int port) {
    if (_instance!=null) {
			throw new IllegalStateException("PDAsynchBatchTaskExecutorClt.setHostPort(): getInstance() has already been invoked");
		}
		_host = hostipaddress;
    _port = port;
  }


  /**
   * the (internet) IP address of the server this client will be connecting to 
   * when submitting work.
   * @return String
   */
  public static synchronized String getHostIPAddress() {
    if ("localhost".equals(_host)) {
      // figure out the localhost IP address
      try {
        InetAddress i = InetAddress.getLocalHost();
        return i.getHostAddress();
      }
      catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    } else return _host;
  }


  /**
   * the port of the server this client will be connecting to when submitting
   * work.
   * @return int
   */
  public static synchronized int getPort() { return _port; }


  /**
   * the main method of the class. Sends over the network the tasks parameter
   * to the PDAsynchBatchTaskExecutorSrv server who then distributes them to one
   * of the available workers in the network. Method returns immediately.
   * @param tasks TaskObject[]
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws PDAsynchBatchTaskExecutorException
	 * @throws PDAsynchBatchTaskExecutorNWAException if no worker has capacity
   */
  public synchronized void submitWorkFromSameHost(TaskObject[] tasks)
      throws IOException, ClassNotFoundException, PDAsynchBatchTaskExecutorException {
    if (tasks==null || tasks.length==0)
      throw new PDAsynchBatchTaskExecutorException("PDAsynchBatchTaskExecutorClt.submitWork(tasks): null or empty tasks passed in.");
    /*
    byte[] localipaddr = s.getLocalAddress().getAddress();
    StringBuffer buf = new StringBuffer();
    for (int i=0; i<localipaddr.length; i++) {
      buf.append(String.valueOf(localipaddr[i]));
      if (i < localipaddr.length-1) buf.append('.');
    }
    String client_addr = new String(buf);
    */
		_initCmdStateInvalidated = true;
    InetAddress ia = InetAddress.getLocalHost();
    String client_addr_port = ia.getHostAddress()+"_"+_port;
    TaskObjectsAsynchExecutionRequest req = new TaskObjectsAsynchExecutionRequest(client_addr_port, tasks);
    _oos.writeObject(req);
    _oos.flush();
    Object response = _ois.readObject();
    if (response instanceof OKReply) {
      return;
    }
    else if (response instanceof NoWorkerAvailableResponse)
      throw new PDAsynchBatchTaskExecutorNWAException("no worker was available...");
		else if (response instanceof FailedReply) 
			throw new PDAsynchBatchTaskExecutorException("worker failed...");
    throw new PDAsynchBatchTaskExecutorException("cannot parse response...");
  }


  /**
   * the second main method of the class, works as the method 
	 * <CODE>submitWorkFromSameHost()</CODE>, except that it breaks the tasks into
	 * chunks of the given size, and submits them one at a time. Method returns 
	 * immediately.
   * @param tasks TaskObject[]
	 * @param max_chunk_size int
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws PDAsynchBatchTaskExecutorException
	 * @throws PDAsynchBatchTaskExecutorNWAException if no worker has capacity
   */
  public synchronized void submitWorkFromSameHostInParallel(TaskObject[] tasks, int max_chunk_size)
      throws IOException, ClassNotFoundException, PDAsynchBatchTaskExecutorException {
    if (tasks==null || tasks.length==0)
      throw new PDAsynchBatchTaskExecutorException("PDAsynchBatchTaskExecutorClt.submitWork(tasks): null or empty tasks passed in.");
		_initCmdStateInvalidated = true;
    InetAddress ia = InetAddress.getLocalHost();
    String client_addr_port = ia.getHostAddress()+"_"+_port;
    TaskObjectsAsynchExecutionRequest req = 
			new TaskObjectsAsynchParallelExecutionRequest(client_addr_port, tasks, max_chunk_size);
    _oos.writeObject(req);
    _oos.flush();
    Object response = _ois.readObject();
    if (response instanceof OKReply) {
      return;
    }
    else if (response instanceof NoWorkerAvailableResponse)
      throw new PDAsynchBatchTaskExecutorNWAException("no worker was available...");
		else if (response instanceof FailedReply) 
			throw new PDAsynchBatchTaskExecutorException("worker failed...");
    throw new PDAsynchBatchTaskExecutorException("cannot parse response...");
  }
	
	
  /**
   * sends over the network the tasks parameter
   * to the PDBatchTaskExecutorSrv server, who then distributes them to one
   * of the available workers in the network. Method returns immediately.
   * @param originating_client String &lt;host&gt;_&lt;port&gt; frmt
   * @param tasks TaskObject[]
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws PDAsynchBatchTaskExecutorException
	 * @throws PDAsynchBatchTaskExecutorNWAException when all workers are at full
	 * capacity, and cannot accept other tasks currently.
   */
  public synchronized void submitWork(String originating_client, TaskObject[] tasks)
      throws IOException, ClassNotFoundException, PDAsynchBatchTaskExecutorException {
    if (tasks==null || tasks.length==0)
      throw new PDAsynchBatchTaskExecutorException("PDAsynchBatchTaskExecutorClt.submitWork(clientname, tasks): null or empty tasks passed in.");
    if (originating_client==null || originating_client.length()==0)
      throw new PDAsynchBatchTaskExecutorException("PDAsynchBatchTaskExecutorClt.submitWork(clientname, tasks): null or empty clientname passed in.");
		_initCmdStateInvalidated = true;
    TaskObjectsAsynchExecutionRequest req = new TaskObjectsAsynchExecutionRequest(originating_client, tasks);
    _oos.writeObject(req);
    _oos.flush();
    Object response = _ois.readObject();
    if (response instanceof OKReply) {
      return;
    }
    else if (response instanceof NoWorkerAvailableResponse)
      throw new PDAsynchBatchTaskExecutorNWAException("no worker was available...");
		else if (response instanceof FailedReply) 
			throw new PDAsynchBatchTaskExecutorException("worker failed...");
    throw new PDAsynchBatchTaskExecutorException("cannot parse response...");
  }


  /**
   * same method as <CODE>submitWork(client, tasks)</CODE> but carries all
   * the names of the hosts that have submitted the tasks to various servers
   * (only applies when there exists more than one server in the system of
   * servers, clients and workers). Method returns immediately.
   * @param originating_clients Vector // Vector&lt;String&gt;
   * @param tasks TaskObject[]
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws PDAsynchBatchTaskExecutorException
	 * @throws PDAsynchBatchTaskExecutorNWAException if no worker has capacity
   */
  public synchronized void submitWork(Vector originating_clients, TaskObject[] tasks)
      throws IOException, ClassNotFoundException, PDAsynchBatchTaskExecutorException {
    if (tasks==null || tasks.length==0)
      throw new PDAsynchBatchTaskExecutorException("PDAsynchBatchTaskExecutorClt.submitWork(clientsnames, tasks): null or empty tasks passed in.");
    if (originating_clients==null || originating_clients.size()==0)
      throw new PDAsynchBatchTaskExecutorException("PDAsynchBatchTaskExecutorClt.submitWork(clientsnames, tasks): null or empty clientname passed in.");
		_initCmdStateInvalidated = true;
    TaskObjectsAsynchExecutionRequest req = new TaskObjectsAsynchExecutionRequest(originating_clients, tasks);
    _oos.writeObject(req);
    _oos.flush();
    Object response = _ois.readObject();
    if (response instanceof OKReply) {
      return;
    }
    else if (response instanceof NoWorkerAvailableResponse)
      throw new PDAsynchBatchTaskExecutorNWAException("no worker was available...");
		else if (response instanceof FailedReply) 
			throw new PDAsynchBatchTaskExecutorException("worker failed...");
    throw new PDAsynchBatchTaskExecutorException("cannot parse response...");
  }

	
	/**
	 * send an init-cmd to forward to any worker connecting to the server. Method
	 * is synchronized to protect the output connection stream, returns as soon
	 * as the parameter object is written on the socket (does not wait for an 
	 * OKReply or anything from the server). If it is called, it must be the
	 * first method called after object is constructed, and must be called only
	 * once.
	 * @param initcmd PDAsynchInitCmd
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDAsynchBatchTaskExecutorException if any other request has been 
	 * issued from this client before.
	 */
	public synchronized void sendInitCmd(PDAsynchInitCmd initcmd) 
		throws IOException, ClassNotFoundException, PDAsynchBatchTaskExecutorException {
		if (_initCmdStateInvalidated) 
			throw new PDAsynchBatchTaskExecutorException("PDAsynchBatchTaskExecutorClt.sendInitCmd(): "+
				                                           "invalid state for sending init-cmd to server.");
		_oos.writeObject(initcmd);
		_oos.flush();
		_initCmdStateInvalidated = true;
	}
	

	/**
	 * awaits the server to which this client is connected to have at least one
	 * worker ready to execute commands.
	 * @throws IOException
	 * @throws PDAsynchBatchTaskExecutorException 
	 * @throws ClassNotFoundException
	 */
	public synchronized void awaitServerWorkers() throws IOException, PDAsynchBatchTaskExecutorException, ClassNotFoundException {
		if (!_initCmdStateInvalidated) 
			throw new PDAsynchBatchTaskExecutorException("PDAsynchBatchTaskExecutorClt.awaitServerWorkers(): must first call sendInitCmd()");
		_oos.writeObject(new PDAsynchBatchTaskExecutorSrvAwaitWrksRequest());
		_oos.flush();
		Object response = _ois.readObject();
		if (response instanceof OKReply) return;
		else throw new PDAsynchBatchTaskExecutorException("PDAsynchBatchTaskExecutorClt.awaitServerWorkers(): srv failed to send OKreply");
	}
	
	
	/**
	 * disconnects from server and frees up resources.
	 * @throws IOException if socket shut-down fails.
	 */
	public static synchronized void disconnect() throws IOException {
		if (_instance!=null) {
			if (_instance._s!=null) {
				_instance._s.shutdownOutput();
				_instance._s.close();
				_instance._ois = null;
				_instance._oos = null;
				_instance._s = null;
			}
		}
	}
	
}

