package graph.packing;

import java.io.*;

/**
 * comparator for DBBNode objects. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface DBBNodeComparatorIntf extends Serializable {
  public int compare(DBBNodeBase o1, DBBNodeBase o2);
}
