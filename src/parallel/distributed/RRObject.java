package parallel.distributed;

import parallel.*;
import java.io.*;
import java.util.Vector;
import java.util.ArrayList;

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
   * @param originator String the name of the originating client
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
   * @param originators Vector  // Vector&lt;String&gt;
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
	 * @throws ClassNotFoundException
	 * @throws PDBatchTaskExecutorException
   */
  public void runProtocol(PDBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
    if (_tasks==null || _tasks.length==0)
      throw new PDBatchTaskExecutorException("TaskObjectsExecutionRequest.runProtocol(): null or empty _tasks?");
		TaskObjectsExecutionResults results=null;
		try {
			// 1. find an available worker on the net, submit work, wait for results
			results = srv.submitWork(_originatingClients, _tasks);
		}
		catch (IOException e) {  // worker disconnected, try one last time
			utils.Messenger.getInstance().msg("worker connection lost, will try one last time", 1);
			results = srv.submitWork(_originatingClients, _tasks);
		}
		// 2. send back the results to the requestor
		oos.writeObject(results);
		oos.flush();
    return;
  }
}


/**
 * auxiliary class wrapping a request for processing asynchronously TaskObjects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class TaskObjectsAsynchExecutionRequest extends RRObject {
  //private final static long serialVersionUID = 5801899648236803371L;
  TaskObject[] _tasks;
  Vector _originatingClients;  // Vector<String>  ordered by event time


  /**
   * sole public constructor.
   * @param originator String the name of the originating client
   * @param tasks TaskObject[]
   */
  public TaskObjectsAsynchExecutionRequest(String originator, TaskObject[] tasks) {
    _tasks = tasks;
    _originatingClients = new Vector();
    _originatingClients.addElement(originator);
  }


  /**
   * constructor used only by servers that have a client connection to
   * other servers.
   * @param originators Vector  // Vector&lt;String&gt;
   * @param tasks TaskObject[]
   */
  TaskObjectsAsynchExecutionRequest(Vector originators, TaskObject[] tasks) {
    _tasks = tasks;
    _originatingClients = originators;
  }


  /**
   * finds a free worker and submits the tasks for processing, then sends
   * OKReply back to the requestor. In case no worker is available,
   * sends back the tasks, wrapped in a NoWorkerAvailableResponse object.
   * @param srv PDAsynchBatchTaskExecutorSrv
   * @param ois ObjectInputStream
   * @param oos ObjectOutputStream
   * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDAsynchBatchTaskExecutorException
   */
  public void runProtocol(PDAsynchBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) 
		throws IOException, ClassNotFoundException, PDAsynchBatchTaskExecutorException {
    if (_tasks==null || _tasks.length==0)
      throw new PDAsynchBatchTaskExecutorException("TaskObjectsAsynchExecutionRequest.runProtocol(): null or empty _tasks?");
		try {
			// 1. find an available worker on the net, submit work
			srv.submitWork(_originatingClients, _tasks);
		}
		catch (IOException e) {  // worker disconnected, try one last time
			utils.Messenger.getInstance().msg("worker connection lost, will try one last time", 1);
			srv.submitWork(_originatingClients, _tasks);
		}
		// 2. send back OKReply to requestor
		oos.writeObject(new OKReply());
		oos.flush();
		// return;
  }
	
	
	/**
	 * unsupported method always throws PDAsynchBatchTaskExecutorException.
	 * @param srv PDBatchTaskExecutorSrv
	 * @param ois ObjectInputStream
	 * @param oos ObjectOutputStream
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDAsynchBatchTaskExecutorException 
	 */
  public void runProtocol(PDBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
		throw new PDAsynchBatchTaskExecutorException("Unsupported method");
	}
}


/**
 * auxiliary class wrapping a request for asking for worker availability.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class PDAsynchBatchTaskExecutorWrkAvailabilityRequest extends RRObject {
  //private final static long serialVersionUID = 5801899648236803371L;


  /**
   * sole public constructor.
   */
  public PDAsynchBatchTaskExecutorWrkAvailabilityRequest() {
		// no-op.
  }


  /**
	 * unsupported operation always throws.
   * @param srv PDAsynchBatchTaskExecutorSrv
   * @param ois ObjectInputStream
   * @param oos ObjectOutputStream
	 * @throws PDAsynchBatchTaskExecutorException
   */
  public void runProtocol(PDAsynchBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) 
		throws PDAsynchBatchTaskExecutorException {
    throw new PDAsynchBatchTaskExecutorException("Unsupported method");
  }
	
	
	/**
	 * unsupported method always throws PDAsynchBatchTaskExecutorException.
	 * @param srv PDBatchTaskExecutorSrv
	 * @param ois ObjectInputStream
	 * @param oos ObjectOutputStream
	 * @throws PDAsynchBatchTaskExecutorException 
	 */
  public void runProtocol(PDBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) throws PDAsynchBatchTaskExecutorException {
		throw new PDAsynchBatchTaskExecutorException("Unsupported method");
	}
}


