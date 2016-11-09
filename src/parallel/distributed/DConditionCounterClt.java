package parallel.distributed;

import parallel.*;
import java.net.*;
import java.io.*;

/**
 * class implements a distributed <CODE>parallel.ConditionCounter</CODE> across 
 * many threads living in many (remote) JVMs. Such a class is useful when 
 * tasks must be distributed across a network of JVMs to be run asynchronously;
 * in such cases, the spawning process often needs to know when all tasks have
 * finished executing, and this class provides such a means. 
 * The distributed condition counter can be used as follows: every task to be 
 * distributed, upon creation (in its constructor) or even before in its caller, 
 * increments the distributed condition counter, and upon finishing execution, 
 * decrements that same distributed condition counter. The main process that 
 * spawned the original task(s) (and may have incremented the condition counter
 * in the first place instead of the constructors), awaits on the same 
 * distributed condition counter. When the awaiting is completed, the process 
 * knows that all tasks have been successfully executed. The class fits well 
 * with the <CODE>PDAsynchBatchTaskExecutor[Clt|Srv|Wrk]</CODE> framework.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DConditionCounterClt {
  private String _host = "localhost";
  private int _port = 7899;
	private String _coordname = "DCondCntCoord_"+_host+"_"+_port;


  /**
   * no-arg constructor (no-op) assumes the following defaults:
	 * <ul>
   * <li> host="localhost"
   * <li> port=7899
	 * </ul>
   */
  public DConditionCounterClt() {
		// no-op
  }


  /**
   * constructor specifies an IP address by first two argument parameters and 
	 * the coord-name specified in the third parameter.
   * @param host String
   * @param port int
   * @param coordname String
   */
  public DConditionCounterClt(String host, int port, String coordname) {
    // initialization
    _host = host;
    _port = port;
    _coordname = coordname;
  }

	
	/**
	 * increments the associated distributed condition counter.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException 
	 */
	public void increment() throws IOException, ClassNotFoundException, ParallelException {
		DMsgPassingCoordinatorClt coordclt = new DMsgPassingCoordinatorClt(_host,_port,_coordname);
		coordclt.sendData(new DConditionCounterIncrRequest());
	}

	
	/**
	 * increments the associated distributed condition counter.
	 * @param num int
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException 
	 */
	public void increment(int num) throws IOException, ClassNotFoundException, ParallelException {
		DMsgPassingCoordinatorClt coordclt = new DMsgPassingCoordinatorClt(_host,_port,_coordname);
		coordclt.sendData(new DConditionCounterIncrRequest(num));
	}

	
	/**
	 * decrements the associated distributed condition counter.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException 
	 */
	public void decrement() throws IOException, ClassNotFoundException, ParallelException {
		DMsgPassingCoordinatorClt coordclt = new DMsgPassingCoordinatorClt(_host,_port,_coordname);
		coordclt.sendData(new DConditionCounterDecrRequest());
	}

	
	/**
	 * resets the associated distributed condition counter.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException 
	 */
	public void reset() throws IOException, ClassNotFoundException, ParallelException {
		DMsgPassingCoordinatorClt coordclt = new DMsgPassingCoordinatorClt(_host,_port,_coordname);
		coordclt.sendData(new DConditionCounterResetRequest());
	}
	
	
	/**
	 * awaits for the associated distributed condition counter to reach zero.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException 
	 */
	public void await() throws IOException, ClassNotFoundException, ParallelException {
		DMsgPassingCoordinatorClt coordclt = new DMsgPassingCoordinatorClt(_host,_port,_coordname);
		coordclt.sendData(new DConditionCounterAwaitRequest());
	}

	
	/**
	 * shuts-down the associated distributed condition counter server.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ParallelException 
	 */
	public void shutDownSrv() throws IOException, ClassNotFoundException, ParallelException {
		DMsgPassingCoordinatorClt coordclt = new DMsgPassingCoordinatorClt(_host,_port,_coordname);
		coordclt.sendData(new DConditionCounterShutDownRequest());
	}

}

