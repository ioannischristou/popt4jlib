/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.NumberPartitioning;

import parallel.ParallelException;

/**
 * auxiliary class using the <CODE>ThreadLocal</CODE> mechanism to create
 * thread-local FNPTask3 object pools. Essentially, the three classes
 * <CODE>FNPTask3, FNPTask3Pool, FNPTask3ThreadLocalPools</CODE>
 * implement a pattern that this author termed "The Thread-Local Object-Pool
 * Design Pattern" that can result in dramatic speedups in run-time especially
 * when an application would need to create lots of <CODE>FNPTask3</CODE> objects
 * simultaneously from many concurrent threads. Not part of the public API.
 * Notice: This implementation only allows the creation of pools of 
 * <CODE>FNPTask3</CODE> objects of a single size for each thread. In
 * case the <CODE>getThreadLocalPool(n)</CODE> method is called in the same
 * thread more than once with different argument values, the first call that
 * has an arg. value different than the initial call to the method, will return 
 * null. Therefore, this implementation is not well-suited to handling variable
 * length objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class FNPTask3ThreadLocalPools {
  private static boolean _poolSizeResetAllowed = true;

	private static ThreadLocal _pools = new ThreadLocal() {
    protected Object initialValue() {
      return null;
    }
  };

  static FNPTask3Pool getThreadLocalPool(int n) throws IllegalArgumentException {
    if (n<=0) throw new IllegalArgumentException("FNPTask3ThreadLocalPools.getThreadLocalPool(n): n is <= 0");
		FNPTask3Pool p = (FNPTask3Pool) _pools.get();
    if (p==null) {
			synchronized (FNPTask3ThreadLocalPools.class) {
				_poolSizeResetAllowed=false;
			}
      p = new FNPTask3Pool(n);
			p.initialize();
      _pools.set(p);
    } else {
			if (n!=p.getVectorsLength()) return null;
		}
    return p;
  }

	
	/**
	 * sets the size of the thread-local pools. Must only be called once, before
	 * any other call to methods of this class or any other of the FNPTask3* 
	 * classes.
	 * @param poolsize int
	 * @throws ParallelException if there has been a call to any of the other 
	 * methods of the family of FNPTask3* classes.
	 * @throws IllegalArgumentException if poolsize &le; 0.
	 */
	public static synchronized void setPoolSize(int poolsize) 
	  throws ParallelException, IllegalArgumentException {
		if (!_poolSizeResetAllowed) 
			throw new ParallelException(
				"popt4jlib.NumberPartitioning.FNPTask3ThreadLocalPools.setPoolSize(): "+
				"getThreadLocalPool(n) or setPoolSize(size) has already been called from some thread");
		_poolSizeResetAllowed = false;
		FNPTask3Pool.setPoolSize(poolsize);
	}

}
