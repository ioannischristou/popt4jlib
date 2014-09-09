package popt4jlib.MSSC;

/**
 * specifies that the clustering process will stop after a fixed number of
 * calls to the <CODE>isDone()</CODE> method.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ClustererTerminationNumIters implements ClustererTerminationIntf {
  private int _numItersLeft;
  private int _initialNumIters;


  /**
   * sole public constructor's argument specifies the total number of calls to
   * the <CODE>isDone()</CODE> before it returns true.
   * @param totalItersAllowed int
   */
  public ClustererTerminationNumIters(int totalItersAllowed) {
    _numItersLeft = totalItersAllowed;
    _initialNumIters= totalItersAllowed;
  }


  /**
   * will return true after it has been called exactly as many times as the
   * argument in the constructor of this object.
   * @return boolean
   */
  public boolean isDone() {
    if (_numItersLeft-- <= 0) return true;
    return false;
  }


  /**
   * register with a particular clusterer.
   * @param p ClustererIntf
   */
  public void registerClustering(ClustererIntf p) {
    _numItersLeft = _initialNumIters;  // reset iterations left
  }
}

