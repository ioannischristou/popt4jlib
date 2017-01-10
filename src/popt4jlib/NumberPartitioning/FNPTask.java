package popt4jlib.NumberPartitioning;

import popt4jlib.PoolableObjectIntf;
import popt4jlib.IdentifiableIntf;
import parallel.TaskObject;
import parallel.ComparableTaskObject;
import parallel.FasterParallelAsynchBatchPriorityTaskExecutor;
import parallel.SimplePriorityMsgPassingCoordinator;
import java.util.Vector;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;


/**
 * class implements the CKK algorithm (Complete Karmarkar-Karp Algorithm) for
 * 2-way Number Partitioning, due to Richard Korf (1997). This class is almost
 * the same as <CODE>NPTask</CODE> except that it implements also the 
 * Thread-Local Object-Pool design pattern found in other places in this library
 * as well. Since the <CODE>run()</CODE> main method delegates to the 
 * <CODE>runAux(depth)</CODE> method, which in turn sometimes creates new 
 * <CODE>FNPTask</CODE> objects, and submits them to an asynchronous priority
 * batch task executor, to be able to reclaim such objects without any locking,
 * we resort to the creation of special <CODE>ReleaseFNPTaskObject</CODE> 
 * objects, that are also submitted to the same executor, that when executed,
 * will force the executor to delete the FNPTask object they encapsulate on the 
 * same thread that the object was created, which necessarily increases the 
 * complexity of the overall program. 
 * To see the difference between having the ThreadLocal Object Pool (TLOP) and 
 * not having it, run the program with the following two parameter sets:
 * 1. java ... 40 10000000000000 8 13 7 1 20 // this is with the TLOP
 * this runs in about 69 seconds on a T530 laptop.
 * 2. java ... 40 10000000000000 8 13 7 1 20 10 // force a pool size of size 10 
 * - essentially non-existent
 * this runs in about 217 seconds on the same laptop.
 *
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FNPTask implements ComparableTaskObject, PoolableObjectIntf {
  // private final static long serialVersionUID=123456789L;
	// object data
  private long[] _descNumbers;
  private long _numbersSum;
  private long _numAdds;
  private long _numSubs;
  private boolean _isDone = false;
  private long _myid;
	private int _sz;  // the true size of the array _descNumbers

  private static boolean _useExecuteBatch=true;  // indicates whether to allow batch task submission to executor
  private static FasterParallelAsynchBatchPriorityTaskExecutor _executor=null;  // set once from main()
  private static int _maxDepthAllowed = 0;  // only set at most once from main()
  private static int _minArrLenAllowed2Fork = Integer.MAX_VALUE;  // only set at most once from main()
  private static long _numNodesDone = 0;  // indicates num nodes executed, that were submitted to executor
  private static long _id = 0;  // object id
	static final boolean _DO_COLLECT_STATS = false;  // compile-time constant
	private static long _numOpenNodes = 0;  // num current open nodes (total)
	private static long _maxOpenNodes = 0;  // max num open nodes (total) ever
	private static long _numTotalNodes = 0;  // num nodes created (total) 
                                           // -- NOT the same as below as it 
	                                         // counts number of times setData(...) 
	                                         // is called too
	private static long _totalNumObjs = 0;  // total num FNPTask objects created
	private static long _incumbent = Long.MAX_VALUE;  // global current incumbent
  private static ThreadLocal _localIncs = new ThreadLocal() {  // for optimization purposes only
    protected Object initialValue() {
      return new Double(Double.MAX_VALUE);
    }
  };

	// pool-related data
	private final static boolean _USE_POOLS=true;  // compile-time flag indicates use of pools or not
  private final static boolean _DO_RESET_ON_RELEASE=false;  // compile-time flag for resetting array elems on reset
	private FNPTaskPool _pool=null;
  private int _poolPos=-1;
  private boolean _isUsed=false;  // redundant init.


  public FNPTask(long id, long[] desc_numbers, long array_sum, long subtractions, long additions) {
    _myid = id;
    _descNumbers = desc_numbers;
		_sz = _descNumbers.length;
    _numbersSum = array_sum;
    _numAdds = additions;
    _numSubs = subtractions;
    if (_DO_COLLECT_STATS) {
			synchronized (FNPTask.class) {
				++_totalNumObjs;
			}
			incrNumTotalNodes();
			incrNumOpenNodes();
		}
  }
	
	
  /**
   * this constructor is to be used only from the FNPTaskPool for
   * constructing managed objects (re-claimable ones).
	 * @param n int the FNPTask object's array of longs.
   * @param pool FNPTaskPool
   * @param poolpos int
   */
  FNPTask(int n, FNPTaskPool pool, int poolpos) {
    if (_USE_POOLS==false) throw new IllegalArgumentException("_USE_POOLS==false: this ctor should not be used");
		_pool=pool;
    _poolPos=poolpos;
    _isUsed=false;
		_descNumbers = new long[n];
    if (_DO_COLLECT_STATS) {
			synchronized (FNPTask.class) {
				++_totalNumObjs;
			}
			incrNumTotalNodes();
			incrNumOpenNodes();
		}
  }
	
	
	FNPTask(int sz) {
		_descNumbers = new long[sz];
    if (_DO_COLLECT_STATS) {
			synchronized (FNPTask.class) {
				++_totalNumObjs;
			}
			incrNumTotalNodes();
			incrNumOpenNodes();
		}
	}

	
	/**
	 * returns the <CODE>_myid</CODE> data member, which is unique among all tasks
	 * that are actually submitted to the executor but not among all FNPTask 
	 * objects.
	 * @return long
	 */
	public long getId() { 
		return _myid; 
	}

	
	/**
	 * this is the method that the executor executes (implementing the 
	 * <CODE>run()</CODE> method of the <CODE>ComparableTaskObject</CODE> 
	 * interface that the class implements). It usually creates two
	 * more <CODE>FNPTask</CODE> objects, one corresponding to the decision to 
	 * keep the two top numbers together, and the other to keep them apart. Then,
	 * the two new tasks have to be executed as well, recursively (but may well
	 * be submitted to the same executor, one of whose threads is currently 
	 * executing this task.)
	 * @return Serializable not used.
	 */
  public Serializable run() {
    if (foundOptimalSolution()) {
			incrNumNodesDone();
			if (_DO_COLLECT_STATS) {
				decrNumOpenNodes();
			}
			if (isManaged()) {  // only synchronize and declare done for managed objs
				synchronized (this) {
					_isDone = true;
					notifyAll();
				}
			}
			return this;
		}  // quick short-cut
    Serializable res = runAux(0);
    incrNumNodesDone();
		if (_DO_COLLECT_STATS) {
			decrNumOpenNodes();
		}
    return res;
  }


  private Serializable runAux(int depth) {
		try {
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
			if (_sz==1) {
	      double local_inc = ((Double) _localIncs.get()).doubleValue();
		    if (_descNumbers[0]<local_inc) {  // call -synchronized- updateIncumbent() only if it's worth it
			    updateIncumbent(_descNumbers[0]);
				}
	      return new Long(_descNumbers[0]);
		  }
			long diff = _descNumbers[0] - _descNumbers[1];
	    if (_sz==2) {
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

	    FNPTask left_node = newInstance(_descNumbers.length, _sz-1);
			long[] left_branch = left_node._descNumbers;
			boolean keep_checking = true;
			for (int i=0; i<_sz-1; i++) {
				if (i==left_node._sz-1) {  // i==left_branch.length-1
					if (keep_checking) left_branch[i] = diff;
	        else left_branch[i] = _descNumbers[left_node._sz];  // left_branch.length
		      break;
			  }
				if (_descNumbers[i+2]<=diff && keep_checking) {
					left_branch[i] = diff;
	        keep_checking = false;
		    } else if (keep_checking) left_branch[i] = _descNumbers[i+2];
			  else left_branch[i] = _descNumbers[i+1];
	    }
			long left_sum = _numbersSum - _descNumbers[1] - _descNumbers[1];

		  FNPTask right_node = newInstance(_descNumbers.length, _sz-1);
			long[] right_branch = right_node._descNumbers;
			right_branch[0] = sum;
		  for (int i=1;i<right_node._sz; i++)  // right_branch.length
			  right_branch[i] = _descNumbers[i+1];
			final int mythreadid = (int)((IdentifiableIntf)Thread.currentThread()).getId();
	    if (depth >= _maxDepthAllowed && _sz >= _minArrLenAllowed2Fork) { // ok, submit to executor
			  left_node.setData(incrId(), left_sum, _numSubs+1, _numAdds, true);
				right_node.setData(incrId(), _numbersSum, _numSubs, _numAdds+1, true);
		    if (_useExecuteBatch) {  // asynch execution
			    List nodes = new ArrayList();
					nodes.add(left_node);
					nodes.add(right_node);
					try {
						_executor.executeBatch(nodes);
					}
					catch (parallel.ParallelExceptionUnsubmittedTasks e) {
						// some tasks failed to be submitted
						nodes = e.getUnsubmittedTasks();
						// run the tasks in current thread
						for (int i=0; i<nodes.size(); i++) {
							FNPTask ni = (FNPTask) nodes.get(i);
							ni.run();  // serial execution
							ni.release();
						}
					}
					catch (Exception e2) {
						e2.printStackTrace();  // catch-all
					}
					try {
						if (left_node.isManaged() && left_node.isUsed()) 
							_executor.execute(ReleaseFNPTaskObject.newInstance(left_node, mythreadid));
						if (right_node.isManaged() && right_node.isUsed()) 
							_executor.execute(ReleaseFNPTaskObject.newInstance(right_node, mythreadid));
					}
					catch (parallel.ParallelException e) {
						e.printStackTrace();
					}
				} else {  // _useExecuteBatch==false
		      try {
			      if (!_executor.execute(left_node)) {
							left_node.run();  // serial execution
							left_node.release();
						} else {
							if (left_node.isManaged()) _executor.execute(ReleaseFNPTaskObject.newInstance(left_node, mythreadid));
						}
				  }
					catch (Exception e) {
						//e.printStackTrace();
						System.err.println("(thread-pool full): left node not executed...");
					}
					try {
						if (!_executor.execute(right_node)) {
							right_node.run();
							right_node.release();
						}
						else {
							if (right_node.isManaged()) _executor.execute(ReleaseFNPTaskObject.newInstance(right_node, mythreadid));							
						}
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
				if (_myid % 10 == 0 && foundOptimalSolution()) {
					left_node.release();
					right_node.release();
					if (_DO_COLLECT_STATS) {
						decrNumOpenNodes();
						decrNumOpenNodes();
					}
					return null;
				}
				// these nodes do not inrement the global _id.
				left_node.setData(_myid+1, left_sum, _numSubs+1, _numAdds, false);
				right_node.setData(_myid+2, _numbersSum, _numSubs, _numAdds+1, false);
				left_node.runAux(depth+1);
				left_node.release();
				if (_DO_COLLECT_STATS) decrNumOpenNodes();
				right_node.runAux(depth+1);
				right_node.release();
				if (_DO_COLLECT_STATS) decrNumOpenNodes();
			}
			return this;
		}  // overall try
		finally {
			if (depth==0 && isManaged()) {  // only synchronize for nodes that were submitted to the _executor
				synchronized (this) {
					_isDone = true;
					notifyAll();
				}
			}
		}
  }

	
	int getSize() {
		return _descNumbers.length;
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
   * method is defined in such a way so that the processing of FNPTask objects
   * follows Depth-First-Search.
   * @param other Object
   * @return int
   */
  public int compareTo(Object other) {
		if (other==null) throw new NullPointerException("null arg. passed in");
		if (other instanceof FNPTask == false) return -1;  // FNPTask objects have higher priority
		FNPTask o = (FNPTask) other;
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
   * required to be compatible with compareTo() so that different FNPTask objects
   * are not "lost" when inserted into a TreeSet.
   * @param other Object
   * @return boolean
   */
  public boolean equals(Object other) {
    if (other==null || other instanceof FNPTask == false) return false;
    FNPTask o = (FNPTask) other;
    return _myid == o._myid && _numSubs == o._numSubs && _numAdds == o._numAdds;
  }


  /**
   * returns the _myid value.
   * @return int
   */
  public int hashCode() {
    return (int) _myid;
  }

	// PoolableObjectIntf methods below

	/**
	 * returns an unmanaged (ie not belonging to the thread-local object pool)
	 * FNPTask object that is a cloned (deep) copy of the data in this
	 * object.
	 * Note: this implementation does not support this operation.
	 * @return null
	 */
	public PoolableObjectIntf cloneObject() {
		return null;
	}
  /**
   * indicate item is available for re-use by Object-Pool to which it belongs,
   * and resets its "major" data IFF it is a managed object. Otherwise, it's a
	 * no-op.
   */
  public void release() {
    if (_pool!=null) {
      _isUsed=false;
			if (_DO_RESET_ON_RELEASE) {
				for (int i=0;i<_descNumbers.length; i++) _descNumbers[i] = 0L;  // reset
			}
			_isDone = false;
			_pool.returnObjectToPool(this);
    }
  }
	/**
	 * true IFF this object belongs to some pool.
	 * @return true IFF this object belongs to some pool.
	 */
	public boolean isManaged() {
		return _pool!=null;
	}
  /**
   * return true IFF the object is managed and "currently used", or un-managed.
   * @return boolean
   */
  boolean isUsed() {
    return _isUsed;
  }
  static synchronized long getTotalNumObjs() {
    return _totalNumObjs;
  }
  void setIsUsed() {
    _isUsed=true;
  }
  int getPoolPos() {
    return _poolPos;
  }
	FNPTaskPool getPool() {  // method exists for debugging purposes only
		return _pool;
	}
	
	
	/**
	 * this factory method shall first try to obtain an object from the thread-
	 * local object pool of FNPTask objects (as long as the _USE_POOLS 
	 * compile-time constant flag is true), and if it can't find one (or if the
	 * _USE_POOLS flag is false), it will then produce an unmanaged one.
	 * The _descNumbers array will have length n, and the _sz member will be set
	 * to the 2nd arg.
	 * @param n int the _descNumbers array length
	 * @param sz int the upper bound of the array (usable size)
	 * @return FNPTask
	 */
  public static FNPTask newInstance(int n, int sz) {
		if (_USE_POOLS) {
			FNPTask t = FNPTaskPool.getObject(n);
			t._sz = sz;
			return t;
		}
		else {
			FNPTask t = new FNPTask(n);
			t._sz = sz;
			return t;
		}
  }

	
  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; popt4jlib.NumberPartitioning.FNPTask &lt;#numbers&gt; &lt;maxnumbersize&gt; [numthreads(1)] [maxdepthallowed(0)] [seed(7)] [useExecuteBatch(1)] [minarrlenallowed2fork(Integer.MAX_VALUE)] [FNPTaskpoolsize(100000)] [ReleaseFNPTaskObjectpoolsize(100000)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    final long start = System.currentTimeMillis();
    // register handle to show best soln if we stop the program via ctrl-c
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        System.err.println("best soln="+FNPTask._incumbent+" Total nodes exec'd in executor="+FNPTask._id+" _numNodesDone="+FNPTask._numNodesDone);
				if (_DO_COLLECT_STATS) {
					System.err.println("Total FNPTask Objects Created="+FNPTask._totalNumObjs+" Max Open Nodes="+FNPTask._maxOpenNodes);
					System.err.println("Total Nodes Created (FNPTasks + #setData() calls)="+FNPTask._numTotalNodes);
					System.err.println("Total ReleaseFNPTaskObject Objects Created="+ReleaseFNPTaskObject.getTotalNumObjs());
				}
				System.err.println("Total time (msecs)="+(System.currentTimeMillis()-start));
        System.err.flush();
      }
    }
    );
		if (args.length<2) {
			System.err.println("usage: "+
							"java -cp &lt;classpath&gt; popt4jlib.NumberPartitioning.FNPTask"+
							" <#numbers> <maxnumbersize> "+
							"[numthreads(1)] [maxdepthallowed(0)] [seed(7)] [useExecuteBatch(1)]"+
							" [minarrlenallowed2fork(Integer.MAX_VALUE)] "+
							"[FNPTaskpoolsize(100000)] [ReleaseFNPTaskObjectpoolsize(100000)]");
			System.exit(-1);
		}
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
		if (args.length>7) {
			try {
				int fnptps = Integer.parseInt(args[7]);
				FNPTaskThreadLocalPools.setPoolSize(fnptps);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		if (args.length>8) {
			try {
			int rfnptops = Integer.parseInt(args[8]);
			ReleaseFNPTaskObjectThreadLocalPools.setPoolSize(rfnptops);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

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
      FNPTask root = new FNPTask(incrId(), numbers, sum, 0, 0);
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
				synchronized(FNPTask.class) {
					System.err.println("_id=" + _id + " _numNodesDone=" + _numNodesDone +
						                 " executor tasks=" + _executor.getNumTasksInQueue());
				}
      }
      System.out.println("Optimal partition diff="+_incumbent);
			if (_DO_COLLECT_STATS)
				System.out.println("Total Nodes Created="+FNPTask._numTotalNodes+" Max Open Nodes="+FNPTask._maxOpenNodes);      
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

	
	private void setData(long id, long array_sum, long subtractions, long additions, boolean do_synch) {
    _myid = id;
    _numbersSum = array_sum;
    _numAdds = additions;
    _numSubs = subtractions;
		if (_DO_COLLECT_STATS) {
			incrNumTotalNodes();
			incrNumOpenNodes();
		}
		if (do_synch) {
			synchronized (this) {
				_isDone = false;  // reset field for next use
			}
		}
	}

	
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

