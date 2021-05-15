package parallel;

import java.io.Serializable;

/**
 * defines a TaskObject that causes the executing thread to sleep for a specific
 * amount of time. The main Use-Case for this class is to enforce a sleep period 
 * specified in the object constructor, so that the 
 * <CODE>parallel.distributed.PDBatchTaskExecutorSrv</CODE> server object (or 
 * derived one) that will be distributing these tasks to the workers in the 
 * network for the purpose of applying a previously set command (set via the 
 * <CODE>setCmd(PDBTExecCmd)</CODE> method) will not face the risk of choosing 
 * the same worker for distributing more than one chunk of these tasks to, which
 * would amount to at least one connected worker not executing the mentioned set
 * command as a result of distributing these tasks in the network.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class TimeDelayTask implements TaskObject {
	//private final static long serialVersionUID = -5265604222839083451L;
	private long _sleepPeriod;
	
	public TimeDelayTask(long sleep_period) {
		_sleepPeriod = sleep_period;
	}
	
	
  /**
   * calls <CODE>Thread.sleep(p)</CODE> for the number of milliseconds p 
	 * specified in this object's constructor.
   * @return null
   */
  public Serializable run() {
		try {
			Thread.sleep(_sleepPeriod);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return null;
	}


  /**
   * return always true.
   * @return boolean
   */
  public boolean isDone() {
		return true;
	}


  /**
   * always throws UnsupportedOperationException
   * @param other not used.
   * @throws UnsupportedOperationException
   */
  public void copyFrom(TaskObject other) {
		throw new UnsupportedOperationException("not supported");
	}
}
