package popt4jlib.MSSC;

import popt4jlib.VectorIntf;
import popt4jlib.GradientDescent.VecUtil;
import java.util.Vector;
import java.util.List;


/**
 * specifies that a clustering process must stop when two successive calls of
 * isDone() the centers found by the associated clusterer have not moved at all.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ClustererTerminationNoCenterMove implements ClustererTerminationIntf {
  private ClustererIntf _cl;
  private List _oldcenters;
  private int _totIters = 0;
  final static private double _c = 1e-7;
  final static private int _maxIters = 50;

  public ClustererTerminationNoCenterMove() {
  }


  public void registerClustering(ClustererIntf cl) {
    _cl = cl;
    _oldcenters = null;
    _totIters = 0;
  }


  public boolean isDone() {
    _totIters++;
    boolean result = true;
    // figure out if _oldcenters is the same as new centers
    try {
      if (_oldcenters != null) {
        for (int i = 0; i < _oldcenters.size() && result; i++) {
          VectorIntf ci = (VectorIntf) _cl.getCurrentCenters().get(i);
          VectorIntf oci = (VectorIntf) _oldcenters.get(i);
          //double dist = Document.d(ci, oci);
          double dist = VecUtil.norm2(VecUtil.subtract(ci, oci));
          if (dist>_c) result = false;
        }
      }
      if (_oldcenters==null) {
        result = false;
      }
      if (_cl.getCurrentCenters()!=null) {
        _oldcenters = new Vector(_cl.getCurrentCenters());
      }
      // stop after max_iters
      if (_totIters>=_maxIters) result = true;
      if (result)
        utils.Messenger.getInstance().msg("ClustererTerminationNoCenterMove: done after "+
                                          _totIters+" iterations.",0);
      return result;
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

}

