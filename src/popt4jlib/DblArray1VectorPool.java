/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib;

import java.util.*;


/**
 * implements a thread-local pool of DblArray1Vector objects.
 * In this way, there will be no need for invoking the new operator within the
 * running threads requiring such objects, unless a thread at some
 * point runs out of space in its thread-local pool. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class DblArray1VectorPool {
  /**
   * the maximum number of objects this pool can handle.
   */
  private static final int _NUMOBJS = 20000;
  private ArrayList _pool;
  private int _maxUsedPos=-1;
	/**
	 * compile-time constant used to ensure no thread other than the one
	 * used in "creating" an object may release it.
	 */
	private static final boolean _DO_RELEASE_SANITY_TEST=true;
	/**
	 * compile-time constant used to search the first _NUM_POS_2_TRY positions
	 * in the pool when _maxUsedPos reaches its upper bound, for a freed object.
	 * Must always be less than _NUMOBJS, preferably a very small number (around 
	 * 10) for speed.
	 */
	private static final int _NUM_POS_2_TRY=10;
	
	
  /**
   * sole constructor. Creates <CODE>_NUMOBJS</CODE> "empty"
   * DblArray1Vector objects.
	 * @param n int the fixed size of each vector in the pool.
   */
  public DblArray1VectorPool(int n) {
    _pool = new ArrayList(_NUMOBJS);
    for (int i=0; i<_NUMOBJS; i++) {
      _pool.add(new DblArray1Vector(n, this, i));
    }
  }


	/**
   * factory object creation method, first tries to return a managed object from
   * the pool, and if it cannot find one, creates a new (unmanaged) one. The
   * returned object, always has its data member correctly set.
	 * @param n int array size
	 * @return DblArray1Vector
	 */
  static DblArray1Vector getObject(int n) {
    DblArray1VectorPool pool = DblArray1VectorThreadLocalPools.getThreadLocalPool(n);
    DblArray1Vector p = pool.getObjectFromPool();
    try {
			if (p!=null) {  // ok, return managed object
			  return p;
			} else  // oops, create new unmanaged object
				return new DblArray1Vector(n);
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		}
  }

	
	/**
	 * returns the argument to this pool, IFF it belongs to this pool.
	 * @param ind DblArray1Vector
	 */
  void returnObjectToPool(DblArray1Vector ind) {
		if (_DO_RELEASE_SANITY_TEST) {
			DblArray1VectorPool pool = DblArray1VectorThreadLocalPools.getThreadLocalPool(ind.getNumCoords());
			if (pool!=this) {
				Integer yI = null;
				System.err.println("null ref yI="+yI.intValue());  // force NullPointerException
			}
		}
    if (ind.getPoolPos()==_maxUsedPos) {
      --_maxUsedPos;
      while (_maxUsedPos>=0 &&
             ((DblArray1Vector)_pool.get(_maxUsedPos)).isUsed()==false)
        --_maxUsedPos;
    }
    return;
  }

	
  /**
   * return an unmanaged "free" object from the pool, or null if it cannot find
   * one.
   * @return DblArray1Vector
   */
  private DblArray1Vector getObjectFromPool() {
    if (_maxUsedPos<_NUMOBJS-1) {
      _maxUsedPos++;
      DblArray1Vector ind = (DblArray1Vector) _pool.get(_maxUsedPos);
      ind.setIsUsed();
      return ind;
    } else {
			for (int i=0; i<_NUM_POS_2_TRY; i++) {
				DblArray1Vector indi = (DblArray1Vector) _pool.get(i);
				if (!indi.isUsed()) {
					indi.setIsUsed();
					return indi;				
				}
			}
		}
		return null;
  }
	
}
