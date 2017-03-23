package parallel.distributed;

import parallel.*;
import java.io.*;

/**
 * implements a DMsgIntf object that asks a DConditionCounterSrv to decrement 
 * its associated ConditionCounter. Not for use as part of the normal public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DConditionCounterDecrRequest implements DMsgIntf {
  // private final static long serialVersionUID = -5265604222839083451L;
	private int _num;


  /**
   * no-arg constructor implies decrement by 1.
   */
  public DConditionCounterDecrRequest() {
		_num=1;
  }
	
	
	/**
	 * constructor sets the size of the decrement request.
	 * @param num int
	 */
	public DConditionCounterDecrRequest(int num) {
		_num = num;
	}


  /**
   * decrements the ConditionCounter (via sockets) to the DConditionCounter 
	 * object associated with the server it is sent to.
   * @param oos ObjectOutputStream
   * @throws ParallelException
   * @throws IOException
   */
  public void execute(ObjectOutputStream oos) throws ParallelException, IOException {
    DConditionCounter.getInstance().decrement(_num);
    // ok, handling thread on server decreased the associated unique CondCounter
		// no need for reset
    oos.writeObject(new OKReply());
    oos.flush();
  }

	
  public String toString() {
    return "DConditionCounterDecrRequest():_num="+_num;
  }

}

