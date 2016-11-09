/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.NumberPartitioning;

import java.util.*;


/**
 * implements a thread-local pool of <CODE>ReleaseFNPTaskObject</CODE> objects.
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
class ReleaseFNPTaskObjectPool {
  /**
   * the maximum number of objects this pool can handle.
   */
  private static int _NUMOBJS = 100000;
  private ArrayList _pool;
  private int _maxUsedPos=-1;
	private int _minUsedPos=_NUMOBJS;


  /**
   * sole public constructor. Reserves <CODE>_NUMOBJS</CODE> "empty"
   * <CODE>ReleaseFNPTaskObject</CODE> objects, then the 
	 * <CODE>initialize()</CODE> method creates them.
   */
  ReleaseFNPTaskObjectPool() {
    _pool = new ArrayList(_NUMOBJS);
		/* itc: 2015-01-15 moved code below to initialize() method
    for (int i=0; i<_NUMOBJS; i++) {
      _pool.add(new ReleaseFNPTaskObject(this, i));
    }
		*/
  }


  /**
   * factory object creation method, first tries to return a managed object from
   * the pool, and if it cannot find one, creates a new (unmanaged) one. The
   * returned object, always has its data members (values) correctly set.
   * @param data FNPTask 
   * @param threadid int
   * @return ReleaseFNPTaskObject
   */
  static ReleaseFNPTaskObject getObject(FNPTask data, int threadid) {
    ReleaseFNPTaskObjectPool pool = ReleaseFNPTaskObjectThreadLocalPools.getThreadLocalPool();
    ReleaseFNPTaskObject p = pool.getObjectFromPool();
    if (p!=null) {  // ok, return managed object
      p.setData(data, threadid);
      return p;
    } else  // oops, create new unmanaged object
      return new ReleaseFNPTaskObject(data, threadid);
  }

	
	/**
	 * method is only called from <CODE>ReleaseFNPTaskObjectThreadLocalPools</CODE> 
	 * once right after this object is constructed to avoid escaping "this" in the
	 * constructor.
	 */
	void initialize() {
    for (int i=0; i<_NUMOBJS; i++) {
      _pool.add(new ReleaseFNPTaskObject(this, i));
    }		
	}

  /**
   * return a managed "free" object from the pool, or null if it cannot find
   * one.
   * @return ReleaseFNPTaskObject
   */
  ReleaseFNPTaskObject getObjectFromPool() {
    if (_maxUsedPos<_NUMOBJS-1) {  // try the right end
      _maxUsedPos++;
      ReleaseFNPTaskObject dlp = (ReleaseFNPTaskObject) _pool.get(_maxUsedPos);
      dlp.setIsUsed();
			if (_minUsedPos>_maxUsedPos) _minUsedPos = _maxUsedPos;
      return dlp;
    } else {  // try the left end
			if (_minUsedPos>0) {
				_minUsedPos--;
				ReleaseFNPTaskObject ind = (ReleaseFNPTaskObject) _pool.get(_minUsedPos);
				ind.setIsUsed();
				if (_minUsedPos>_maxUsedPos) _maxUsedPos = _minUsedPos;
				return ind;
			}
		}
    return null;
  }


  /**
   * only called from <CODE>ReleaseFNPTaskObject.release()</CODE> method.
   * @param dlp ReleaseFNPTaskObject
   */
  void returnObjectToPool(ReleaseFNPTaskObject dlp) {
		// corner case: the returned object was the only one "out-of-the-pool"
		if (_maxUsedPos==_minUsedPos) {
			_maxUsedPos = -1;
			_minUsedPos = _NUMOBJS;
			return;
		}
    if (dlp.getPoolPos()==_maxUsedPos) {
      --_maxUsedPos;
      while (_maxUsedPos>=0 &&
             ((ReleaseFNPTaskObject)_pool.get(_maxUsedPos)).isUsed()==false)
        --_maxUsedPos;
    }
		if (dlp.getPoolPos()==_minUsedPos) {
			++_minUsedPos;
			while (_minUsedPos<_NUMOBJS &&
						 ((ReleaseFNPTaskObject)_pool.get(_minUsedPos)).isUsed()==false)
				++_minUsedPos;
		}
    return;
  }
	
	
	/**
	 * sets the number of objects in the thread-local pools to the size specified.
	 * Must only be called once, before any pool is actually constructed (should
	 * only be called from the ReleaseFNPTaskObjectThreadLocalPools class).
	 * @param num int
	 * @throws IllegalArgumentException if the argument is &le; 0
	 */
	static void setPoolSize(int num) throws IllegalArgumentException {
		if (num <= 0) 
			throw new IllegalArgumentException("setPoolSize(n): n<=0");
		_NUMOBJS = num;
	}

}
