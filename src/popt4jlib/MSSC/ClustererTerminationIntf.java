package popt4jlib.MSSC;

/**
 * Interface defines the methods that must be supported by any class that will
 * specify termination criteria for a clusterer algorithm.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface ClustererTerminationIntf {

  /**
   * return true only when the ClustererIntf object that has registered with
   * this object, satisfies the criterion specified by this object.
   * @return boolean
   */
  boolean isDone();


  /**
   * register with a specific clusterer (implementing the ClustererIntf).
   * The <CODE>clusterVectors()</CODE> method of the clusterer must then check
   * with this object's <CODE>isDone()</CODE> method to see whether the
   * clustering process can stop.
   * @param problem ClustererIntf
   */
  public void registerClustering(ClustererIntf problem);
}
