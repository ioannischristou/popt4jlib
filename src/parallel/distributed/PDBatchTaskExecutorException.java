package parallel.distributed;

/**
 * indicates an exception in the execution of a task collection in a remote
 * PDBatchTaskExecutor[Wrk].
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBatchTaskExecutorException extends Exception {
  private String _msg;
  public PDBatchTaskExecutorException(String msg) {
    _msg = msg;
    System.err.println(msg);
  }
}
