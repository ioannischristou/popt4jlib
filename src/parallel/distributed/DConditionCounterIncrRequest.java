package parallel.distributed;

import parallel.*;
import java.io.*;

/**
 * implements a DMsgIntf object that asks a DConditionCounterSrv to increment 
 * its associated ConditionCounter. Not for use as part of the normal public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DConditionCounterIncrRequest implements DMsgIntf {
  // private final static long serialVersionUID = -5265604222839083451L;
	int _num=1;

  /**
   * no-arg constructor.
   */
  public DConditionCounterIncrRequest() {
  }
	
	
	/**
	 * constructor sets the value that the request asks to increment the condition
	 * counter by.
	 * @param num int
	 */
	public DConditionCounterIncrRequest(int num) {
		_num = num;
	}


  /**
   * increments the ConditionCounter (via sockets) to the DConditionCounter 
	 * object associated with the server it is sent to.
   * @param oos ObjectOutputStream
   * @throws ParallelException
   * @throws IOException
   */
  public void execute(ObjectOutputStream oos) throws ParallelException, IOException {
    DConditionCounter.getInstance().increment(_num);
    // ok, handling thread on server increased the associated unique CondCounter
    oos.writeObject(new OKReply());
    oos.flush();
  }

  public String toString() {
    return "DConditionCounterIncrRequest()";
  }

}

