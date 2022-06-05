package popt4jlib.NumberPartitioning;

import java.util.*;


/**
 * implements a thread-local pool of <CODE>FNPTask</CODE> objects.
 * In this way, there will be no need for invoking the new operator within the
 * running threads requiring such objects, unless a thread at some
 * point runs out of space in its thread-local pool. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class FNPTaskPool {
  /**
   * the maximum number of objects this pool can handle.
   */
  private static int _NUMOBJS = 100000;
  private ArrayList _pool;
  private int _maxUsedPos=-1;
	private int _minUsedPos=_NUMOBJS;
	private int _vecLen=-1;
	
	
	/**
	 * compile-time constant used to ensure no thread other than the one
	 * used in "creating" an object may release it.
	 */
	private static final boolean _DO_RELEASE_SANITY_TEST=true;
	/**
	 * compile-time constant used to ensure that no object returned, was "used"
	 * at the time of return.
	 */
	private static final boolean _DO_GET_SANITY_TEST=true;
	
	
  /**
   * sole constructor. Creates <CODE>_NUMOBJS</CODE> "empty"
   * FNPTask objects.
	 * @param n int the fixed size of the long array of each object in the pool.
   */
  public FNPTaskPool(int n) {
    _pool = new ArrayList(_NUMOBJS);
		_vecLen = n;
		/* itc: 2015-01-15: moved code below to initialize() method
    for (int i=0; i<_NUMOBJS; i++) {
      _pool.add(new FNPTask(n, this, i));
    } 
		*/
  }


	/**
   * factory object creation method, first tries to return a managed object from
   * the pool, and if it cannot find one, creates a new (unmanaged) one. The
   * returned object, always has its data member correctly set.
	 * @param n int array size
	 * @return FNPTask
	 */
  static FNPTask getObject(int n) {
    FNPTaskPool pool = FNPTaskThreadLocalPools.getThreadLocalPool(n);
    if (pool==null) return new FNPTask(n);  // no pool for such size
		FNPTask p = pool.getObjectFromPool();
    try {
			if (p!=null) {  // ok, return managed object
			  return p;
			} else  // oops, create new unmanaged object
				return new FNPTask(n);
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		}
  }

	
	/**
	 * method is only called from <CODE>FNPTaskThreadLocalPools</CODE> right after
	 * this object is constructed to avoid escaping "this" in the constructor.
	 */
	void initialize() {
    for (int i=0; i<_NUMOBJS; i++) {
      _pool.add(new FNPTask(_vecLen, this, i));
    } 		
	}
	
	
	/**
	 * returns the argument to this pool, IFF it belongs to this pool.
	 * @param ind FNPTask
	 */
  void returnObjectToPool(FNPTask ind) {
		if (_DO_RELEASE_SANITY_TEST) {
			FNPTaskPool pool = 
				FNPTaskThreadLocalPools.getThreadLocalPool(ind.getSize());
			if (pool!=this) {
				/*
				Integer yI = null;
				System.err.println("null ref yI="+yI.intValue());  // force NullPointerException
				*/
				throw new Error("FNPTaskPool.returnObjectToPool(): pool!=this");
			}
		}
		// corner case: the returned object was the only one "out-of-the-pool"
		if (_maxUsedPos==_minUsedPos) {
			if (_DO_RELEASE_SANITY_TEST) {
				if (ind.getPoolPos()!=_minUsedPos) {
					/*
					Integer yI = null;
					System.err.println("null ref yI="+yI.intValue());  // force NullPointerException					
					*/
					throw new Error("FNPTaskPool.returnObjectToPool(o): o.getPoolPos() "+
						              "is not equal to _minUsedPos though "+
						              "_minUsedPos=_maxUsedPos");
				}
			}
			_maxUsedPos = -1;
			_minUsedPos = _NUMOBJS;
			return;
		}
    if (ind.getPoolPos()==_maxUsedPos) {
      --_maxUsedPos;
      while (_maxUsedPos>=0 &&
             ((FNPTask)_pool.get(_maxUsedPos)).isUsed()==false)
        --_maxUsedPos;
    }
		if (ind.getPoolPos()==_minUsedPos) {
			++_minUsedPos;
			while (_minUsedPos<_NUMOBJS &&
						 ((FNPTask)_pool.get(_minUsedPos)).isUsed()==false)
				++_minUsedPos;
		}
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
  private FNPTask getObjectFromPool() {
    if (_maxUsedPos<_NUMOBJS-1) {  // try right end
      _maxUsedPos++;
      FNPTask ind = (FNPTask) _pool.get(_maxUsedPos);
      ind.setIsUsed();
			if (_minUsedPos>_maxUsedPos) _minUsedPos = _maxUsedPos;
      return ind;
    } else {  // try the left end
			if (_minUsedPos>0) {
				_minUsedPos--;
				FNPTask ind = (FNPTask) _pool.get(_minUsedPos);
				if (_DO_GET_SANITY_TEST && ind.isUsed()) {
					/*
					Integer yI = null;
					System.err.println("getObjectFromPool(): left doesn't work: null ref yI="+yI.intValue());  // force NullPointerException
					*/
					throw new Error("FNPTaskPool.getObjectFromPool(): left doesn't work");
				}
				ind.setIsUsed();
				if (_minUsedPos>_maxUsedPos) _maxUsedPos = _minUsedPos;
				return ind;
			}
		}
		return null;
  }

	
	/**
	 * sets the number of objects in the thread-local pools to the size specified.
	 * Must only be called once, before any pool is actually constructed (should
	 * only be called from the <CODE>FNPTaskThreadLocalPools</CODE> class).
	 * @param num int
	 * @throws IllegalArgumentException if the argument is &le; 0
	 */
	static void setPoolSize(int num) throws IllegalArgumentException {
		if (num <= 0) 
			throw new IllegalArgumentException("setPoolSize(n): n<=0");
		_NUMOBJS = num;
	}

}
