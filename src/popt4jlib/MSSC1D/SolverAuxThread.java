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
public class SolverAuxThread extends Thread {
  private SolverAux _r = null;


  public SolverAuxThread(SolverAux r) {
    _r = r;
  }


  public void run() {
    _r.go();
  }


  public SolverAux getSolverAux() {
    return _r;
  }

}
