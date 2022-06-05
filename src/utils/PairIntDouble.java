package utils;

import java.io.Serializable;


/**
 * utility class with the same semantics as the Pair class, but restricted so
 * that the first object in the Pair is an int and the second a double. Also,
 * it implements the Thread-Local Object Pool mechanism for significantly
 * reducing new object allocations (especially useful in multi-threaded
 * contexts.) However, it becomes the responsibility of the programmer to always
 * call <CODE>release()</CODE> on an object that is no longer needed to free-up
 * objects in the pool, otherwise, the pool will soon be exhausted, and when
 * new objects are needed, a call to <CODE>new PairIntDouble(i,val)</CODE> will
 * occur each time afterwards.
 * Notes:
 * <ul>
 * <li> 20191227: modified <CODE>hashCode()</CODE> to work according to Bloch's
 * recommendations.
 * <li> 20211009: corrected <CODE>hashCode()</CODE> for the int field part as
 * FindBugs pointed out; forced Error to be thrown in some (impossible) error
 * conditions that previously would throw NullPointerException's; if an object 
 * of this class travels through the wire to another JVM process, it becomes 
 * immediately "unmanaged" in that it belongs to no pool, and the related fields
 * are appropriately set
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
public class PairIntDouble implements Comparable, Serializable {
  // private static final long serialVersionUID=...L;
  private int _key;
  private double _val;
  transient private PairIntDoublePool _pool;
	// itc-20211009: below values are needed when this object is transferred
	// through the wire, so it becomes "unmanaged"
  transient private int _poolPos=-1;       // itc-20211009 used to be 0
  transient private boolean _isUsed=true;  // itc-20211009 used to be false
  // private static long _totalNumObjs=0;

	
  /**
   * returns a managed object if space exists for the current thread, else
   * creates a new unmanaged object. This method forms the only public
   * interface method for getting a new object (the programmer has no control
   * over whether the object will come from the current thread's local pool
   * or via a call to <CODE>new PairIntDouble(i,val);</CODE>.)
   * @param i int
   * @param val double
   * @return PairIntDouble
   */
  public static PairIntDouble newInstance(int i, double val) {
    return PairIntDoublePool.getObject(i, val);
  }


  /**
   * get the key.
   * @return int
   */
  public int getInt() {
    return _key;
  }


  /**
   * get the value.
   * @return double
   */
  public double getDouble() {
    return _val;
  }


  /**
   * indicate item is available for re-use by Object-Pool to which it belongs,
   * and resets its _key and _val data IFF it is a managed object.
   */
  public void release() {
    if (_pool!=null) {
      _isUsed=false;
      _key=-1;
      _val=0.0;
      _pool.returnObjectToPool(this);
    }
  }


  /**
   * comparison is based on the double value <CODE>_val</CODE> of this object;
   * as well as <CODE>_key</CODE>.
   * @param o Object
   * @return boolean
   */
  public boolean equals(Object o) {
//    if (o==null) return false;
//    try {
//      PairIntDouble dd = (PairIntDouble) o;
//      if (_val==dd._val) return true;
//      else return false;
//    }
//    catch (ClassCastException e) {
//      return false;
//    }
    if (o==this) return true;
    if (o==null) return false;
    try {
      PairIntDouble dd = (PairIntDouble) o;
      return Double.compare(_val, dd._val)==0 && _key == dd._key;
    }
    catch (ClassCastException e) {
      return false;
    }
  }


  /**
   * returns the hash-code computed according to Joshua Bloch's suggestions in 
	 * "Effective Java 2".
   * @return int
   */
  public int hashCode() {
    if (Double.isNaN(_val) || Double.isInfinite(_val))
      return 0;  // guard against NAN or infinity values
    // return (int) Math.floor(_val);
		int result = 17;
		// int c = (int)(_key ^ (_key >>> 32));
		// itc20211009: above is redundant and confusing for non-long fields
		int c = _key;
		result = 31*result + c;
		long tmplvl = Double.doubleToLongBits(_val);
		c = (int)(tmplvl ^ (tmplvl >>> 32));
		result = 31*result + c;
		return result;		
  }


  /**
   * comparison is based on the double value <CODE>_val</CODE> of this object
   * (increasing order of <CODE>_val</CODE>), with secondary key, the
   * <CODE>_key</CODE> value.
   * @param o Object
   * @return int
   */
  public int compareTo(Object o) {
    // PairIntDouble c = (PairIntDouble) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    // if (_val < c._val) return -1;
    // else if (_val == c._val) return 0;
    // else return 1;
    if (o==this) return 0;
    PairIntDouble c = (PairIntDouble) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    //if (_val < c._val) return -1;
    //else if (_val == c._val) return 0;
    //else return 1;
    int v = Double.compare(_val, c._val);
    return v!=0 ? v : (_key < c._key ? -1 : (_key > c._key ? 1 : 0));
  }


  /**
   * this constructor is to be used only from the PairIntDoublePool for
   * constructing managed objects (re-claimable ones).
   * @param pool
   * @param poolpos
   */
  PairIntDouble(PairIntDoublePool pool, int poolpos) {
    _pool=pool;
    _poolPos=poolpos;
    _isUsed=false;
    _key=-1;
    _val=0.0;
    /*
    synchronized (PairIntDouble.class) {
      ++_totalNumObjs;
    }
    */
  }


  /**
   * constructor is used to create unmanaged objects only.
   * @param i int
   * @param val double
   */
  PairIntDouble(int i, double val) {
    _key = i;  _val = val;
    _pool=null;
    _poolPos=-1;
    _isUsed=true;
    /*
    synchronized (PairIntDouble.class) {
      ++_totalNumObjs;
    }
    */
  }


  /**
   * this method exists only for the Object-Pool mechanism.
   * @param id long
   */
  void setKey(int id) {
    if (_isUsed) {
      _key = id;
    } else {
			/* itc-20211009: throw Error instead
      // force a NullPointerException for debugging the pooling mechanism
      Integer null_y=null;
      _key = null_y.intValue();
			*/
			throw new Error("PairIntDouble.setKey(): _isUsed is false?");
    }
  }


  /**
   * this method exists only for the Object-Pool mechanism.
   * @param v double
   */
  void setVal(double v) {
    if (_isUsed) _val = v;
    else {  
			/* itc-20211009: throw Error instead
      // force a NullPointerException for debugging the pooling mechanism
      Double null_y=null;
      _val = null_y.doubleValue();
			*/
			throw new Error("PairIntDouble.setVal(): _isUsed is false?");
    }
  }


  /**
   * return true IFF the object is managed and "currently used", or un-managed.
   * @return boolean
   */
  boolean isUsed() {
    return _isUsed;
  }


  /*
  public static synchronized long getTotalNumObjs() {
    return _totalNumObjs;
  }
  */

  void setIsUsed() {
    _isUsed=true;
  }

  int getPoolPos() {
    return _poolPos;
  }

}
