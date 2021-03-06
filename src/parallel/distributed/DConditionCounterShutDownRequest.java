package parallel.distributed;

import parallel.*;
import java.io.*;

/**
 * implements a DMsgIntf object that asks a DConditionCounterSrv to shut-down. 
 * Not for use as part of the normal public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DConditionCounterShutDownRequest implements DMsgIntf {
  // private final static long serialVersionUID = -5265604222839083451L;


  /**
   * single constructor.
   */
  public DConditionCounterShutDownRequest() {
  }


  /**
   * shuts-down the ConditionCounterSrv it is sent to.
   * @param oos ObjectOutputStream
   * @throws ParallelException
   * @throws IOException
   */
  public void execute(ObjectOutputStream oos) throws ParallelException, IOException {
    // no need for reset
		oos.writeObject(new OKReply());
    oos.flush();
    DConditionCounter.getInstance().shutDown();
  }

	/**
	 * return &quot;DConditionCounterShutDownRequest()&quot;.
	 * @return String
	 */
  public String toString() {
    return "DConditionCounterShutDownRequest()";
  }

}

