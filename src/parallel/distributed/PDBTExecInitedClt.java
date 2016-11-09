package parallel.distributed;

import parallel.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * thread-safe class implements the "Client" for networks of PDBTExecInitedWrk 
 * workers.
 * Connects ONLY to a host server represented by the PDBTExec[SingleCltWrk]Init[~ed]Srv
 * object, to a specific host/port IP address. Once this client object connects
 * to the server, the connection stays on, until the
 * <CODE>terminateConnection()</CODE> method is invoked (this is unlike the
 * <CODE>PDBatchTaskExecutorClt</CODE> class where a new connection is
 * established and closed during each method invocation.)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBTExecInitedClt {
	private String _host="localhost";  // default host
	private int _port=7891;  // default client port
	Socket _s=null;
	ObjectInputStream _ois=null;
	ObjectOutputStream _oos=null;

	/**
	 * public no-arg constructor, will assume connection is to be made on
	 * localhost, port 7891.
	 */
	public PDBTExecInitedClt() {
		// no-op
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

	
	/*
	 * The code below is not thread-safe and thus is commented out. The obvious
	 * reason is that two threads calling this code, may easily use the shared 
	 * _oos to send a request, and wait for results on the same _ois, which would
	 * of course totally mess up the other end of the socket. The situation cannot
	 * be redeemed by simply guarding the use of the _ois and _oos members.
	 * the main method of the class. Sends over the network the tasks parameter to
	 * the PDBTExec[SingleCltWrk]Init[~ed]Srv server, who then distributes them to 
	 * one of the available workers in the network. Method blocks until results 
	 * are retrieved.
	 * @param tasks TaskObject[]
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDBatchTaskExecutorException
	 * @return Object[]
	 *
	public Object[] submitWorkFromSameHost(TaskObject[] tasks)
		throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
		if (tasks==null || tasks.length==0)
			throw new PDBatchTaskExecutorException("PDBTExecInitedClt.submitWork(tasks): null or empty tasks passed in.");
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
		if (response instanceof TaskObjectsExecutionResults) 
			return ((TaskObjectsExecutionResults) response)._results;
		else if (response instanceof NoWorkerAvailableResponse) 
			throw new PDBatchTaskExecutorException("no worker was available...");
	  else if (response instanceof FailedReply) 
			throw new PDBatchTaskExecutorException("at least one worker failed to process request...");
		else 
			throw new PDBatchTaskExecutorException("cannot parse response...");
	}
  */

	
	/**
	 * the main method of the class. Sends over the network the tasks parameter to
	 * the PDBTExec[SingleCltWrk]Init[~ed]Srv server, who then distributes them to 
	 * one of the available workers in the network. Method blocks until results 
	 * are retrieved.
	 * @param tasks TaskObject[]
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDBatchTaskExecutorException
	 * @return Object[]
	 */
	public synchronized Object[] submitWorkFromSameHost(TaskObject[] tasks)
		throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
		if (tasks==null || tasks.length==0)
			throw new PDBatchTaskExecutorException("PDBTExecInitedClt.submitWork(tasks): null or empty tasks passed in.");
		if (_s==null)
			throw new PDBatchTaskExecutorException("PDBTExecInitedClt.submitWork(tasks): no connection to server (submitInitCmd() likely not called yet)");
		InetAddress ia = InetAddress.getLocalHost();
		String client_addr_port = ia.getHostAddress()+"_"+_port;
		// submit a request for parallel/distributed execution of the tasks
		TaskObjectsParallelExecutionRequest req = new TaskObjectsParallelExecutionRequest(client_addr_port, tasks);
		_oos.writeObject(req);
		_oos.flush();
		Object response = _ois.readObject();
		if (response instanceof TaskObjectsExecutionResults) 
			return ((TaskObjectsExecutionResults) response)._results;
		else if (response instanceof NoWorkerAvailableResponse) 
			throw new PDBatchTaskExecutorException("no worker was available...");
	  else if (response instanceof FailedReply) 
			throw new PDBatchTaskExecutorException("at least one worker failed to process request...");
		else 
			throw new PDBatchTaskExecutorException("cannot parse response...");
	}

	
	/*
	 * The code below is not thread-safe and thus is commented out. The obvious
	 * reason is that two threads calling this code, may easily use the shared 
	 * _oos to send a request, and wait for results on the same _ois, which would
	 * of course totally mess up the other end of the socket. The situation cannot
	 * be redeemed by simply guarding the use of the _ois and _oos members.
	 * same as the main method of the class 
	 * <CODE>submitWorkFromSameHost(tasks)</CODE>, except it also specifies the
	 * granularity of the breaking-up of the array of tasks into batches to 
	 * be submitted to the workers.
	 * @param tasks TaskObject[]
	 * @param grainsize int a value of 1 means the tasks are to be divided exactly
	 * among as many batches as there are workers currently available.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDBatchTaskExecutorException
	 * @return Object[]
	 *
	public Object[] submitWorkFromSameHost(TaskObject[] tasks, int grainsize)
		throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
		if (tasks==null || tasks.length==0)
			throw new PDBatchTaskExecutorException("PDBTExecInitedClt.submitWork(tasks): null or empty tasks passed in.");
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
		TaskObjectsParallelExecutionRequest req = new TaskObjectsParallelExecutionRequest(client_addr_port, tasks, grainsize);
		_oos.writeObject(req);
		_oos.flush();
		Object response = _ois.readObject();
		if (response instanceof TaskObjectsExecutionResults) 
			return ((TaskObjectsExecutionResults) response)._results;
		else if (response instanceof NoWorkerAvailableResponse) 
			throw new PDBatchTaskExecutorException("no worker was available...");
	  else if (response instanceof FailedReply) 
			throw new PDBatchTaskExecutorException("at least one worker failed to process request...");
		else 
			throw new PDBatchTaskExecutorException("cannot parse response...");
	}
  */
	
	
	/**
	 * same as the main method of the class 
	 * <CODE>submitWorkFromSameHost(tasks)</CODE>, except it also specifies the
	 * granularity of the breaking-up of the array of tasks into batches to 
	 * be submitted to the workers.
	 * @param tasks TaskObject[]
	 * @param grainsize int a value of 1 means the tasks are to be divided exactly
	 * among as many batches as there are workers currently available.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDBatchTaskExecutorException
	 * @return Object[]
	 */
	public synchronized Object[] submitWorkFromSameHost(TaskObject[] tasks, int grainsize)
		throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
		if (tasks==null || tasks.length==0)
			throw new PDBatchTaskExecutorException("PDBTExecInitedClt.submitWork(tasks,gsz): null or empty tasks passed in.");
		if (_s==null) 
			throw new PDBatchTaskExecutorException("PDBTExecInitedClt.submitWork(tasks,gsz): no connection to server (submitInitCmd() likely not called yet)");
		InetAddress ia = InetAddress.getLocalHost();
		String client_addr_port = ia.getHostAddress()+"_"+_port;
		// submit a request for parallel/distributed execution of the tasks
		TaskObjectsParallelExecutionRequest req = new TaskObjectsParallelExecutionRequest(client_addr_port, tasks, grainsize);
		_oos.writeObject(req);
		_oos.flush();
		Object response = _ois.readObject();
		if (response instanceof TaskObjectsExecutionResults) 
			return ((TaskObjectsExecutionResults) response)._results;
		else if (response instanceof NoWorkerAvailableResponse) 
			throw new PDBatchTaskExecutorException("no worker was available...");
	  else if (response instanceof FailedReply) 
			throw new PDBatchTaskExecutorException("at least one worker failed to process request...");
		else 
			throw new PDBatchTaskExecutorException("cannot parse response...");
	}

	
	/*
	 * The code below is not thread-safe and thus is commented out. The obvious
	 * reason is that two threads calling this code, may easily use the shared 
	 * _oos to send a request, and wait for results on the same _ois, which would
	 * of course totally mess up the other end of the socket. The situation cannot
	 * be redeemed by simply guarding the use of the _ois and _oos members.
	 * this method must be invoked only once, prior to any other invocation of the
	 * <CODE>submitWorkFromSameHost()</CODE> method. It sends to the server the
	 * RRObject to be executed on each worker connected/to-be-connected to the
	 * server, in order to initialize its state.
	 * @param task RRObject
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDBatchTaskExecutorException
	 *
	public void submitInitCmd(RRObject task)
		throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
		if (task == null) {
			throw new PDBatchTaskExecutorException("PDBatchTaskExecutorClt.submitInitCmd(task): null or empty task passed in.");
		}
		utils.Messenger mger = utils.Messenger.getInstance();
		synchronized (this) {
			if (_s == null) {
				mger.msg("PDTExecInitedClt.submitInitCmd(): creating socket connection", 1);
				_s = new Socket(_host, _port);
				_oos = new ObjectOutputStream(_s.getOutputStream());
				_oos.flush();
				_ois = new ObjectInputStream(_s.getInputStream());
				mger.msg("PDBTExecInitedClt.submitInitCmd(): connection created.", 1);
			}
		}
		mger.msg("PDTExecInitedClt.submitInitCmd(): sending object=" + task, 1);
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
  */
	
	
	/**
	 * this method must be invoked only once, prior to any other invocation of the
	 * <CODE>submitWorkFromSameHost()</CODE> method. It sends to the server the
	 * RRObject to be executed on each worker connected/to-be-connected to the
	 * server, in order to initialize its state.
	 * @param task RRObject
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDBatchTaskExecutorException if the method was called before or
	 * if task passed in was null
	 */
	public synchronized void submitInitCmd(RRObject task)
		throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
		if (task == null) {
			throw new PDBatchTaskExecutorException("PDBatchTaskExecutorClt.submitInitCmd(task): null or empty task passed in.");
		}
		utils.Messenger mger = utils.Messenger.getInstance();
		if (_s == null) {
			mger.msg("PDTExecInitedClt.submitInitCmd(): creating socket connection", 1);
			_s = new Socket(_host, _port);
			_oos = new ObjectOutputStream(_s.getOutputStream());
			_oos.flush();
			_ois = new ObjectInputStream(_s.getInputStream());
			mger.msg("PDBTExecInitedClt.submitInitCmd(): connection created.", 1);
		} else {
			throw new PDBatchTaskExecutorException("PDBatchTaskExecutorClt.submitInitCmd(task): has been called before.");
		}
		mger.msg("PDTExecInitedClt.submitInitCmd(): sending object=" + task, 1);
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


	/**
	 * closes the connection to the server, causing server to release resources.
	 * @throws IOException 
	 */
	public synchronized void terminateConnection() throws IOException {
		if (_s!=null) {
			_s.shutdownOutput();
			_s.close();
			_s = null;
		}
	}

}

