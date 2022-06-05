package popt4jlib.MSSC;

import java.util.ArrayList;
import popt4jlib.VectorIntf;
import popt4jlib.GradientDescent.VecUtil;
import java.util.List;


/**
 * specifies that a clustering process must stop when two successive calls of
 * <CODE>isDone()</CODE> the centers found by the associated clusterer have not 
 * moved at all. Notice that as a safe-guard, there is still a limit on the 
 * maximum number of iterations allowed when the no-arg constructor is used, 
 * currently set to 50 (in this case, the "no center move" actually allows a
 * "movement" of any center to a new position within the circle with center the
 * previous center position and radius <CODE>1.E-7</CODE>.
 * <p>Note:
 * <ul>
 * <li>20181105: added one-arg constructor to allow for the centers to move 
 * slightly from their previous position, and still satisfy the criteria for 
 * "no movement".
 * <li>20220221: modified the functionality to stop after <CODE>_maxIters</CODE>
 * so that when a min distance is specified, the iterations no longer matter.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2022</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.2
 */
public class ClustererTerminationNoCenterMove 
  implements ClustererTerminationIntf {
  private ClustererIntf _cl;
  private List _oldcenters;
  private int _totIters = 0;
  private double _c = 1e-7;
  final private int _maxIters;


	/**
	 * no-arg constructor retains the default radius of centers that are allowed
	 * to move and still be considered as no-move, 1E-7.
	 */
  public ClustererTerminationNoCenterMove() {
		_maxIters = 50;
  }

	
	/**
	 * constructor sets the distance allowed for centers to move from iteration
	 * to iteration and still be considered as no-move.
	 * @param c double
	 */
  public ClustererTerminationNoCenterMove(double c) {
		_c = c;
		_maxIters = Integer.MAX_VALUE;
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
          //double dist = VecUtil.norm2(VecUtil.subtract(ci, oci));
					double dist = VecUtil.getEuclideanDistance(ci, oci);
          if (dist>_c) result = false;
        }
      }
      if (_oldcenters==null) {
        result = false;
      }
      if (_cl.getCurrentCenters()!=null) {
        _oldcenters = new ArrayList();
				List cur_centers = _cl.getCurrentCenters();
				final int cl_sz = cur_centers.size();
				for (int i=0; i<cl_sz; i++) {
					_oldcenters.add(((VectorIntf)cur_centers.get(i)).newInstance());
				}
      }
      // stop after max_iters
      if (_totIters>=_maxIters) result = true;
      if (result)
        utils.Messenger.getInstance().msg("ClustererTerminationNoCenterMove: "+
					                                "done after "+_totIters+" iterations",
					                                0);
      return result;
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

}

