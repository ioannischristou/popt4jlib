package utils;

import java.util.ArrayList;

/**
 * The class is responsible for maintaining a sufficiently large array of
 * PairIntDouble objects, and there will be a pool for each thread of execution.
 * In this way, there will be no need for invoking the new operator within the
 * running threads requiring PairIntDouble objects, unless a thread at some
 * point runs out of space in its thread-local pool.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PairIntDoublePool {
  /**
   * the maximum number of objects this pool can handle.
   */
  private static final int _NUMOBJS = 20000;
  private ArrayList _pool;
  private int _maxUsedPos=-1;
	private int _minUsedPos=_NUMOBJS;
	
	private static final boolean _DO_GET_SANITY_TEST=false;
	

  /**
   * sole public constructor. Creates <CODE>_NUMOBJS</CODE> "empty"
   * PairIntDouble objects.
   */
  PairIntDoublePool() {
    _pool = new ArrayList(_NUMOBJS);
		/* itc: 2015-01-15: moved code below to initialize() method
    for (int i=0; i<_NUMOBJS; i++) {
      _pool.add(new PairIntDouble(this, i));
    }
		*/
  }


  /**
   * factory object creation method, first tries to return a managed object from
   * the pool, and if it cannot find one, creates a new (unmanaged) one. The
   * returned object, always has its data members (values) correctly set.
   * @param i int
   * @param val double
   * @return PairIntDouble
   */
  static PairIntDouble getObject(int i, double val) {
    PairIntDoublePool pool = PairIntDoubleThreadLocalPools.getThreadLocalPool();
    PairIntDouble p = pool.getObjectFromPool();
    if (p!=null) {  // ok, return managed object
      p.setKey(i);
      p.setVal(val);
      return p;
    } else  // oops, create new unmanaged object
      return new PairIntDouble(i, val);
  }


	/**
	 * method is only called from <CODE>PairIntDoubleThreadLocalPools</CODE>
	 * right after this object is constructed to avoid escaping "this" in the 
	 * constructor.
	 */
	void initialize() {
    for (int i=0; i<_NUMOBJS; i++) {
      _pool.add(new PairIntDouble(this, i));
    }		
	}
	

  /**
   * return an unmanaged "free" object from the pool, or null if it cannot find
   * one.
   * @return PairIntDouble
   */
  PairIntDouble getObjectFromPool() {
    if (_maxUsedPos<_NUMOBJS-1) {  // try the right end
      _maxUsedPos++;
      PairIntDouble dlp = (PairIntDouble) _pool.get(_maxUsedPos);
      dlp.setIsUsed();
      return dlp;
    } else {  // try the left end
			if (_minUsedPos>0) {
				_minUsedPos--;
				PairIntDouble dlp = (PairIntDouble) _pool.get(_minUsedPos);
				if (_DO_GET_SANITY_TEST && dlp.isUsed()) {
					Integer yI = null;
					System.err.println("getObjectFromPool(): left doesn't work: null ref yI="+yI.intValue());  // force NullPointerException
				}
				dlp.setIsUsed();
				if (_minUsedPos>_maxUsedPos) _maxUsedPos = _minUsedPos;
				return dlp;
			}
		}
    return null;
  }

	
	/**
	 * return the argument into the pool.
	 * @param dlp PairIntDouble
	 */
  void returnObjectToPool(PairIntDouble dlp) {
		// corner case: the returned object was the only one "out-of-the-pool"
		if (_maxUsedPos==_minUsedPos) {
			_maxUsedPos = -1;
			_minUsedPos = _NUMOBJS;
			return;
		}
    if (dlp.getPoolPos()==_maxUsedPos) {
      --_maxUsedPos;
      while (_maxUsedPos>=0 &&
             ((PairIntDouble)_pool.get(_maxUsedPos)).isUsed()==false)
        --_maxUsedPos;
    }
  	if (dlp.getPoolPos()==_minUsedPos) {
			++_minUsedPos;
			while (_minUsedPos<_NUMOBJS &&
						 ((PairIntDouble)_pool.get(_minUsedPos)).isUsed()==false)
				++_minUsedPos;
		}
		return;
  }

}

