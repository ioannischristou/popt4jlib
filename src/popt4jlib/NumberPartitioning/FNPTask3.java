package popt4jlib.NumberPartitioning;

import popt4jlib.IdentifiableIntf;
import parallel.ThreadSpecificTaskObject;
import parallel.DynamicAsynchTaskExecutor3;
import parallel.ParallelException;
import java.io.Serializable;


/**
 * class implements the CKK algorithm (Complete Karmarkar-Karp Algorithm) for
 * 2-way Number Partitioning, due to Richard Korf (1997). This class is almost
 * the same as <CODE>FNPTask</CODE> except that it implements the 
 * Thread-Local Object-Pool design pattern as a simple array for each thread 
 * acting as a stack, again without any need for synchronization; does not need
 * any <CODE>ReleaseFNPTaskObject</CODE>, and also uses the 
 * <CODE>DynamicAsynchTaskExecutor3</CODE> class for dispatching and executing
 * (non-comparable) tasks.
 * <p>Notes:
 * <ul>
 * <li>2021-10-09: made non-serializable pool and related data transient to 
 * support possible future "transfer through the wire".
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FNPTask3 implements ThreadSpecificTaskObject {
  // private final static long serialVersionUID=123456789L;
	// object data
  private long[] _descNumbers;
  private long _numbersSum;
  private long _numAdds;
  private long _numSubs;
  private long _myid;
	private int _sz;  // the true size of the array _descNumbers
	
	// itc-20211009: _originalthreadid used to be init. to zero
	private transient int _originalthreadid = Integer.MAX_VALUE;  // the thread 
	                                                              // from whose 
	                                                              // pool it is 
                                                                // "borrowed"
	private transient int _threadid = Integer.MAX_VALUE;  // indicate it can run 
	                                                      // anywhere

	private final static boolean _DO_RELEASE_SANITY_CHECK=false;  // compile-time 
	                                                              // constant
  private static DynamicAsynchTaskExecutor3 _executor=null;  // main() sets once
	private static int _numThreads=0;  // set once from main()
  private static int _maxDepthAllowed = 0;  // only set at most once from main()
  private static int _minArrLenAllowed2Fork = Integer.MAX_VALUE;  // only set at 
	                                                                // most once 
	                                                                // from main()
  private static long _numNodesDone = 0;  // indicates num nodes executed, that 
	                                        // were submitted to executor
  private static long _id = 0;  // object id
	private static final boolean _DO_COLLECT_STATS = false;  // compile-time const
	private static long _numOpenNodes = 0;  // num current open nodes (total)
	private static long _maxOpenNodes = 0;  // max num open nodes (total) ever
	private static long _numTotalNodes = 0;  // num nodes created (total) 
                                           // -- NOT the same as below as it 
	                                         // counts # of times setData(...) 
	                                         // is called too
	private static long _totalNumObjs = 0;  // total num FNPTask3 objects created
	private static long _totalNumLocks4Release = 0;  // total number of locks  
	// (on the global shared queue) caused by re-submiting an FNPTask3 object to
	// release it.
	private final static boolean _do_runAux2 = true;  // compile-time constant
	private static long _incumbent = Long.MAX_VALUE;  // global current incumbent
  private static ThreadLocal _localIncs = new ThreadLocal() {  // for 
		                                                           // optimization 
		                                                           // purposes only
    protected Object initialValue() {
      return new Double(Double.MAX_VALUE);
    }
  };

	// pool related data
	private transient FNPTask3Pool _pool=null;
	private transient boolean _isUsed = true;  // itc-20211009: used to be false 

	
  public FNPTask3(long id, long[] desc_numbers, long array_sum, 
		              long subtractions, long additions) {
    _myid = id;
    _descNumbers = desc_numbers;
		_sz = _descNumbers.length;
    _numbersSum = array_sum;
    _numAdds = additions;
    _numSubs = subtractions;
    if (_DO_COLLECT_STATS) {
			synchronized (FNPTask3.class) {
				++_totalNumObjs;
			}
			incrNumTotalNodes();
			incrNumOpenNodes();
		}
		_originalthreadid = Integer.MAX_VALUE;
  }
	
	
	FNPTask3(int sz) {
		_descNumbers = new long[sz];
    if (_DO_COLLECT_STATS) {
			synchronized (FNPTask3.class) {
				++_totalNumObjs;
			}
			incrNumTotalNodes();
			incrNumOpenNodes();
		}
		_originalthreadid = (int)((IdentifiableIntf)Thread.currentThread()).getId();
	}

	
	FNPTask3(FNPTask3Pool pool, int sz) {
		this(sz);
		_pool = pool;
	}

	
	/**
	 * returns the <CODE>_myid</CODE> data member, which is unique among all tasks
	 * that are actually submitted to the executor but not among all FNPTask3 
	 * objects.
	 * @return long
	 */
	public long getId() { 
		return _myid; 
	}
	
	
	/**
	 * return the id of the thread on which this object should execute. See 
	 * specification details in interface 
	 * <CODE>parallel.ThreadSpecificTaskObject</CODE>.
	 * @return int <CODE>_threadid</CODE>
	 */
	public int getThreadIdToRunOn() {
		return _threadid;
	}

	
	/**
	 * this is the method that the executor executes (implementing the 
	 * <CODE>run()</CODE> method of the <CODE>TaskObject</CODE> 
	 * interface that the class implements). It usually creates two
	 * more <CODE>FNPTask3</CODE> objects, one corresponding to the decision to 
	 * keep the two top numbers together, and the other to keep them apart. Then,
	 * the two new tasks have to be executed as well, recursively (but may well
	 * be submitted to the same executor, one of whose threads is currently 
	 * executing this task.)
	 */
  public void run() {
		final int cur_thread_id = _executor.getCurrentThreadId();
    if (!_do_runAux2 && _threadid!=Integer.MAX_VALUE) {  
      // only task is to release me
			if (cur_thread_id!=_threadid) {  // sanity check
				System.err.println("cur_thread_id="+cur_thread_id+
					                 " _threadid="+_threadid);
				System.exit(-1);
			}
			release();
			return;
		}
		if (foundOptimalSolution()) {
			incrNumNodesDone();
			if (_DO_COLLECT_STATS) {
				decrNumOpenNodes();
			}
			// don't bother releasing this object
			return;
		}  // quick short-cut
    Serializable res = _do_runAux2 ? runAux2(0) : runAux(0);
    incrNumNodesDone();
		if (_DO_COLLECT_STATS) {
			decrNumOpenNodes();
		}
		if (isManaged()) {  // next code is for releasing this task back to the pool
			// notice that usually, it will take a lot of time (running "runAux(0);")
			// before this object is released back into its pool. The exact time 
			// depends on the _maxDepthAllowed and _minArrLenAllowed2Fork values.
			if (_originalthreadid==cur_thread_id) {  // short-cut?
				release();
				return;
			} else {  // nope, do the work
				if (_do_runAux2) 
					throw new Error("shouldn't happen");  // valid only with new runAux2()
				                                        // not runAux()
				// now resubmit to executor for release:
				// this incurs two locks, just so as to release this object! (one lock
				// by this thread to submit to the global shared queue, and one when
				// this object is finally received from the right thread.)
				_threadid = _originalthreadid;
				try {
					if (_DO_COLLECT_STATS) {
						synchronized(FNPTask3.class) {
							_totalNumLocks4Release += 2;
						}
					}
					_executor.execute(this);  
				}
				catch (ParallelException e) {
					e.printStackTrace();
					System.exit(-1);
				}
				return;  // return null;
			}
		}
  }

	
  private Serializable runAux(int depth) {
		try {
	    // 1. check for pruning
		  long val = _descNumbers[0] - (_numbersSum - _descNumbers[0]);
			if (val>=0) {
				// prune by largest element being large enough
	      double local_inc = ((Double) _localIncs.get()).doubleValue();
		    if (val<local_inc) {  // call -synchronized- updateIncumbent() only if 
					                    // it's worth it
			    updateIncumbent(val);
				}
	      return new Long(val);
		  }
			if (_sz==1) {
	      double local_inc = ((Double) _localIncs.get()).doubleValue();
		    if (_descNumbers[0]<local_inc) {  // call -synchronized- 
					                                // updateIncumbent() only if it's 
					                                // worth it
			    updateIncumbent(_descNumbers[0]);
				}
	      return new Long(_descNumbers[0]);
		  }
			long diff = _descNumbers[0] - _descNumbers[1];
	    if (_sz==2) {
		    double local_inc = ((Double) _localIncs.get()).doubleValue();
			  if (diff<local_inc) {  // call -synchronized- updateIncumbent() only if 
					                     // it's worth it
				  updateIncumbent(diff);
				}	
	      return new Long(diff);
		  }
	    // last attempt at fathoming: normally we want the _incumbent, but
	    // the cost of getting a synchronized access is probably not worth it.
	    if (_numbersSum % 2 == 1 && ((Double) _localIncs.get()).doubleValue()==1)
		    return null;  // no reason to return anything here.

	    // do the branching
		  long sum = _descNumbers[0] + _descNumbers[1];

	    FNPTask3 left_node = newInstance(_descNumbers.length, _sz-1);
			long[] left_branch = left_node._descNumbers;
			boolean keep_checking = true;
			for (int i=0; i<_sz-1; i++) {
				if (i==left_node._sz-1) {  // i==left_branch.length-1
					if (keep_checking) left_branch[i] = diff;
	        else 
						left_branch[i] = _descNumbers[left_node._sz];  // left_branch.length
		      break;
			  }
				if (_descNumbers[i+2]<=diff && keep_checking) {
					left_branch[i] = diff;
	        keep_checking = false;
		    } else if (keep_checking) left_branch[i] = _descNumbers[i+2];
			  else left_branch[i] = _descNumbers[i+1];
	    }
			long left_sum = _numbersSum - _descNumbers[1] - _descNumbers[1];
		  FNPTask3 right_node = newInstance(_descNumbers.length, _sz-1);
			long[] right_branch = right_node._descNumbers;
			right_branch[0] = sum;
		  for (int i=1;i<right_node._sz; i++)  // right_branch.length
			  right_branch[i] = _descNumbers[i+1];
			
	    if (depth >= _maxDepthAllowed && 
				  _sz >= _minArrLenAllowed2Fork) { // ok, submit to executor
				left_node.setData(incrId(), left_sum, _numSubs+1, _numAdds);
				try {
					if (_myid>10*_numThreads)
						_executor.resubmitToSameThread(left_node);  // mostly send nodes to 
					                                              // cold-local queue
					else _executor.execute(left_node);
				}
				catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
				right_node.setData(incrId(), _numbersSum, _numSubs, _numAdds+1);
				try {
					if (_myid>10*_numThreads) 
						_executor.resubmitToSameThread(right_node);  // mostly send nodes to 
					                                               // cold-local queue
					else _executor.execute(right_node);
				}
				catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
			else {  // run children in same thread, now
				// the first condition in the check below is needed to avoid slow-down  
				// of multi-threaded CPU utilization
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
				left_node.setData(_myid+1, left_sum, _numSubs+1, _numAdds);
				right_node.setData(_myid+2, _numbersSum, _numSubs, _numAdds+1);
				left_node.runAux(depth+1);
				left_node.release();
				if (_DO_COLLECT_STATS) decrNumOpenNodes();
				right_node.runAux(depth+1);
				right_node.release();
				if (_DO_COLLECT_STATS) decrNumOpenNodes();
			}
			return null; // return this;
		}  // overall try
		finally {
			// no-op
		}
  }

	
  private Serializable runAux2(int depth) {
		try {
	    // 1. check for pruning
		  long val = _descNumbers[0] - (_numbersSum - _descNumbers[0]);
			if (val>=0) {
				// prune by largest element being large enough
	      double local_inc = ((Double) _localIncs.get()).doubleValue();
		    if (val<local_inc) {  // call -synchronized- updateIncumbent() only if 
					                    // it's worth it
			    updateIncumbent(val);
				}
	      return new Long(val);
		  }
			if (_sz==1) {
	      double local_inc = ((Double) _localIncs.get()).doubleValue();
		    if (_descNumbers[0]<local_inc) {  // call -synchronized- 
					                                // updateIncumbent() only if it's 
					                                // worth it
			    updateIncumbent(_descNumbers[0]);
				}
	      return new Long(_descNumbers[0]);
		  }
			long diff = _descNumbers[0] - _descNumbers[1];
	    if (_sz==2) {
		    double local_inc = ((Double) _localIncs.get()).doubleValue();
			  if (diff<local_inc) {  // call -synchronized- updateIncumbent() only if 
					                     // it's worth it
				  updateIncumbent(diff);
				}	
	      return new Long(diff);
		  }
	    // last attempt at fathoming: normally we want the _incumbent, but
	    // the cost of getting a synchronized access is probably not worth it.
	    if (_numbersSum % 2 == 1 && ((Double) _localIncs.get()).doubleValue()==1)
		    return null;  // no reason to return anything here.

	    // do the branching
		  long sum = _descNumbers[0] + _descNumbers[1];

			final boolean exec_cond_numthreads = _myid > 10*_numThreads;
			final boolean exec_cond_basic = depth >= _maxDepthAllowed && 
										                  _sz >= _minArrLenAllowed2Fork;
	    FNPTask3 left_node = null;
			// decide where to get the node from? pool or create new un-managed one?
			if (exec_cond_basic && !exec_cond_numthreads) {
				left_node = new FNPTask3(_descNumbers.length);
				left_node._sz = _sz-1;
			} else left_node = newInstance(_descNumbers.length, _sz-1);  // try pool
			long[] left_branch = left_node._descNumbers;
			boolean keep_checking = true;
			for (int i=0; i<_sz-1; i++) {
				if (i==left_node._sz-1) {  // i==left_branch.length-1
					if (keep_checking) left_branch[i] = diff;
	        else 
						left_branch[i] = _descNumbers[left_node._sz];  // left_branch.length
		      break;
			  }
				if (_descNumbers[i+2]<=diff && keep_checking) {
					left_branch[i] = diff;
	        keep_checking = false;
		    } else if (keep_checking) left_branch[i] = _descNumbers[i+2];
			  else left_branch[i] = _descNumbers[i+1];
	    }
			long left_sum = _numbersSum - _descNumbers[1] - _descNumbers[1];
		  FNPTask3 right_node = null;
			// decide where to get the node from? pool or create new un-managed one?
			if (exec_cond_basic && !exec_cond_numthreads) {
				right_node = new FNPTask3(_descNumbers.length);
				right_node._sz = _sz-1;
			} else right_node = newInstance(_descNumbers.length, _sz-1);
			long[] right_branch = right_node._descNumbers;
			right_branch[0] = sum;
		  for (int i=1;i<right_node._sz; i++)  // right_branch.length
			  right_branch[i] = _descNumbers[i+1];
			
	    if (exec_cond_basic) { // ok, submit to executor
				left_node.setData(incrId(), left_sum, _numSubs+1, _numAdds);
				try {
					if (exec_cond_numthreads) {
						left_node._threadid = left_node._originalthreadid;
						_executor.submitToSameThread(left_node);  // mostly send nodes to 
						                                          // hot-local queue
					}
					else _executor.execute(left_node);
				}
				catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
				right_node.setData(incrId(), _numbersSum, _numSubs, _numAdds+1);
				try {
					if (exec_cond_numthreads) {
						right_node._threadid = right_node._originalthreadid;
						_executor.submitToSameThread(right_node);  // mostly send nodes to 
						                                           // hot-local queue
					}
					else _executor.execute(right_node);
				}
				catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
			else {  // run children in same thread, now
				// the first condition in the check below is needed to avoid slow-down  
				// of multi-threaded CPU utilization
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
				left_node.setData(_myid+1, left_sum, _numSubs+1, _numAdds);
				right_node.setData(_myid+2, _numbersSum, _numSubs, _numAdds+1);
				left_node.runAux2(depth+1);
				left_node.release();
				if (_DO_COLLECT_STATS) decrNumOpenNodes();
				right_node.runAux2(depth+1);
				right_node.release();
				if (_DO_COLLECT_STATS) decrNumOpenNodes();
			}
			return null; // return this;
		}  // overall try
		finally {
			// no-op
		}
  }
	
			
	int getSize() {
		return _descNumbers.length;
	}

	
  /**
   * indicate item is available for re-use by Object-Pool to which it belongs,
   * and resets its "major" data IFF it is a managed object. Otherwise, it's a
	 * no-op.
   */
  public void release() {
    if (_pool!=null) {
			if (_DO_RELEASE_SANITY_CHECK) {
				FNPTask3Pool p = FNPTask3ThreadLocalPools.getThreadLocalPool(getSize());
				if (p!=_pool) {
					String str = "Current-Thread-id="+
						           ((IdentifiableIntf) Thread.currentThread()).getId();
					str += 
						" _threadid="+_threadid+" _originalthreadid="+_originalthreadid;
					throw new Error("release() called from the wrong thread. Expl: "+str);
				}
			}
			if (_isUsed) {
				_isUsed=false;
				_pool.returnObjectToPool(this);
			}
			else {
				/*
				Integer y_null = null;
				System.err.println("FNPTask3.release(): this is managed "+
								           "but release() is called on it twice..."+
								           " raising NullPointerException"+y_null.intValue());
				*/
				throw new Error("FNPTask3.release(): this is managed but release() "+
					              "is called on it twice...");
			}
    }
  }
	/**
	 * true IFF this object belongs to some pool.
	 * @return true IFF this object belongs to some pool.
	 */
	boolean isManaged() {
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
	FNPTask3Pool getPool() {  // method exists for debugging purposes only
		return _pool;
	}
	
	
	/**
	 * this factory method shall first try to obtain an object from the thread-
	 * local object pool of FNPTask3 objects, and if it can't find one, it will 
	 * then produce an unmanaged one.
	 * The _descNumbers array will have length n, and the _sz member will be set
	 * to the 2nd arg.
	 * @param n int the _descNumbers array length
	 * @param sz int the upper bound of the array (usable size)
	 * @return FNPTask3
	 */
  public static FNPTask3 newInstance(int n, int sz) {
		FNPTask3 t = FNPTask3Pool.getObject(n);
		t._sz = sz;
		return t;
  }

	
  /**
   * invoke as:
   * <CODE>java -cp &lt;classpath&gt; popt4jlib.NumberPartitioning.FNPTask3 
	 *       &lt;#numbers&gt; &lt;maxnumbersize&gt; 
	 *       [numthreads(1)] [maxdepthallowed(0)] [seed(7)] 
	 *       [minarrlenallowed2fork(Integer.MAX_VALUE)] 
	 *       [FNPTask3poolsize(100000)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    final long start = System.currentTimeMillis();
    // register handle to show best soln if we stop the program via ctrl-c
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        System.err.println("best soln="+FNPTask3._incumbent+
					                 " Total nodes exec'd in executor="+FNPTask3._id+
					                 " _numNodesDone="+FNPTask3._numNodesDone);
				if (_DO_COLLECT_STATS) {
					System.err.println("Total FNPTask3 Objects Created="+
						                 FNPTask3._totalNumObjs+
						                 " Max Open Nodes="+FNPTask3._maxOpenNodes);
					System.err.println("Total Nodes Created "+
						                 "(FNPTask3s + #setData() calls)="+
						                 FNPTask3._numTotalNodes);
					System.err.println("Total locks required for releasing "+
						                 "FNPTask3 objects="+
						                 FNPTask3._totalNumLocks4Release);
				}
				System.err.println("Total time (msecs)="+
					                 (System.currentTimeMillis()-start));
        System.err.flush();
      }
    }
    );
		if (args.length<2) {
			System.err.println("usage: "+
							           "java -cp &lt;classpath&gt; "+
				                 "popt4jlib.NumberPartitioning.FNPTask3"+
							           " <#numbers> <maxnumbersize> "+
							           "[numthreads(1)] [maxdepthallowed(0)] [seed(7)]" +
							           " [minarrlenallowed2fork(Integer.MAX_VALUE)] "+
							           "[FNPTask3poolsize(100000)]");
			System.exit(-1);
		}
		int n = Integer.parseInt(args[0]);
    long m = Long.parseLong(args[1]);
    long seed = 7;
    int numthreads = 1;
    if (args.length>2) numthreads = Integer.parseInt(args[2]);
		_numThreads = numthreads;
    if (args.length>3) _maxDepthAllowed = Integer.parseInt(args[3]);
    if (args.length>4) seed = Long.parseLong(args[4]);
    utils.RndUtil.getInstance().setSeed(seed);
    if (args.length>5) _minArrLenAllowed2Fork = Integer.parseInt(args[5]);
		if (args.length>6) {
			try {
				int fnptps = Integer.parseInt(args[6]);
				FNPTask3ThreadLocalPools.setPoolSize(fnptps);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

    long[] numbers = new long[n];
    long sum = 0L;
    for (int i=0; i<n; i++) {
      numbers[i] = 
				Math.round(utils.RndUtil.getInstance().getRandom().nextDouble()*m);
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
			_executor = DynamicAsynchTaskExecutor3.
				            newDynamicAsynchTaskExecutor3(2, numthreads);
      FNPTask3 root = new FNPTask3(incrId(), numbers, sum, 0, 0);
      _executor.execute(root);
      while (!done()) {  // used to be !foundOptimalSolution() && !done()
        Thread.sleep(1000);
				synchronized(FNPTask3.class) {
					System.err.println("_id=" + _id + " _numNodesDone=" + _numNodesDone +
						                 " executor tasks=" + 
						                 _executor.getNumTasksInQueue());
					if (_DO_COLLECT_STATS) 
						System.err.println("locks requested 4 releases so far=" + 
							                 _totalNumLocks4Release);
				}
      }
      System.out.println("Optimal partition diff="+_incumbent);
			if (_DO_COLLECT_STATS) {
				System.out.println("Total Nodes Created="+FNPTask3._numTotalNodes+
								           " Max Open Nodes="+FNPTask3._maxOpenNodes+
													 " locks requested 4 releases="+
					                 _totalNumLocks4Release);      
			}
			//System.out.print("Finished. Shuting down executor...");
      //_executor.shutDownAndWait4Threads();
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
    }
    else if (value > _incumbent) {
      _localIncs.set(new Double(_incumbent));  // update incumbent known by 
			                                         // thread
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

	
	private void setData(long id, long array_sum, long subtractions, 
		                   long additions) {
    _myid = id;
    _numbersSum = array_sum;
    _numAdds = additions;
    _numSubs = subtractions;
		_threadid = Integer.MAX_VALUE;
		if (_DO_COLLECT_STATS) {
			incrNumTotalNodes();
			incrNumOpenNodes();
		}
	}

	
  private synchronized static boolean done() {
    return _id <= _numNodesDone;
  }

	
	// --- experimental (but actually working) commented out methods below
	
	
	/**
	 * experimental (but working) versions of run() and runAux() methods.
	 * Essentially, they differ from the "accepted" methods above only in the way 
	 * they handle the timing of the release of an FNPTask3 object.
	 */
	/*
  public void run() {
		final int cur_thread_id = _executor.getCurrentThreadId();
    if (_threadid!=Integer.MAX_VALUE) {  // only task is to release me
			if (cur_thread_id!=_threadid) {  // sanity check
				System.err.println("cur_thread_id="+cur_thread_id+" _threadid="+_threadid);
				System.exit(-1);
			}
			release();
			return;
		}
		if (foundOptimalSolution()) {
			incrNumNodesDone();
			if (_DO_COLLECT_STATS) {
				decrNumOpenNodes();
			}
			// don't bother releasing this object
			return;
		}  // quick short-cut
    Serializable res = runAux(0);
    incrNumNodesDone();
		if (_DO_COLLECT_STATS) {
			decrNumOpenNodes();
		}
  }
	*/
	
	
	/**
	 * experimental (but working) versions of run() and runAux() methods.
	 * @param depth
	 * @return 
	 */
	/*
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
				if (isManaged()) {  // next code is for releasing this task back to the pool
					if (_originalthreadid==_executor.getCurrentThreadId()) {  // short-cut?
						release();
					} else {  // nope, do the work
						// now resubmit to executor for release
						_threadid = _originalthreadid;
						try {
							_executor.execute(this);  // this incurs at least one lock, just so as to release this object!
						}
						catch (ParallelException e) {
							e.printStackTrace();
							System.exit(-1);
						}
					}
				}								
	      return new Long(val);
		  }
			if (_sz==1) {
	      double local_inc = ((Double) _localIncs.get()).doubleValue();
		    if (_descNumbers[0]<local_inc) {  // call -synchronized- updateIncumbent() only if it's worth it
			    updateIncumbent(_descNumbers[0]);
				}
				if (isManaged()) {  // next code is for releasing this task back to the pool
					if (_originalthreadid==_executor.getCurrentThreadId()) {  // short-cut?
						release();
					} else {  // nope, do the work
						// now resubmit to executor for release
						_threadid = _originalthreadid;
						try {
							_executor.execute(this);  // this incurs at least one lock, just so as to release this object!
						}
						catch (ParallelException e) {
							e.printStackTrace();
							System.exit(-1);
						}
					}
				}				
	      return new Long(_descNumbers[0]);
		  }
			long diff = _descNumbers[0] - _descNumbers[1];
	    if (_sz==2) {
		    double local_inc = ((Double) _localIncs.get()).doubleValue();
			  if (diff<local_inc) {  // call -synchronized- updateIncumbent() only if it's worth it
				  updateIncumbent(diff);
				}	
				if (isManaged()) {  // next code is for releasing this task back to the pool
					if (_originalthreadid==_executor.getCurrentThreadId()) {  // short-cut?
						release();
					} else {  // nope, do the work
						// now resubmit to executor for release
						_threadid = _originalthreadid;
						try {
							_executor.execute(this);  // this incurs at least one lock, just so as to release this object!
						}
						catch (ParallelException e) {
							e.printStackTrace();
							System.exit(-1);
						}
					}
				}				
	      return new Long(diff);
		  }
	    // last attempt at fathoming: normally we want the _incumbent, but
	    // the cost of getting a synchronized access is probably not worth it.
	    if (_numbersSum % 2 == 1 && ((Double) _localIncs.get()).doubleValue() == 1) {
				if (isManaged()) {  // next code is for releasing this task back to the pool
					if (_originalthreadid==_executor.getCurrentThreadId()) {  // short-cut?
						release();
					} else {  // nope, do the work
						// now resubmit to executor for release
						_threadid = _originalthreadid;
						try {
							_executor.execute(this);  // this incurs at least one lock, just so as to release this object!
						}
						catch (ParallelException e) {
							e.printStackTrace();
							System.exit(-1);
						}
					}
				}				
		    return null;  // no reason to return anything here.
			}

	    // do the branching
		  long sum = _descNumbers[0] + _descNumbers[1];

	    FNPTask3 left_node = newInstance(_descNumbers.length, _sz-1);
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
		  FNPTask3 right_node = newInstance(_descNumbers.length, _sz-1);
			long[] right_branch = right_node._descNumbers;
			right_branch[0] = sum;
		  for (int i=1;i<right_node._sz; i++)  // right_branch.length
			  right_branch[i] = _descNumbers[i+1];
			
	    if (depth >= _maxDepthAllowed && _sz >= _minArrLenAllowed2Fork) { // ok, submit to executor
				left_node.setData(incrId(), left_sum, _numSubs+1, _numAdds);
				try {
					if (_myid>10*_numThreads)
						_executor.resubmitToSameThread(left_node);  // mostly send nodes to cold-local queue
					else _executor.execute(left_node);
				}
				catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
				right_node.setData(incrId(), _numbersSum, _numSubs, _numAdds+1);
				try {
					if (_myid>10*_numThreads) 
						_executor.resubmitToSameThread(right_node);  // mostly send nodes to cold-local queue
					else _executor.execute(right_node);
				}
				catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
				
				if (isManaged()) {  // next code is for releasing this task back to the pool
					if (_originalthreadid==_executor.getCurrentThreadId()) {  // short-cut?
						release();
						return null;
					} else {  // nope, do the work
						// now resubmit to executor for release
						_threadid = _originalthreadid;
						try {
							_executor.execute(this);  // this incurs at least one lock, just so as to release this object!
						}
						catch (ParallelException e) {
							e.printStackTrace();
							System.exit(-1);
						}
						return null;
					}
				}
				
			}
			else {  // run children in same thread, now
				// the first condition in the check below is needed to avoid slow-down of 
				// multi-threaded CPU utilization
				if (_myid % 10 == 0 && foundOptimalSolution()) {
					//left_node.release();
					//right_node.release();
					if (_DO_COLLECT_STATS) {
						decrNumOpenNodes();
						decrNumOpenNodes();
					}
					return null;
				}
				// these nodes do not inrement the global _id.
				left_node.setData(_myid+1, left_sum, _numSubs+1, _numAdds);
				right_node.setData(_myid+2, _numbersSum, _numSubs, _numAdds+1);
				
				if (isManaged()) {  // next code is for releasing this task back to the pool
					if (_originalthreadid==_executor.getCurrentThreadId()) {  // short-cut?
						release();
					} else {  // nope, do the work
						// now resubmit to executor for release
						_threadid = _originalthreadid;
						try {
							_executor.execute(this);  // this incurs at least one lock, just so as to release this object!
						}
						catch (ParallelException e) {
							e.printStackTrace();
							System.exit(-1);
						}
					}
				}				
				
				left_node.runAux(depth+1);
				//left_node.release();
				if (_DO_COLLECT_STATS) decrNumOpenNodes();
				right_node.runAux(depth+1);
				//right_node.release();
				if (_DO_COLLECT_STATS) decrNumOpenNodes();
			}
			return null; // return this;
		}  // overall try
		finally {
			// no-op
		}
  }
	*/
}

