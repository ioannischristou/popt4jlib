package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;

/**
 * Same functionality as <CODE>DConditionCounter[Clt|Srv]</CODE> classes, but
 * use long-lived connections, for clients that call frequently incr/decr ops.
 * When no longer needed, client must call <CODE>closeConnection()</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DConditionCounterLLCClt {
  private String _host = "localhost";
  private int _port = 7899;
	private String _coordname = "DCondCntCoord_"+_host+"_"+_port;
	private DActiveMsgPassingCoordinatorLongLivedConnClt _coordclt;

  /**
   * no-arg constructor (no-op) assumes the following defaults:
	 * <ul>
   * <li> host="localhost"
   * <li> port=7899
	 * </ul>
	 * @throws UnknownHostException
	 * @throws IOException
   */
  public DConditionCounterLLCClt() throws UnknownHostException, IOException {
		_coordclt = new DActiveMsgPassingCoordinatorLongLivedConnClt(_host, _port, _coordname);
  }


  /**
   * constructor specifies an IP address by first two argument parameters and 
	 * the coord-name specified in the third parameter.
   * @param host String
   * @param port int
   * @param coordname String
	 * @throws UnknownHostException
	 * @throws IOException
   */
  public DConditionCounterLLCClt(String host, int port, String coordname)
      throws UnknownHostException, IOException {
    // initialization
    _host = host;
    _port = port;
    _coordname = coordname;
		_coordclt = new DActiveMsgPassingCoordinatorLongLivedConnClt(_host, _port, _coordname);
  }

	
	/**
	 * increments the associated distributed condition counter.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException 
	 */
	public synchronized void increment() throws IOException, ClassNotFoundException, ParallelException {
		_coordclt.sendData(-1, new DConditionCounterIncrRequest());
	}

	
	/**
	 * increments the associated distributed condition counter.
	 * @param num int
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException 
	 */
	public synchronized void increment(int num) throws IOException, ClassNotFoundException, ParallelException {
		_coordclt.sendData(-1, new DConditionCounterIncrRequest(num));
	}

	
	/**
	 * decrements the associated distributed condition counter.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException 
	 */
	public synchronized void decrement() throws IOException, ClassNotFoundException, ParallelException {
		_coordclt.sendData(-1, new DConditionCounterDecrRequest());
	}

	
	/**
	 * resets the associated distributed condition counter.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException 
	 */
	public synchronized void reset() throws IOException, ClassNotFoundException, ParallelException {
		_coordclt.sendData(-1, new DConditionCounterResetRequest());
	}
	
	
	/**
	 * awaits for the associated distributed condition counter to reach zero.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException 
	 */
	public void await() throws IOException, ClassNotFoundException, ParallelException {
		// notice the use (valid) of the DMsgPassingCoordinatorClt to connect to a
		// DActiveMsgPassingLongLivedConnSrv!
		DMsgPassingCoordinatorClt coordclt = new DMsgPassingCoordinatorClt(_host,_port,_coordname);
		coordclt.sendData(new DConditionCounterAwaitRequest());
	}

	
	/**
	 * close the long-lived connection to the DConditionCounterLLCSrv.
	 * @throws IOException 
	 */
	public synchronized void closeConnection() throws IOException {
		_coordclt.closeConnection();
	}
}

