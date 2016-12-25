package parallel.distributed;

/**
 * indicates an exception in the execution of a task collection in a remote
 * PDAsynchBatchTaskExecutor[Wrk].
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDAsynchBatchTaskExecutorException extends PDBatchTaskExecutorException {
  public PDAsynchBatchTaskExecutorException(String msg) {
    super("PDAsynchBatchTaskExecutorException:"+msg);
    //System.err.println(msg);
  }
}
