package utils;

import java.util.*;
import java.io.*;


/**
 * auxiliary class allowing comparison of sets of integers.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntSet extends TreeSet implements Comparable, Serializable {
  private final static long serialVersionUID = -7487030281885541652L;

  public IntSet() {
    super();
  }


  public IntSet(Set s) {
    super(s);
  }


  public int hashCode() {
    return size();
  }


  public boolean equals(Object o) {
    if (o==null) return false;
    int n = compareTo(o);
    return (n==0);
  }


  public int compareTo(Object o) {
    if (o==null) return 1;
    TreeSet to = (TreeSet) o;
    Iterator it = iterator();
    Iterator oit = to.iterator();
    while (it.hasNext()) {
      Integer mi = (Integer) it.next();
      if (oit.hasNext()) {
        Integer oi = (Integer) oit.next();
        if (mi.intValue()<oi.intValue()) return -1;
        else if (mi.intValue()>oi.intValue()) return 1;
      }
      else return 1;
    }
    if (oit.hasNext()) return -1;
    else return 0;
  }
	
	
	public String toString() {
		String res = "[";
		Iterator it = super.iterator();
		while (it.hasNext()) {
			Integer i = (Integer) it.next();
			res += i.intValue();
			if (it.hasNext()) res += ",";
		}
		res += "]";
		return res;
	}
	
}

