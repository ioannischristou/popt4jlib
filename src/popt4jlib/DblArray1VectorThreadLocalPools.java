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
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1VectorThreadLocalPools {
  private static ThreadLocal _pools = new ThreadLocal() {
    protected Object initialValue() {
      return null;
    }
  };

  static DblArray1VectorPool getThreadLocalPool(int n) {
    DblArray1VectorPool p = (DblArray1VectorPool) _pools.get();
    if (p==null) {
      p = new DblArray1VectorPool(n);
      _pools.set(p);
    }
    return p;
  }

	
}
