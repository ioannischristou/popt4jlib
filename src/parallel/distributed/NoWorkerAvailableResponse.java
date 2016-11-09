package parallel.distributed;

import parallel.*;
import java.io.Serializable;

/**
 * auxiliary class used for wrapping an array of TaskObjects to send back to
 * the server in the event a worker cannot handle them.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class NoWorkerAvailableResponse implements Serializable {
  private final static long serialVersionUID = -3114134659102563212L;
  TaskObject[] _tasks;

  NoWorkerAvailableResponse(TaskObject[] tasks) {
    _tasks = tasks;
  }

}
