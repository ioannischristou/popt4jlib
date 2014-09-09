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
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class RegisteredParcelThreadLocalPools {
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
      p = new RegisteredParcelPool();
      _pools.set(p);
    }
    return p;
  }

}

