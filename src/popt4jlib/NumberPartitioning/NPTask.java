package popt4jlib.NumberPartitioning;

import parallel.TaskObject;
import parallel.ComparableTaskObject;
import parallel.FasterParallelAsynchBatchPriorityTaskExecutor;
import java.util.Vector;
import java.io.Serializable;
import parallel.SimplePriorityMsgPassingCoordinator;


/**
 * class implements the CKK algorithm (Complete Karmarkar-Karp Algorithm) for
 * 2-way Number Partitioning, due to Richard Korf (1997). The tree-build method
 * however is parallelized using a priority-ordering parallel task executor, so
 * as to implement a guarantee that one thread performs Depth-First Search
 * without any special programming tricks.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class NPTask implements ComparableTaskObject {
  // private final static long serialVersionUID=123456789L;
  private long[] _descNumbers;
  private long _numbersSum;
  private long _numAdds;
  private long _numSubs;
  private boolean _isDone = false;
  private long _myid;

  private static boolean _useExecuteBatch=true;
  private static FasterParallelAsynchBatchPriorityTaskExecutor _executor=null;  // set once from main()
  private static int _maxDepthAllowed = 0;  // only set at most once from main()
  private static int _minArrLenAllowed2Fork = Integer.MAX_VALUE;  // only set at most once from main()
  private static long _numNodesDone = 0;
  private static long _id = 0;
	/*
	private static long _numOpenNodes = 0;
	private static long _maxOpenNodes = Long.MIN_VALUE;
	private static long _numTotalNodes = 0;
  */
	private static long _incumbent = Long.MAX_VALUE;
  private static ThreadLocal _localIncs = new ThreadLocal() {  // for optimization purposes only
    protected Object initialValue() {
      return new Double(Double.MAX_VALUE);
    }
  };
  //private static Object _waitOn = new Object();


  public NPTask(long id, long[] desc_numbers, long array_sum, long subtractions, long additions) {
    _myid = id;
    _descNumbers = desc_numbers;
    _numbersSum = array_sum;
    _numAdds = additions;
    _numSubs = subtractions;
		//incrNumTotalNodes();
		//incrNumOpenNodes();
  }


  public Serializable run() {
    if (foundOptimalSolution()) {
			incrNumNodesDone();
			//decrNumOpenNodes();
			return this;
		}  // quick short-cut
    Serializable res = runAux(0);
    incrNumNodesDone();
		//decrNumOpenNodes();
    return res;
  }


  private Serializable runAux(int depth) {
    // 1. check for pruning
    long val = _descNumbers[0] - (_numbersSum - _descNumbers[0]);
    if (val>=0) {
      // prune by largest element being large enough
      double local_inc = ((Double) _localIncs.get()).doubleValue();
      if (val<local_inc) {  // call -synchronized- updateIncumbent() only if it's worth it
        updateIncumbent(val);
      }
      return new Long(val);
    }
    if (_descNumbers.length==1) {
      double local_inc = ((Double) _localIncs.get()).doubleValue();
      if (_descNumbers[0]<local_inc) {  // call -synchronized- updateIncumbent() only if it's worth it
        updateIncumbent(_descNumbers[0]);
      }
      return new Long(_descNumbers[0]);
    }
    long diff = _descNumbers[0] - _descNumbers[1];
    if (_descNumbers.length==2) {
      double local_inc = ((Double) _localIncs.get()).doubleValue();
      if (diff<local_inc) {  // call -synchronized- updateIncumbent() only if it's worth it
        updateIncumbent(diff);
      }
      return new Long(diff);
    }
    // last attempt at fathoming: normally we want the _incumbent, but
    // the cost of getting a synchronized access is probably not worth it.
    if (_numbersSum % 2 == 1 && ((Double) _localIncs.get()).doubleValue() == 1)
      return null;  // no reason to return anything here.

    // do the branching
    long sum = _descNumbers[0] + _descNumbers[1];

    long[] left_branch = new long[_descNumbers.length-1];
    boolean keep_checking = true;
    for (int i=0; i<_descNumbers.length-1; i++) {
      if (i==left_branch.length-1) {
        if (keep_checking) left_branch[i] = diff;
        else left_branch[i] = _descNumbers[left_branch.length];
        break;
      }
      if (_descNumbers[i+2]<=diff && keep_checking) {
        left_branch[i] = diff;
        keep_checking = false;
      } else if (keep_checking) left_branch[i] = _descNumbers[i+2];
      else left_branch[i] = _descNumbers[i+1];
    }
    //print(_id,"LEFT: ", left_branch);
    long left_sum = _numbersSum - _descNumbers[1] - _descNumbers[1];

    long[] right_branch = new long[_descNumbers.length-1];
    right_branch[0] = sum;
    for (int i=1;i<right_branch.length; i++)
      right_branch[i] = _descNumbers[i+1];
    //print(_id,"RIGHT: ", right_branch);

    if (depth >= _maxDepthAllowed && _descNumbers.length >= _minArrLenAllowed2Fork) { // ok, submit to executor
      NPTask left_node = new NPTask(incrId(), left_branch, left_sum, _numSubs+1, _numAdds);
      NPTask right_node = new NPTask(incrId(), right_branch, _numbersSum, _numSubs, _numAdds+1);
      if (_useExecuteBatch) {
        Vector nodes = new Vector();
        nodes.add(left_node);
        nodes.add(right_node);
        try {
          _executor.executeBatch(nodes);
        }
				catch (parallel.ParallelExceptionUnsubmittedTasks e2) {
					// executor was full and some tasks were not submitted
					nodes = e2.getUnsubmittedTasks();
					// execute each unsubmitted task serially on the current thread
					for (int i=0; i<nodes.size(); i++) {
						NPTask ni = (NPTask) nodes.get(i);
						ni.run(); 
					}
				}
        catch (Exception e) {
          e.printStackTrace();  // catch-all clause
        }
      } else {  // _useExecuteBatch==false
        try {
          if (!_executor.execute(left_node)) left_node.run();
        }
        catch (Exception e) {
          //e.printStackTrace();
          System.err.println("(thread-pool full): left node not executed...");
        }
        try {
          if (!_executor.execute(right_node)) right_node.run();
        }
        catch (Exception e) {
          //e.printStackTrace();
          System.err.println("(thread-pool full): right node not executed...");
        }
      }
    }
    else {  // run children in same thread, now
			// the first condition in the check below is needed to avoid slow-down of 
			// multi-threaded CPU utilization
			if (_myid % 10 == 0 && foundOptimalSolution()) return null;
      // these nodes do not inrement the global _id.
      NPTask left_node = new NPTask(_myid+1, left_branch, left_sum, _numSubs+1, _numAdds);
      NPTask right_node = new NPTask(_myid+2, right_branch, _numbersSum, _numSubs, _numAdds+1);
      left_node.runAux(depth+1);
			//decrNumOpenNodes();
      right_node.runAux(depth+1);
			//decrNumOpenNodes();
    }

    // if _isDone was used somehow, then the synchronization code would be needed
    //synchronized (this) {
    _isDone = true;
    //}
    return this;
  }


  /**
   * returns true only if the run() method has run to completion.
   * @return boolean
   */
  public synchronized boolean isDone() {
    return _isDone;
  }


  /**
   * throws exception (unsupported).
   * @param other TaskObject
   * @throws IllegalArgumentException
   */
  public void copyFrom(TaskObject other) throws IllegalArgumentException {
    throw new IllegalArgumentException("unsupported");
  }


  /**
   * method is defined in such a way so that the processing of NPTask objects
   * follows Depth-First-Search.
   * @param other Object
   * @return int
   */
  public int compareTo(Object other) {
    NPTask o = (NPTask) other;
    if (_numSubs > o._numSubs) return -1;
    else if (_numSubs == o._numSubs) {
      if (_numAdds > o._numAdds) return -1;
      else if (_numAdds == o._numAdds) {
        if (_myid<o._myid) return -1;
        else if (_myid>o._myid) return 1;
        else return 0;
      }
      else return 1;
    }
    else return 1;
  }


  /**
   * required to be compatible with compareTo() so that different NPTask objects
   * are not "lost" when inserted into a TreeSet.
   * @param other Object
   * @return boolean
   */
  public boolean equals(Object other) {
    if (other==null || other instanceof NPTask == false) return false;
    NPTask o = (NPTask) other;
    return _myid == o._myid && _numSubs == o._numSubs && _numAdds == o._numAdds;
  }


  /**
   * returns the _myid value.
   * @return int
   */
  public int hashCode() {
    return (int) _myid;
  }


  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; popt4jlib.NumberPartitioning.NPTask &lt;#numbers&gt; &lt;maxnumbersize&gt; [numthreads(1)] [maxdepthallowed(0)] [seed(7)] [useExecuteBatch(1)] [minarrlenallowed2fork(Integer.MAX_VALUE)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    final long start = System.currentTimeMillis();
    // register handle to show best soln if we stop the program via ctrl-c
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        System.err.println("best soln="+NPTask._incumbent+" Total nodes exec'd in executor="+NPTask._id+" _numNodesDone="+NPTask._numNodesDone);
				//System.err.println("Total Nodes Created="+NPTask._numTotalNodes+" Max Open Nodes="+NPTask._maxOpenNodes);
        System.err.println("Total time (msecs)="+(System.currentTimeMillis()-start));
        System.err.flush();
      }
    }
    );
    int n = Integer.parseInt(args[0]);
    long m = Long.parseLong(args[1]);
    long seed = 7;
    int numthreads = 1;
    if (args.length>2) numthreads = Integer.parseInt(args[2]);
    if (args.length>3) _maxDepthAllowed = Integer.parseInt(args[3]);
    if (args.length>4) seed = Long.parseLong(args[4]);
    utils.RndUtil.getInstance().setSeed(seed);
    int ueb = 1;
    if (args.length>5) ueb = Integer.parseInt(args[5]);
    if (ueb==0) _useExecuteBatch = false;
    if (args.length>6) _minArrLenAllowed2Fork = Integer.parseInt(args[6]);

    long[] numbers = new long[n];
    long sum = 0L;
    for (int i=0; i<n; i++) {
      numbers[i] = Math.round(utils.RndUtil.getInstance().getRandom().nextDouble()*m);
      sum += numbers[i];
    }
    java.util.Arrays.sort(numbers);
    int nhalf = n/2;
    for (int i=0; i<nhalf; i++) {
      long tmp = numbers[i];
      numbers[i] = numbers[n-1-i];
      numbers[n-1-i] = tmp;
    }
    // check
    for (int i=0; i<n; i++) System.err.print(numbers[i]+" ");
    System.err.println();
    try {
			int maxcoordqsz = ((int) Math.pow(2.0, _maxDepthAllowed+2)*numthreads);
			SimplePriorityMsgPassingCoordinator.setMaxSize(maxcoordqsz);
      _executor = FasterParallelAsynchBatchPriorityTaskExecutor.
										newFasterParallelAsynchBatchPriorityTaskExecutor(numthreads);
      NPTask root = new NPTask(incrId(), numbers, sum, 0, 0);
      _executor.execute(root);
      while (!done()) {  // used to be !foundOptimalSolution() && !done()
        Thread.sleep(1000);
        // code commented below is in fact worse than simple busy-waiting behavior
        /*
        synchronized (_waitOn) {
          try {
            _waitOn.wait();
          }
          catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
        */
				synchronized(NPTask.class) {
					System.err.println("_id=" + _id + " _numNodesDone=" + _numNodesDone +
						                 " executor tasks=" + _executor.getNumTasksInQueue());
				}
      }
      System.out.println("Optimal partition diff="+_incumbent);
			//System.out.println("Total Nodes Created="+NPTask._numTotalNodes+" Max Open Nodes="+NPTask._maxOpenNodes);      
			System.out.print("Finished. Shuting down executor...");
      _executor.shutDown();
      System.out.println("Done.");
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }


  private synchronized static void updateIncumbent(long value) {
    if (value < _incumbent) {
      _localIncs.set(new Double(value));  // update incumbent found by thread
      _incumbent = value;
      System.out.println("incumbent soln found="+value);
      /*
      synchronized (_waitOn) {
        _waitOn.notify();
      }
      */
    }
    else if (value > _incumbent) {
      _localIncs.set(new Double(_incumbent));  // update incumbent known by thread
    }
  }


  private synchronized static boolean foundOptimalSolution() {
    return _incumbent == 0;
  }


  private synchronized static long incrId() {
    return ++_id;
  }


  private synchronized static void incrNumNodesDone() {
    ++_numNodesDone;
    /*
    synchronized (_waitOn) {
      _waitOn.notify();
    }
    */
  }
	
	
	/*
	private synchronized static void incrNumTotalNodes() {
		++_numTotalNodes;
	}
	private synchronized static void incrNumOpenNodes() {
		if (++_numOpenNodes>_maxOpenNodes) {
			_maxOpenNodes = _numOpenNodes;
		}
	}
	private synchronized static void decrNumOpenNodes() {
		--_numOpenNodes;
	}
	*/
	
	
  private synchronized static boolean done() {
    return _id <= _numNodesDone;
  }


/*
  private synchronized static void print(long id, String str, long[] nums) {
    System.err.print("("+id+")"+str+"[");
    for (int i=0; i<nums.length; i++) {
      System.err.print(nums[i]+" ");
    }
    System.err.println("]");
  }
*/

}

