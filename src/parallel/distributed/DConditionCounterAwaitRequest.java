package parallel.distributed;

import parallel.*;
import java.io.*;

/**
 * implements a DMsgIntf object that asks a DConditionCounterSrv to await 
 * its associated ConditionCounter until it reaches zero. Not for use as part of 
 * the normal public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DConditionCounterAwaitRequest implements DMsgIntf {
  // private final static long serialVersionUID = -5265604222839083451L;


  /**
   * single constructor.
   */
  public DConditionCounterAwaitRequest() {
  }


  /**
   * awaits the ConditionCounter (via sockets) to the DConditionCounter 
	 * object associated with the server it is sent to.
   * @param oos ObjectOutputStream
   * @throws ParallelException
   * @throws IOException
   */
  public void execute(ObjectOutputStream oos) throws ParallelException, IOException {
    DConditionCounter.getInstance().await();
    // ok, handling thread on server awaited the associated unique CondCounter
		// no need for reset
    oos.writeObject(new OKReply());
    oos.flush();
  }

  public String toString() {
    return "DConditionCounterAwaitRequest()";
  }

}

