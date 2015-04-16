/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.NumberPartitioning;

import parallel.ParallelException;

/**
 * auxiliary class using the <CODE>ThreadLocal</CODE> mechanism to create
 * thread-local <CODE>ReleaseFNPTaskObject</CODE> object pools. Essentially, the
 * three classes <CODE>ReleaseFNPTaskObject, ReleaseFNPTaskObjectPool,
 * ReleaseFNPTaskObjectThreadLocalPools</CODE>
 * implement a pattern that this author termed "The Thread-Local Object-Pool
 * Design Pattern" that can result in dramatic speedups in run-time especially
 * when an application would need to create big numbers of 
 * <CODE>ReleaseFNPTaskObject</CODE> objects simultaneously from many 
 * concurrent threads.
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class ReleaseFNPTaskObjectThreadLocalPools {
  private static boolean _poolSizeResetAllowed = true;
	
	private static ThreadLocal _pools = new ThreadLocal() {
    protected Object initialValue() {
      return null;
    }
  };

	
  /**
   * returns the thread-local <CODE>ReleaseFNPTaskObjectPool</CODE> object 
	 * associated with the current thread invoking this method. Used only in the
   * <CODE>ReleaseFPTaskObjectPool.getObject(task,threadid)</CODE> method.
   * @return ReleaseFNPTaskObjectPool
   */
  static ReleaseFNPTaskObjectPool getThreadLocalPool() {
    ReleaseFNPTaskObjectPool p = (ReleaseFNPTaskObjectPool) _pools.get();
    if (p==null) {
			synchronized (ReleaseFNPTaskObjectThreadLocalPools.class) {
				_poolSizeResetAllowed=false;
			}
      p = new ReleaseFNPTaskObjectPool();
			p.initialize();
      _pools.set(p);
    }
    return p;
  }
	
	
	/**
	 * sets the size of the thread-local pools. Must only be called once, before
	 * any other call to methods of this class or any other of the 
	 * ReleaseFNPTaskObject* classes.
	 * @param poolsize int
	 * @throws ParallelException if there has been a call to any of the other 
	 * methods of the family of ReleaseFNPTaskObject* classes.
	 * @throws IllegalArgumentException if poolsize &lte; 0.
	 */
	public static synchronized void setPoolSize(int poolsize) 
	  throws ParallelException, IllegalArgumentException {
		if (!_poolSizeResetAllowed) 
			throw new ParallelException(
				"popt4jlib.NumberPartitioning.ReleaseFNPTaskObjectThreadLocalPools.setPoolSize(): "+
				"getThreadLocalPool() or setPoolSize has already been called from some thread");
		_poolSizeResetAllowed = false;
		ReleaseFNPTaskObjectPool.setPoolSize(poolsize);
	}
	
}

