package parallel;

/**
 * auxiliary class using the <CODE>ThreadLocal</CODE> mechanism to create
 * thread-local <CODE>RegisteredParcel</CODE> object pools. Essentially, the
 * three classes <CODE>RegisteredParcel, RegisteredParcelPool,
 * RegisteredParcelThreadLocalPools</CODE>
 * implement a pattern that this author termed "The Thread-Local Object-Pool
 * Design Pattern" that can result in dramatic speedups in run-time especially
 * when an application would need to create big numbers of RegisteredParcel
 * objects simultaneously from many concurrent threads.
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class RegisteredParcelThreadLocalPools {
  private static boolean _poolSizeResetAllowed = true;
	
	private static ThreadLocal _pools = new ThreadLocal() {
    protected Object initialValue() {
      return null;
    }
  };

	
  /**
   * returns the thread-local RegisteredParcelPool object associated with the
   * current thread invoking this method. Used only in the
   * <CODE>RegisteredParcelPool.getObject(from,to,data)</CODE> method.
   * @return RegisteredParcelPool
   */
  static RegisteredParcelPool getThreadLocalPool() {
    RegisteredParcelPool p = (RegisteredParcelPool) _pools.get();
    if (p==null) {
			synchronized (RegisteredParcelThreadLocalPools.class) {
				_poolSizeResetAllowed=false;
			}
      p = new RegisteredParcelPool();
			p.initialize();
      _pools.set(p);
    }
    return p;
  }
	
	
	/**
	 * sets the size of the thread-local pools. Must only be called once, before
	 * any other call to methods of this class or any other of the 
	 * RegisteredParcel* classes.
	 * @param poolsize
	 * @throws ParallelException if there has been a call to any of the other 
	 * methods of the family of RegisteredParcel* classes.
	 * @throws IllegalArgumentException if poolsize &lte 0.
	 */
	public static synchronized void setPoolSize(int poolsize) 
	  throws ParallelException, IllegalArgumentException {
		if (!_poolSizeResetAllowed) 
			throw new ParallelException(
				"parallel.RegisteredParcelThreadLocalPools.setPoolSize(): "+
				"getThreadLocalPool() or setPoolSize has already been called from some thread");
		_poolSizeResetAllowed = false;
		RegisteredParcelPool.setPoolSize(poolsize);
	}

}

