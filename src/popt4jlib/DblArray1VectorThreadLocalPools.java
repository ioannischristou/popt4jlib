/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib;

/**
 * auxiliary class using the <CODE>ThreadLocal</CODE> mechanism to create
 * thread-local DblArray1Vector object pools. Essentially, the three classes
 * <CODE>DblArray1Vector, DblArray1VectorPool, 
 * DblArray1VectorThreadLocalPools</CODE>
 * implement a pattern that this author termed "The Thread-Local Object-Pool
 * Design Pattern" that can result in dramatic speedups in run-time especially
 * when an application would need to create lots of DblArray1Vector objects
 * simultaneously from many concurrent threads. Not part of the public API.
 * <p> Notice: This implementation only allows the creation of pools of 
 * <CODE>DblArray1Vector</CODE> objects of a single size for each thread. In
 * case the <CODE>getThreadLocalPool(n)</CODE> method is called in the same
 * thread more than once with different argument values, the first call that
 * has an arg. value different than the initial call to the method, will return 
 * null. Therefore, this implementation is not well-suited to handling variable
 * length vectors.
 * </p>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final public class DblArray1VectorThreadLocalPools {
  private static boolean _poolSizeResetAllowed = true;
	
	private static ThreadLocal _pools = new ThreadLocal() {
    protected Object initialValue() {
      return null;
    }
  };

  static DblArray1VectorPool getThreadLocalPool(int n) throws IllegalArgumentException {
    if (n<=0) throw new IllegalArgumentException("DblArray1VectorThreadLocalPools.getThreadLocalPool(n): n is <= 0");
		DblArray1VectorPool p = (DblArray1VectorPool) _pools.get();
    if (p==null) {
			synchronized (DblArray1VectorThreadLocalPools.class) {
				_poolSizeResetAllowed=false;
			}
      p = new DblArray1VectorPool(n);
			p.initialize();
      _pools.set(p);
    } else {
			if (n!=p.getVectorsLength()) return null;
		}
    return p;
  }

	
	/**
	 * sets the size of the thread-local pools. Must only be called once, before
	 * any other call to methods of this class or any other of the 
	 * DblArray1Vector* classes.
	 * @param poolsize int
	 * @throws IllegalStateException if there has been a call to any of the other 
	 * methods of the family of DblArray1Vector* classes.
	 * @throws IllegalArgumentException if poolsize &le; 0.
	 */
	public static synchronized void setPoolSize(int poolsize) 
	  throws IllegalArgumentException, IllegalStateException {
		if (!_poolSizeResetAllowed) 
			throw new IllegalStateException(
				"popt4jlib.DblArray1VectorThreadLocalPools.setPoolSize(): "+
				"getThreadLocalPool() or setPoolSize() has already been called from some thread");
		_poolSizeResetAllowed = false;
		DblArray1VectorPool.setPoolSize(poolsize);
	}

}
