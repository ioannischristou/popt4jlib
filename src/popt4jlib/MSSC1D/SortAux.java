package popt4jlib.MSSC1D;

/**
 * auxiliary class, not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class SortAux implements Comparable {
  int _i;
  double _v;


  public SortAux(int ind, double v) {
    _i = ind; _v = v;
  }


  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      SortAux s = (SortAux) o;
      if (_v==s._v) return true;
      else return false;
    }
    catch (ClassCastException e) {
      return false;
    }
  }


  public int hashCode() {
    return (int) Math.floor(_v);
  }


  public int compareTo(Object o) {
    SortAux c = (SortAux) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    return Double.compare(_v, c._v);
  }
}
