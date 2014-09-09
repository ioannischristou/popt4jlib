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
 * <p>Copyright: Copyright (c) 2011</p>
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

  /**
   * sole public constructor. Creates <CODE>_NUMOBJS</CODE> "empty"
   * PairIntDouble objects.
   */
  PairIntDoublePool() {
    _pool = new ArrayList(_NUMOBJS);
    for (int i=0; i<_NUMOBJS; i++) {
      _pool.add(new PairIntDouble(this, i));
    }
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
   * return an unmanaged "free" objec from the pool, or null if it cannot find
   * one.
   * @return PairIntDouble
   */
  PairIntDouble getObjectFromPool() {
    if (_maxUsedPos<_NUMOBJS-1) {
      _maxUsedPos++;
      PairIntDouble dlp = (PairIntDouble) _pool.get(_maxUsedPos);
      dlp.setIsUsed();
      return dlp;
    }
    else return null;
  }

  void returnObjectToPool(PairIntDouble dlp) {
    if (dlp.getPoolPos()==_maxUsedPos) {
      --_maxUsedPos;
      while (_maxUsedPos>=0 &&
             ((PairIntDouble)_pool.get(_maxUsedPos)).isUsed()==false)
        --_maxUsedPos;
    }
    return;
  }

}

