package graph.packing;

import parallel.*;

/**
 * wrapper for the <CODE>parallel.FasterParallelAsynchBatchTaskExecutor</CODE> 
 * class. It allows the creation of a unique thread-pool in the system (as a 
 * static <CODE>parallel.FasterParallelAsynchBatchTaskExecutor</CODE> object 
 * data member). Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class BBThreadPool {
  static private int _numThreads = 1;  // default
  static private FasterParallelAsynchBatchTaskExecutor _pool = null;


  /**
   * set the number of threads of the pool. Must be called only once, prior
   * to using the BBThreadPool.
   * @param n int
   */
  static void setNumthreads(int n) {
    _numThreads = n;
    try {
      _pool = FasterParallelAsynchBatchTaskExecutor.
							newFasterParallelAsynchBatchTaskExecutor(_numThreads, true);
    }
    catch (ParallelException e) {
      e.printStackTrace();  // cannot get here
    }
  }


  /**
   * return the number of threads of the unique thread pool.
   * @return int
   */
  static int getNumThreads() { return _numThreads; }


  /**
   * get the pool.
   * @return FasterParallelAsynchBatchTaskExecutor
   */
  static FasterParallelAsynchBatchTaskExecutor getPool() { return _pool; }

}

