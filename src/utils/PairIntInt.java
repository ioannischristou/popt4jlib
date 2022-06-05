package utils;

import java.io.Serializable;


/**
 * utility class with the same semantics as PairIntDouble, but holds two ints.
 * Notes:
 * <ul>
 * <li>20191227: modified the <CODE>hashCode()</CODE> method according to 
 * the suggestions given in "Effective Java", 2nd edition.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2019</p>
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
   * return the value recommended by Joshua Bloch.
   * @return int
   */
  public int hashCode() {
    // return _first+_second;
		int result = 17;
		// int c = (int)(_first ^ (_first >>> 32));
		// itc20211009: above is redundant and confusing for non-long fields		
		int c = _first;
		result = 31*result + c;
		// c = (int)(_second ^ (_second >>> 32));
		// itc20211009: above is redundant and confusing for non-long fields
		c = _second;
		result = 31*result + c;
		return result;		
  }


  /**
   * compare first the _first, then the _second.
   * @param o Object
   * @return int
   */
  public int compareTo(Object o) {
    if (o==this) return 0;
    PairIntInt c = (PairIntInt) o;
		int fc =  // Integer.compare(_first, c._first);
              _first < c._first ? -1 :
                                  (_first==c._first ? 0 : 1);
		//return fc==0 ? Integer.compare(_second, c._second) : fc;
    if (fc!=0) return fc;
    else {
	    int fs = _second < c._second ? -1 :
                                     (_second==c._second ? 0 : 1);
      return fs;
    }
  }
}
