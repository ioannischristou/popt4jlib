/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.NumberPartitioning;

import java.util.*;


/**
 * implements a stack-based thread-local pool of <CODE>FNPTask2</CODE> objects.
 * In this way, there will be no need for invoking the new operator within the
 * running threads requiring such objects, unless a thread at some
 * point runs out of space in its thread-local pool. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class FNPTask2Pool {
  /**
   * the maximum number of objects this pool can handle.
   */
  private static int _NUMOBJS = 100000;
  private FNPTask2[] _pool;
  private int _topAvailPos=-1;
	private int _vecLen;
	
	
  /**
   * sole constructor. Creates <CODE>_NUMOBJS</CODE> "empty"
   * FNPTask2 objects.
	 * @param n int the fixed size of the long array of each object in the pool.
   */
  public FNPTask2Pool(int n) {
    _pool = new FNPTask2[_NUMOBJS];
		/* itc: 2015-01-15: moved code below to initialize() method
    for (int i=0; i<_NUMOBJS; i++) {
      _pool[i] = new FNPTask2(this, n);
    }
		_topAvailPos = _NUMOBJS-1;
		*/
		_vecLen = n;
  }


	/**
   * factory object creation method, first tries to return a managed object from
   * the pool, and if it cannot find one, creates a new (unmanaged) one. The
   * returned object, always has its data member correctly set.
	 * @param n int array size
	 * @return FNPTask2
	 */
  static FNPTask2 getObject(int n) {
    FNPTask2Pool pool = FNPTask2ThreadLocalPools.getThreadLocalPool(n);
    if (pool==null) return new FNPTask2(n);  // no pool for such size
		FNPTask2 p = pool.getObjectFromPool();
    try {
			if (p!=null) {  // ok, return managed object
			  return p;
			} else  // oops, create new unmanaged object
				return new FNPTask2(n);
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		}
  }

	
	/**
	 * method is only called from <CODE>FNPTask2ThreadLocalPools</CODE> right
	 * after this object is constructed to avoid escaping "this" in the 
	 * constructor.
	 */
	void initialize() {
    for (int i=0; i<_NUMOBJS; i++) {
      _pool[i] = new FNPTask2(this, _vecLen);
    }
		_topAvailPos = _NUMOBJS-1;		
	}
	
	
	/**
	 * returns the argument to this pool, IFF it belongs to this pool.
	 * @param ind FNPTask2
	 */
  void returnObjectToPool(FNPTask2 ind) {
		_pool[++_topAvailPos] = ind;
    return;
  }
	
	
	/**
	 * return the length of the vectors in this pool.
	 * @return int
	 */
	int getVectorsLength() {
		return _vecLen;
	}

	
  /**
   * return an managed "free" object from the pool, or null if it cannot find
   * one.
   * @return FNPTask
   */
  private FNPTask2 getObjectFromPool() {
    if (_topAvailPos>=0) {
			FNPTask2 obj = _pool[_topAvailPos--];
			obj.setIsUsed();
			return obj;
		}
		return null;
  }

	
	/**
	 * sets the number of objects in the thread-local pools to the size specified.
	 * Must only be called once, before any pool is actually constructed (should
	 * only be called from the <CODE>FNPTask2ThreadLocalPools</CODE> class).
	 * @param num int
	 * @throws IllegalArgumentException if the argument is &le; 0
	 */
	static void setPoolSize(int num) throws IllegalArgumentException {
		if (num <= 0) 
			throw new IllegalArgumentException("setPoolSize(n): n<=0");
		_NUMOBJS = num;
	}

}
