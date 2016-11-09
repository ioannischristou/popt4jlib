package popt4jlib.MSSC;

import utils.Messenger;

/**
 * specifies that the clustering process may end when in two successive calls of
 * the <CODE>isDone()</CODE> method, no vector has changed its assignment to
 * another cluster.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ClustererTerminationNoMove implements ClustererTerminationIntf {
  private ClustererIntf _cl;
  private int _oldasgns[];
  private int _totIters = 0;

  public ClustererTerminationNoMove() {
  }


  public void registerClustering(ClustererIntf cl) {
    _cl = cl;
    _oldasgns = null;
    try {
      if (_cl.getClusteringIndices()!=null) {
        _oldasgns = new int[_cl.getClusteringIndices().length];
        for (int i = 0; i < _oldasgns.length; i++)
          _oldasgns[i] = _cl.getClusteringIndices()[i];
      }
    }
    catch (Exception e) {
      _oldasgns = null;
    }
    _totIters = 0;
  }


  public boolean isDone() {
    _totIters++;
    boolean result = true;
    // figure out if _oldasgns is the same as new clustering indices
    try {
      if (_oldasgns != null) {
        for (int i = 0; i < _oldasgns.length && result; i++) {
          if (_oldasgns[i] != _cl.getClusteringIndices()[i]) result = false;
        }
      }
      if (_oldasgns==null) {
        result = false;
        if (_cl.getClusteringIndices()!=null)
          _oldasgns = new int[_cl.getClusteringIndices().length];
      }
      if (_cl.getClusteringIndices()!=null) {
        for (int i = 0; i < _oldasgns.length; i++) {
          _oldasgns[i] = _cl.getClusteringIndices()[i];
        }
      }
      if (result)
        Messenger.getInstance().msg("ClustererTerminationNoMove: done after "+
                                    _totIters+" iterations.",0);
      return result;
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

}

