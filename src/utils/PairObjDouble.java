package utils;

/**
 * utility class with the same semantics as PairIntDouble except the first (key)
 * object in a PairObjDouble can be any Object.
 * Notes:
 * <ul>
 * <li> 20191227: modified <CODE>hashCode()</CODE> to work according to Bloch's
 * suggestions.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PairObjDouble implements Comparable {
  Object _arg;
  double _val;

  /**
   * public constructor
   * @param arg Object
   * @param val double
   */
  public PairObjDouble(Object arg, double val) {
    _arg = arg;  _val = val;
  }


  /**
   * return the first (key) object
   * @return Object
   */
  public Object getArg() {
    return _arg;
  }


  /**
   * return the second (value) object, being a double
   * @return double
   */
  public double getDouble() {
    return _val;
  }


  /**
   * comparison based on the value (<CODE>_val</CODE>) object
   * @param o Object
   * @return boolean
   */
  public boolean equals(Object o) {
    if (o==this) return true;
    if (o==null) return false;
    try {
      PairObjDouble dd = (PairObjDouble) o;
      return Double.compare(_val, dd._val)==0;
      //if (_val==dd._val) return true;
      //else return false;
    }
    catch (ClassCastException e) {
      return false;
    }
  }


  /**
   * as in PairIntDouble, only use the <CODE>_val</CODE> field for the hash-code
	 * computation.
   * @return int
   */
  public int hashCode() {
    if (Double.isNaN(_val)) return 0;  // guard against NAN value
    // return (int) Math.floor(_val);
		int result = 17;
		long tmplvl = Double.doubleToLongBits(_val);
		int c = (int)(tmplvl ^ (tmplvl >>> 32));
		result = 31*result + c;
		return result;		
  }


  /**
   * as in PairIntDouble
   * @param o Object
   * @return int
   */
  public int compareTo(Object o) {
    if (o==this) return 0;
    PairObjDouble c = (PairObjDouble) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    //if (_val < c._val) return -1;
    //else if (_val == c._val) return 0;
    //else return 1;
    return Double.compare(_val, c._val);
  }
}
