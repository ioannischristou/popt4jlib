package parallel.distributed;

import parallel.*;
import java.io.*;
import java.net.*;
import java.util.*;


/**
 * class implements the "Client" for networks of PDBTExecInitedWrk workers.
 * Connects ONLY to a host server represented by the PDBTExecSingleCltWrkInitSrv 
 * object, to a specific host/port IP address. Once this client object connects
 * to the server, the connection stays on, until the 
 * <CODE>terminateConnection()</CODE> method is invoked (this is unlike the 
 * <CODE>PDBatchTaskExecutorClt</CODE> class where a new connection is 
 * established and closed during each method invocation.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBTExecInitedClt {
  private String _host="localhost";  // default host
  private int _port = 7891;  // default client port
	Socket _s=null;
	ObjectInputStream _ois=null;
	ObjectOutputStream _oos=null;

  /**
   * public no-arg constructor, will assume connection is to be made on
   * localhost, port 7891.
   */
  public PDBTExecInitedClt() {

  }


  /**
   * constructor provides explicitly the server host/port connection parameters.
   * @param hostipaddress String
   * @param port int
   */
  public PDBTExecInitedClt(String hostipaddress, int port) {
    _host = hostipaddress;
    _port = port;
  }


  /**
   * the (internet) IP address of the server this client will be connecting to 
	 * when submitting work.
   * @return String
   */
  public String getHostIPAddress() {
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
  public int getPort() { return _port; }


  /**
   * the main method of the class. Sends over the network the tasks parameter
   * to the PDBatchTaskExecutorSrv server, who then distributes them to one
   * of the available workers in the network. Method blocks until results are
   * retrieved.
   * @param tasks TaskObject[]
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws PDBatchTaskExecutorException
   * @return Object[]
   */
  public Object[] submitWorkFromSameHost(TaskObject[] tasks)
      throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
    if (tasks==null || tasks.length==0)
      throw new PDBatchTaskExecutorException("PDBatchTaskExecutorClt.submitWork(tasks): null or empty tasks passed in.");
		synchronized (this) {
      if (_s==null) {
				_s = new Socket(_host, _port);
		    _oos = new ObjectOutputStream(_s.getOutputStream());
				_oos.flush();
				_ois = new ObjectInputStream(_s.getInputStream());
			}
		}
    InetAddress ia = InetAddress.getLocalHost();
    String client_addr_port = ia.getHostAddress()+"_"+_port;
		// submit a request for parallel/distributed execution of the tasks
    TaskObjectsParallelExecutionRequest req = new TaskObjectsParallelExecutionRequest(client_addr_port, tasks);
    _oos.writeObject(req);
    _oos.flush();
    Object response = _ois.readObject();
    if (response instanceof TaskObjectsExecutionResults) {
      return ( (TaskObjectsExecutionResults) response)._results;
    }
    else if (response instanceof NoWorkerAvailableResponse)
      throw new PDBatchTaskExecutorException("no worker was available...");
		else if (response instanceof FailedReply) 
			throw new PDBatchTaskExecutorException("at least one worker failed to process request...");
    else throw new PDBatchTaskExecutorException("cannot parse response...");
  }


	/**
	 * this method must be invoked only once, prior to any other invocation of the 
	 * <CODE>submitWorkFromSameHost()</CODE> method. It sends to the server the
	 * RRObject to be executed on each worker connected/to-be-connected to the 
	 * server, in order to initialize its state. 
	 * @param task RRObject
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDBatchTaskExecutorException 
	 */
	public void submitInitCmd(RRObject task) 
      throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
    if (task==null)
      throw new PDBatchTaskExecutorException("PDBatchTaskExecutorClt.submitInitCmd(task): null or empty task passed in.");
		utils.Messenger mger = utils.Messenger.getInstance();
		synchronized (this) {
      if (_s==null) {
				mger.msg("PDTExecInitedClt.submitInitCmd(): creating socket connection",1);
				_s = new Socket(_host, _port);
		    _oos = new ObjectOutputStream(_s.getOutputStream());
				_oos.flush();
				_ois = new ObjectInputStream(_s.getInputStream());
				mger.msg("PDBTExecInitedClt.submitInitCmd(): connection created.", 1);
			}
		}
		mger.msg("PDTExecInitedClt.submitInitCmd(): sending object="+task,1);
    _oos.writeObject(task);
    _oos.flush();
		mger.msg("PDBTExecInitedClt.submitInitCmd(): object sent.", 1);
    Object response = _ois.readObject();
		mger.msg("PDBTExecInitedClt.submitInitCmd(): done reading response.", 1);
    if (response instanceof OKReply) return;
    else if (response instanceof NoWorkerAvailableResponse)
      throw new PDBatchTaskExecutorException("no worker was available...");
		else if (response instanceof FailedReply) 
			throw new PDBatchTaskExecutorException("at least one worker failed to process request...");
    else throw new PDBatchTaskExecutorException("cannot parse response...");
	}
	
	
	public synchronized void terminateConnection() throws IOException {
		if (_s!=null) _s.close();
	}
	
}

