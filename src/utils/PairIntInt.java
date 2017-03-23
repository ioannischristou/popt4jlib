package utils;

import java.io.Serializable;


/**
 * utility class with the same semantics as PairIntDouble, but holds two ints.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PairIntInt implements Comparable, Serializable {
	// private final static long serialVersionUID=...L;
  private int _first;
  private int _second;

	
  /**
   * public constructor.
   * @param f int
   * @param s int
   */
  public PairIntInt(int f, int s) {
    _first = f;  _second = s;
  }


  /**
   * return the first int.
   * @return int
   */
  public int getFirst() {
    return _first;
  }


  /**
   * return the second int.
   * @return int
   */
  public int getSecond() {
    return _second;
  }


  /**
   * comparison based on the value of both ints that are held by this class.
   * @param o Object
   * @return boolean
   */
  public boolean equals(Object o) {
    if (o==this) return true;
    if (o==null) return false;
    try {
      PairIntInt dd = (PairIntInt) o;
      return _first==dd._first && _second == dd._second;
    }
    catch (ClassCastException e) {
      return false;
    }
  }


  /**
   * return the sum of the two ints held by this object.
   * @return int
   */
  public int hashCode() {
    return _first+_second;
  }


  /**
   * compare first the _first, then the _second.
   * @param o Object
   * @return int
   */
  public int compareTo(Object o) {
    if (o==this) return 0;
    PairIntInt c = (PairIntInt) o;
		int fc = Integer.compare(_first, c._first);
		return fc==0 ? Integer.compare(_second, c._second) : fc;
  }
}