/**
 * auxiliary class wrapping a request for processing in parallel TaskObjects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class TaskObjectsParallelExecutionRequest extends TaskObjectsExecutionRequest {
  // private final static long serialVersionUID = 5801899648236803371L;
	private final static int _MAX_NUM_THREADS_TO_USE = 64;  // max. number of threads to use to submit batches
	private final static int _GRAIN_SIZE=3;  // this size controls the granularity 
	// of the batch jobs to be submitted to the server to forward to workers, 
	// unless it is overridden by the value below; a grain-size of 1, implies that
	// the tasks will be divided (normally) among the number of workers known to 
	// the system and submitted to them in a single batch for each; a grain-size
	// of 2, implies that there will be twice as many batches as there are workers
	// known to the system, and each batch independently submitted to the server,
	// and so on.
	private int _grainSize=-1;  // if positive, use this size as grain-size


  /**
   * sole public constructor.
   * @param originator String the name of the originating client
   * @param tasks TaskObject[]
   */
  public TaskObjectsParallelExecutionRequest(String originator, TaskObject[] tasks) {
		super(originator, tasks);
  }

	
  /**
   * sole public constructor.
   * @param originator String the name of the originating client
   * @param tasks TaskObject[]
	 * @param grainsize int
   */
  public TaskObjectsParallelExecutionRequest(String originator, TaskObject[] tasks, int grainsize) {
		super(originator, tasks);
		_grainSize = grainsize;
  }


  /**
   * constructor used only by servers that have a client connection to
   * other servers.
   * @param originators Vector  // Vector&lt;String&gt;
   * @param tasks TaskObject[]
   */
  TaskObjectsParallelExecutionRequest(Vector originators, TaskObject[] tasks) {
		super(originators, tasks);
  }


  /**
   * constructor used only by servers that have a client connection to
   * other servers.
   * @param originators Vector  // Vector&lt;String&gt;
   * @param tasks TaskObject[]
	 * @param grainsize int the grain-size to use when breaking up the tasks into
	 * chunks (batches) to send to workers to execute
   */
  TaskObjectsParallelExecutionRequest(Vector originators, TaskObject[] tasks, int grainsize) {
		super(originators, tasks);
		_grainSize = grainsize;
  }


  /**
   * breaks tasks into batches, and for each batch (in parallel) finds a free 
	 * worker and submits the tasks for processing, then gathers and sends all the 
	 * results back to the requestor. In case no worker is available,
   * sends back the tasks, wrapped in a <CODE>NoWorkerAvailableResponse</CODE> 
	 * object. If any worker fails during processing, a <CODE>FailedReply</CODE> 
	 * object is sent back to the client.
   * @param srv PDBatchTaskExecutorSrv
   * @param ois ObjectInputStream
   * @param oos ObjectOutputStream
   * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws PDBatchTaskExecutorException
   */
  public void runProtocol(PDBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
    if (_tasks==null || _tasks.length==0)
      throw new PDBatchTaskExecutorException("TaskObjectsParallelExecutionRequest.runProtocol(): null or empty _tasks?");
		ArrayList batches = new ArrayList();  // ArrayList<TaskObject[]>
		int grain_size = _grainSize <= 0 ? _GRAIN_SIZE : _grainSize;
		int num_threads = grain_size*srv.getNumWorkers();  // #threads up to grain_size*#workers_currently_available
		if (num_threads>_MAX_NUM_THREADS_TO_USE) num_threads = _MAX_NUM_THREADS_TO_USE;
		// when num_threads > srv.getNumWorkers(), it is necessary that the 
		// srv.submitWork(clients, tasks); method does not throw after a small number
		// of attempts to find a free worker. The PDBTExecSingleCltWrkInitSrv class
		// method, does just that.
		// reduce num_threads if needed
		if (num_threads > _tasks.length) num_threads = _tasks.length;
		if (srv.getNumWorkers()==1) num_threads = 1;  // corner case: with one worker, breaking up tasks makes no sense
		int max_num_tasks_per_batch = _tasks.length / num_threads;
		// if (max_num_tasks_per_batch<1) max_num_tasks_per_batch = 1;  // redundant corner case
		int num = _tasks.length < max_num_tasks_per_batch ? _tasks.length :
			                                                  max_num_tasks_per_batch;
		TaskObject[] batch = new TaskObject[num];
		int cnt = 0;
		int remaining = _tasks.length;
		for (int i=0; i<_tasks.length; i++) {
			if (cnt==batch.length) {
				batches.add(batch);
				num = remaining < max_num_tasks_per_batch ? remaining : max_num_tasks_per_batch;
				if (batches.size()==num_threads-1) 
					num = remaining;  // it's the last batch available, so make enough space for all remaining tasks
				batch = new TaskObject[num];
				cnt=0;
			}
			batch[cnt++] = _tasks[i];
			--remaining;
		}
		// add last batch
		batches.add(batch);
		// batches are ready
		if (num_threads>batches.size()) num_threads = batches.size();
		utils.Messenger.getInstance().msg("TaskObjectsParallelExecutionRequest.runProtocol(): created "+
			                                batches.size()+" batches to be ran by "+num_threads+" threads", 1);
		WrkSubmissionThread[] threads = new WrkSubmissionThread[num_threads];
		for (int i=0; i<num_threads; i++) {
			threads[i] = new WrkSubmissionThread(srv, (TaskObject[]) batches.get(i), _originatingClients);
			threads[i].start();
		}
		for (int i=0; i<num_threads; i++) {
			try {
				threads[i].join();
			}
			catch (InterruptedException e) {
				// e.printStackTrace();
				Thread.currentThread().interrupt();  // recommended
			}
		}
    // finally, gather results, and send them back to the client requesting them
		Object[] res = new Object[_tasks.length];
		cnt = 0;
		for (int i=0; i<num_threads; i++) {
			Object[] res_i = threads[i].getResults();
			if (res_i==null) {  // i-th submission failed, send FailedReply and return
				oos.writeObject(new FailedReply());
				oos.flush();
				return;
			}
			for (int j=0; j<res_i.length; j++) res[cnt++] = res_i[j];
		}
    TaskObjectsExecutionResults results = new TaskObjectsExecutionResults(res);
    // finally, send back the results to the requestor
    oos.writeObject(results);
    oos.flush();
    return;
  }
}


