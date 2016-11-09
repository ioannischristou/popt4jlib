package popt4jlib.MSSC1D;


/**
 * auxiliary class, not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class SolverAuxMatThread extends Thread {
  private SolverAuxMat _r = null;


  public SolverAuxMatThread(SolverAuxMat r) {
    _r = r;
  }


  public void run() {
    _r.go();
  }


  public SolverAuxMat getSolverAuxMat() {
    return _r;
  }

}
