package utils;

/**
 * auxiliary class using the <CODE>ThreadLocal</CODE> mechanism to create
 * thread-local PairIntDouble object pools. Essentially, the three classes
 * <CODE>PairIntDouble, PairIntDoublePool, PairIntDoubleThreadLocalPools</CODE>
 * implement a pattern that this author termed "The Thread-Local Object-Pool
 * Design Pattern" that can result in dramatic speedups in run-time especially
 * when an application would need to create big numbers of PairIntDouble objects
 * simultaneously from many concurrent threads.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PairIntDoubleThreadLocalPools {
  private static ThreadLocal _pools = new ThreadLocal() {
    protected Object initialValue() {
      return null;
    }
  };

  static PairIntDoublePool getThreadLocalPool() {
    PairIntDoublePool p = (PairIntDoublePool) _pools.get();
    if (p==null) {
      p = new PairIntDoublePool();
			p.initialize();
      _pools.set(p);
    }
    return p;
  }

}

