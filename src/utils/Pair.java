package utils;

/**
 * utility class holding pairs of objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Pair {
  private Object _first;
  private Object _second;


  /**
   * public constructor.
   * @param first Object
   * @param second Object
   */
  public Pair(Object first, Object second) {
    _first = first;
    _second = second;
  }

  /**
   * get the first Object in this Pair
   * @return Object the first object in the Pair
   */
  public Object getFirst() { return _first; }


  /**
   * set the value of the first object of this Pair
   * @param f Object set the first object of this Pair to hold a ref to f
   */
  public void setFirst(Object f) { _first = f; }


  /**
   * get the second object of this Pair
   * @return Object the second object in this Pair
   */
  public Object getSecond() { return _second; }


  /**
   * set the value of the second object in this Pair
   * @param s Object set the second object of this Pair to hold a ref to s
   */
  public void setSecond(Object s) { _second = s; }
}

