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
 * auxiliary class wrapping a request for processing TaskObjects in networks of
 * <CODE>PDBatchTaskExecutor[Clt|Srv|Wrk]</CODE> objects. Not part of the public 
 * API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class TaskObjectsExecutionRequest extends RRObject {
  private final static long serialVersionUID = 5801899648236803371L;
  protected TaskObject[] _tasks;
  protected Vector _originatingClients;  // Vector<String>  ordered by event time


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
			utils.Messenger.getInstance().msg("worker connection lost, will try one last time", 2);
			results = srv.submitWork(_originatingClients, _tasks);
		}
		// 2. send back the results to the requestor
		oos.writeObject(results);
		oos.flush();
    return;
  }
}


/**
 * auxiliary class wrapping a request for processing asynchronously TaskObjects,
 * in networks of <CODE>PDAsynchBatchTaskExecutor[Clt|Srv|Wrk]</CODE> objects.
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class TaskObjectsAsynchExecutionRequest extends RRObject {
  //private final static long serialVersionUID = 5801899648236803371L;
  TaskObject[] _tasks;  // need package access so they can be accessed from Wrk
  protected Vector _originatingClients;  // Vector<String>  ordered by event time

	/**
	 * public (empty) no-arg constructor, only needed for sub-classes.
	 */
	public TaskObjectsAsynchExecutionRequest() {
		// no-op.
	}
	

  /**
   * public constructor specifies originator host and tasks.
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
			utils.Messenger.getInstance().msg("TAOER.runProtocol(): Wrk connection lost, will try one last time", 2);
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
 * auxiliary class wrapping a request for processing asynchronously TaskObjects,
 * in networks of <CODE>PDAsynchBatchTaskExecutor[Clt|Srv|Wrk]</CODE> objects,
 * breaking them up in smaller chunks if needed. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class TaskObjectsAsynchParallelExecutionRequest extends TaskObjectsAsynchExecutionRequest {
  //private final static long serialVersionUID = 5801899648236803371L;
	private UnboundedBufferArrayUnsynchronized _taskChunksList=null;  // FIFOList<TaskObject[]>
	private int _maxChunkSize=8;  // default max chunk size represents the number
	                              // of CPU cores in most current machines.
	

  /**
   * public constructor specifies originator host and tasks.
   * @param originator String the name of the originating client
   * @param tasks TaskObject[]
   */
  public TaskObjectsAsynchParallelExecutionRequest(String originator, TaskObject[] tasks) {
		super(originator, tasks);
  }

	
  /**
   * public constructor specifies originator host and tasks, plus the maximum
	 * chunk (batch) size to send to the workers at once.
   * @param originator String the name of the originating client
   * @param tasks TaskObject[]
	 * @param max_chunk_size int
	 * @throws IllegalArgumentException if max_chunk_size &lt; 1
   */
  public TaskObjectsAsynchParallelExecutionRequest(String originator, 
		                                               TaskObject[] tasks, 
																									 int max_chunk_size) {
		this(originator, tasks);
		if (max_chunk_size<1) 
			throw new IllegalArgumentException("TaskObjectsAsynchParallelExecutionRequest: max_chunk_size="+max_chunk_size);
		_maxChunkSize = max_chunk_size;
  }


  /**
   * constructor used only by servers that have a client connection to
   * other servers.
   * @param originators Vector  // Vector&lt;String&gt;
   * @param tasks TaskObject[]
   */
  TaskObjectsAsynchParallelExecutionRequest(Vector originators, TaskObject[] tasks) {
		super(originators, tasks);
  }


  /**
   * finds a free worker and submits the tasks for processing (in chunks if 
	 * needed), then sends <CODE>OKReply</CODE> back to the (client) requestor. In 
	 * case no worker is available, sends back the tasks, wrapped in a 
	 * <CODE>NoWorkerAvailableResponse</CODE> object.
	 * Notice that in this implementation, state is kept of those tasks that have
	 * been successfully submitted, so that in case of some workers failing,
	 * when this object's <CODE>runProtocol()</CODE> method runs again, it will
	 * only submit those tasks that have not yet been successfully submitted.
	 * Though the submission loop is a serial for-loop, the tasks are still 
	 * sent in parallel, since each submission is an "asynchronous" submission in
	 * that there is no blocking for the tasks to complete before sending the 
	 * next chunk of results.
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
      throw new PDAsynchBatchTaskExecutorException(
				"TaskObjectsAsynchParallelExecutionRequest.runProtocol(): null or empty _tasks?");
  	if (_taskChunksList==null) {
			int num_chunks = _tasks.length / _maxChunkSize;
			if (_maxChunkSize*num_chunks < _tasks.length) ++num_chunks;
			_taskChunksList = new UnboundedBufferArrayUnsynchronized(num_chunks);
			// create and put the chunks of up to _maxChunkSize tasks in TaskObject[] 
			// objects in the list
			int cur_batch_length = _tasks.length > _maxChunkSize ? _maxChunkSize : _tasks.length;
			TaskObject[] cur_batch = new TaskObject[cur_batch_length];
			int j=0;
			for (int i=0; i<_tasks.length; i++) {
				cur_batch[j++] = _tasks[i];
				if (j==cur_batch.length) {  // current batch full, store it in buffer, and proceed
					_taskChunksList.addElement(cur_batch);
					int num_rem_tasks = _tasks.length - (i+1);
					cur_batch_length = num_rem_tasks > _maxChunkSize ? _maxChunkSize : num_rem_tasks;
					cur_batch = new TaskObject[cur_batch_length];
					j=0;
				}
			}
		}
		for (int i=0; i<_taskChunksList.size(); i++) {
			// 0. the tasks to send are always at the beginning
			TaskObject[] tasks = (TaskObject[]) _taskChunksList.elementAt(0);
			try {
				// 1. find an available worker on the net, submit work
				srv.submitWork(_originatingClients, tasks);
				// 2. remove this successfully sent chunk from list
				_taskChunksList.remove();
			}
			catch (IOException e) {  // worker disconnected, try one last time
				utils.Messenger.getInstance().msg("TOAPER.runProtocol(): connection to Wrk was lost, will try one last time", 2);
				// 3. try again
				srv.submitWork(_originatingClients, tasks);
				// 4. remove this successfully sent chunk from list
				_taskChunksList.remove();
			}
		}
		// finally, send back OKReply to requestor
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
 * auxiliary class wrapping a request so that 
 * <CODE>PDAsynchBatchTaskExecutorClt</CODE> client objects may await server's 
 * workers. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class PDAsynchBatchTaskExecutorSrvAwaitWrksRequest extends TaskObjectsAsynchExecutionRequest {
		
  /**
   * sole public constructor.
   */
  public PDAsynchBatchTaskExecutorSrvAwaitWrksRequest() {
		super();
  }


  /**
	 * awaits until there is at least one worker available to the server, and
	 * then sends OKReply back to the client.
   * @param srv PDAsynchBatchTaskExecutorSrv
   * @param ois ObjectInputStream
   * @param oos ObjectOutputStream
	 * @throws IOException
   */
  public void runProtocol(PDAsynchBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) 
		throws IOException {
		srv.awaitWorkers();
		oos.writeObject(new OKReply());
		oos.flush();
  }
}


