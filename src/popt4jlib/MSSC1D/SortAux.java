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
      if (Double.compare(_v, s._v)==0 && _i==s._i) return true;
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
    int v = Double.compare(_v, c._v);
    if (v!=0) return v;
    // else return Integer.compare(_i, c._i);
    else {
      if (_i < c._i) return -1;
      else if (_i == c._i) return 0;
      else return 1;
    }
  }
}
