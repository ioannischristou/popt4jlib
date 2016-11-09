package graph;

/**
 * The class represents weighted links and is used in the graph.coarsening 
 * package classes.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LinkPair {
  private int _start;
  private int _end;
  private double _w;

	/**
	 * sole public constructor.
	 * @param start int start of link.
	 * @param end int end of link.
	 * @param w double weight of the link.
	 */
  public LinkPair(int start, int end, double w) {
    _start = start; _end = end; _w = w;
  }


	/**
	 * checks for equality between link start and end points.
	 * @param o Object // LinkPair expected.
	 * @return true iff this._start==o._start &amp;&amp; this._end == o._end.
	 */
  public boolean equals(Object o) {
    if (o==null) return false;
    try {
      LinkPair l = (LinkPair) o;
      return (_start == l._start && _end == l._end);
    }
    catch (ClassCastException e) {
      return false;
    }
  }


	/**
	 * get the hash-code for this LinkPair.
	 * @return _start + _end.
	 */
  public int hashCode() {
    return _start + _end;
  }


	/**
	 * return the start of this LinkPair.
	 * @return int
	 */
  public int getStart() { return _start; }
	/**
	 * return the end of this LinkPair.
	 * @return int
	 */
  public int getEnd() { return _end; }
	/**
	 * return the weight of this LinkPair.
	 * @return double
	 */
  public double getWeight() { return _w; }


	/**
	 * adds to the existing weight of this LinkPair the value passed in.
	 * @param w double
	 * @return the new value of the weight of this LinkPair object.
	 */
  public double addWeight(double w) {
    _w += w;
    return _w;
  }
}
