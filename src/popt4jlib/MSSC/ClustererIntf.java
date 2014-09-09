package popt4jlib.MSSC;

import popt4jlib.VectorIntf;
import java.util.*;

/**
 * The interface every class implementing algorithms for MSSC clustering should
 * obey.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface ClustererIntf {
  // public Clusterer newClusterer() throws ClustererException;


  /**
   * adds a VectorIntf to the set of vectors to be later clustered.
   * @param d VectorIntf
   * @throws ClustererException
   */
  public void addVector(VectorIntf d) throws ClustererException;


  /**
   * adds a set of VectorIntf vectors to the current set of vectors to be later
   * clustered (via a call to <CODE>clusterVectors()</CODE>.
   * @param docs Vector Vector<VectorIntf>
   */
  public void addAllVectors(Vector docs);


  /**
   * sets parameters for the clusterer to use when clustering (eg the
   * termination criteria to be used).
   * @param params Hashtable
   */
  public void setParams(Hashtable params);


  /**
   * returns the set of parameters to be used in the clustering process.
   * @return Hashtable Map<String key , Object value>
   */
  public Hashtable getParams();


  /**
   * sets the initial centers to be used by the clusterer in the clustering
   * process. This call also defines the number of clusters sought.
   * @param centers Vector Vector<VectorIntf center>
   * @throws ClustererException if the argument is empty or null or contains
   * objects that are not VectorIntf objects or if the dimensions don't agree.
   */
  public void setInitialClustering(Vector centers) throws ClustererException;


  /**
   * reset for next use.
   */
  public void reset();


  /**
   * returns current centers. Maybe non responsive from other threads while the
   * clustering process is running (eg synchronized)
   * @return Vector Vector<VectorIntf center>
   */
  public Vector getCurrentCenters();

  /**
   * returns current vectors to be clustered. Maybe non responsive from other
   * threads while the clustering process is running (eg synchronized)
   * @return Vector Vector<VectorIntf doc>
   */
  public Vector getCurrentVectors();

  /**
   * cluster the document vectors and return the Vector<VectorIntf>
   * with the cluster centers.
   * @return Vector
   */
  public Vector clusterVectors() throws ClustererException;


  /**
   * return the clustering as a partition. Must be called after
   * <CODE>clusterVectors()</CODE>.
   * @return int[] an array of size=all docs entered,
   * array values in [1...centers.size()]
   * @throws ClustererException
   */
  public int[] getClusteringIndices() throws ClustererException;


  /**
   * set the ClustererIntf asgnmnt indices to the specified array.
   * Should not throw even if null array is passed in.
   * @param asgn int[]
   * @throws ClustererException
   */
  public void setClusteringIndices(int[] asgn) throws ClustererException;


  /**
   * return an array of size equal to centers.size(), showing for each
   * cluster i, the number of documents assigned to it.
   * Throws if <CODE>clusterVectors()</CODE> has not been called.
   * @throws ClustererException
   * @return int[]
   */
  public int[] getClusterCards() throws ClustererException;


  /**
   * evaluate the current clustering according to the EvaluatorIntf passed in.
   * @param ev EvaluatorIntf
   * @throws ClustererException
   * @return double
   */
  public double eval(EvaluatorIntf ev) throws ClustererException;


  /**
   * return in a Vector<Set cluster> some or all of the clusters produced
   * during the run of the clustering algorithm. May also choose to throw
   * an exception instead, so any calling code must guard for ClustererException
   * @throws ClustererException
   * @return Vector Vector<Set>
   */
  public Vector getIntermediateClusters() throws ClustererException;

}