/**
 * auxiliary class wrapping a request (from 
 * <CODE>PDAsynchBatchTaskExecutorSrv</CODE> objects) asking for worker 
 * availability. Not part of the public API.
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
 * auxiliary class wrapping a request (from 
 * <CODE>PDAsynchBatchTaskExecutorSrv</CODE> objects) asking for worker capacity 
 * (in terms of their msg-passing queue) to accept request for asynch task 
 * execution without throwing <CODE>ParallelException</CODE>. Not part of the 
 * public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class PDAsynchBatchTaskExecutorWrkCapacityRequest extends RRObject {
  //private final static long serialVersionUID = 5801899648236803371L;
	private int _size;


  /**
   * sole public constructor.
	 * @param size int the request batch size
   */
  public PDAsynchBatchTaskExecutorWrkCapacityRequest(int size) {
		_size = size;
  }
	
	
	/**
	 * get the requested batch size.
	 * @return int
	 */
	public int getSize() {
		return _size;
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
 * auxiliary class wrapping a request for processing in parallel TaskObjects, 
 * used with <CODE>PDBatchTaskExecutor[Srv|Wrk|Clt]</CODE> networks of 
 * synchronous execution requests. Not part of the public API.
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
	private utils.Messenger _mger = utils.Messenger.getInstance();

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
		_mger.msg("TaskObjectsParallelExecutionRequest.runProtocol(): created "+
			                                batches.size()+" batches to be ran by "+num_threads+" threads", 2);
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

	
	/**
	 * auxiliary inner-class for submitting in parallel chunks of a TaskObject[]
	 * to different workers connected to the same 
	 * <CODE>PDBatchTaskExecutorSrv</CODE> server. Not part of the public API.
	 * <p>Title: popt4jlib</p>
	 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
	 * <p>Copyright: Copyright (c) 2011-2016</p>
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
			_mger.msg("WrkSubmissionThread(srv,tasks,clts): creating thread for "+tasks.length+" tasks",2);
		}


		public void run() {
			try {
				// find an available worker on the net, submit work, wait for results
				_results = _srv.submitWork(_originatingClients, _tasks);
			}
			catch (IOException e) {  // worker closed connection, try one more time
				_mger.msg("WrkSubmissionThread.run(): current "+
					"worker closed connection, will retry one more time", 2);
				try {
					_results = _srv.submitWork(_originatingClients, _tasks);
				}
				catch (Exception e2) {
					// e2.printStackTrace();
					_mger.msg("WrkSubmissionThread.run(): _srv.submitWork(_originatingClients,"+
							  		_tasks.length+" tasks) threw exception '"+e+
										"'. Exiting with _results set to null",2); 
					_results = null;
				}
			}
			catch (Exception e) {
				// e.printStackTrace();
				_mger.msg("WrkSubmissionThread.run(): _srv.submitWork(_originatingClients,"+
									_tasks.length+" tasks) threw exception '"+e+
									"'. Exiting with _results set to null",2);
				_results = null;
			}
		}


		Object[] getResults() {
			if (_results!=null) return _results._results;
			else return null;
		}

	}

}


/**
 * auxiliary class wrapping the results of processing a TaskObject[]. Not part
 * of the public API.
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
    //return;
  }
}

