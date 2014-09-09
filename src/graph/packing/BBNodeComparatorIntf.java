package graph.packing;

/**
 * comparator for BBNode2 objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface BBNodeComparatorIntf {
  public int compare(BBNodeBase o1, BBNodeBase o2);
}
