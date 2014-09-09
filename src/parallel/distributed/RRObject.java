package parallel.distributed;

import parallel.*;
import java.io.*;
import java.util.Vector;

/**
 * auxiliary base-class wrapping up Requests/Results for TaskObjects processing.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public abstract class RRObject implements Serializable {
  public abstract void runProtocol(PDBatchTaskExecutorSrv srv,
                                   ObjectInputStream ois,
                                   ObjectOutputStream oos)
      throws IOException, ClassNotFoundException, PDBatchTaskExecutorException;
}


/**
 * auxiliary class wrapping a request for processing TaskObjects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class TaskObjectsExecutionRequest extends RRObject {
  private final static long serialVersionUID = 5801899648236803371L;
  TaskObject[] _tasks;
  Vector _originatingClients;  // Vector<String>  ordered by event time


  /**
   * sole public constructor.
   * @param originating_client String
   * @param tasks TaskObject[]
   */
  public TaskObjectsExecutionRequest(String originator, TaskObject[] tasks) {
    _tasks = tasks;
    _originatingClients = new Vector();
    _originatingClients.addElement(originator);
  }


  /**
   * constructor used only by servers that have a client connection to
   * other servers.
   * @param originators Vector
   * @param tasks TaskObject[]
   */
  TaskObjectsExecutionRequest(Vector originators, TaskObject[] tasks) {
    _tasks = tasks;
    _originatingClients = originators;
  }


  /**
   * finds a free worker and submits the tasks for processing, then sends
   * the results back to the requestor. In case no worker is available,
   * sends back the tasks, wrapped in a NoWorkerAvailableResponse object.
   * @param srv PDBatchTaskExecutorSrv
   * @param ois ObjectInputStream
   * @param oos ObjectOutputStream
   * @throws IOException
   */
  public void runProtocol(PDBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
    if (_tasks==null || _tasks.length==0)
      throw new PDBatchTaskExecutorException("TaskObjectsExecutionRequest.runProtocol(): null or empty _tasks?");
    // 1. find an available worker on the net, submit work, wait for results
    TaskObjectsExecutionResults results = srv.submitWork(_originatingClients, _tasks);
    // 2. send back the results to the requestor
    oos.writeObject(results);
    oos.flush();
    return;
  }
}


/**
 * auxiliary class wrapping the results of processing a TaskObject[].
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class TaskObjectsExecutionResults extends RRObject {
  private final static long serialVersionUID = 8784903283598461916L;
  Object[] _results;  // must be Serializable


  /**
   * sole public constructor.
   * @param results Object[]
   */
  public TaskObjectsExecutionResults(Object[] results) {
    _results = results;
  }


  /**
   * sends back the results to the requestor (forwards).
   * @param ois ObjectInputStream
   * @param oos ObjectOutputStream
   * @throws IOException
   */
  public void runProtocol(PDBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) throws IOException, PDBatchTaskExecutorException {
    if (_results==null || _results.length==0)
      throw new PDBatchTaskExecutorException("TaskObjectsExecutionResults.runProtocol(): null or empty _results?");
    oos.writeObject(this);
    oos.flush();
    return;
  }
}

