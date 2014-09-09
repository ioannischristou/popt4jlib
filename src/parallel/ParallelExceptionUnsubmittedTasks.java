package parallel;

import java.util.Vector;

/**
 * encapsulates any unsubmitted tasks to executors.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ParallelExceptionUnsubmittedTasks extends Exception {
  private Vector _unsubmittedTasks;

  /**
   * constucts an exception object, and adds inside the unsubmitted tasks from
   * an executor's execute*() method.
   * @param unsubmittedtasks Vector
   */
  public ParallelExceptionUnsubmittedTasks(Vector unsubmittedtasks) {
    _unsubmittedTasks = unsubmittedtasks;
  }


  /**
   * return the unsubmitted tasks that were collected inside this exception object.
   * @return Vector
   */
  public Vector getUnsubmittedTasks() {
    return _unsubmittedTasks;
  }
}