/**
 * auxiliary class for parallel processing of a TaskObject[].
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class WrkSubmissionThread extends Thread {
	private TaskObject[] _tasks;
	private Vector _originatingClients;
	private PDBatchTaskExecutorSrv _srv;
	private TaskObjectsExecutionResults _results;
	
	public WrkSubmissionThread(PDBatchTaskExecutorSrv srv, TaskObject[] tasks, Vector originatingClients) {
		_srv = srv;
		_tasks = tasks;
		_originatingClients = originatingClients;
		System.err.println("WrkSubmissionThread(srv,tasks,clts): creating thread for "+tasks.length+" tasks");  // itc: HERE rm asap
	}

	
	public void run() {
		try {
			// find an available worker on the net, submit work, wait for results
			_results = _srv.submitWork(_originatingClients, _tasks);
		}
		catch (IOException e) {  // worker closed connection, try one more time
			utils.Messenger.getInstance().msg("WrkSubmissionThread.run(): current "+
				"worker closed connection, will retry one more time", 1);
			try {
				_results = _srv.submitWork(_originatingClients, _tasks);
			}
			catch (Exception e2) {
				// e2.printStackTrace();
				System.err.println("WrkSubmissionThread.run(): _srv.submitWork(_originatingClients,"+
					                 _tasks.length+" tasks) threw exception '"+e+
					                 "'. Exiting with _results set to null");  // itc: HERE rm asap
				_results = null;
			}
		}
		catch (Exception e) {
			// e.printStackTrace();
			System.err.println("WrkSubmissionThread.run(): _srv.submitWork(_originatingClients,"+
				                 _tasks.length+" tasks) threw exception '"+e+
				                 "'. Exiting with _results set to null");  // itc: HERE rm asap
			_results = null;
		}
	}
	
	
	Object[] getResults() {
		if (_results!=null) return _results._results;
		else return null;
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

