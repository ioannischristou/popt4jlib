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
class SolverAux {
  Params _p;
  ClusterSet _allclusters;
  double _part[][];
  ClusterSet _partcs[][];
  int _starti=-1;
  int _endi=-1;  // the indices to work on [_starti, _endi]
  int _k = -1;
  boolean _finish = false;

  public SolverAux(Params p, double part[][], ClusterSet partcs[][], ClusterSet allclusters) {
    _p = p;
    _part = part;
    _partcs = partcs;
    _allclusters = allclusters;
  }


  public void go() {
    while (getFinish()==false) {
      go1();
    }
  }


  public synchronized boolean getFinish() {
    return _finish;
  }


  public synchronized void setFinish() {
    _finish = true;
    notify();
  }


  public synchronized void waitForTask() {
    while (_starti!=-1 || _endi!=-1 || _k!=-1) {
      try {
        wait();  // wait as other operation is still running
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
  }


  public synchronized void runFromTo(int starti, int endi, int k) {
    while (_starti!=-1 || _endi!=-1 || _k!=-1) {
      try {
        wait();  // wait as other operation is still running
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
    // OK, now set values
    _starti = starti; _endi = endi; _k = k;
    notify();  // itc: HERE do I need notifyAll(); ?
  }


  private synchronized void go1() {
    while (_starti==-1 || _endi==-1 || _k==-1) {
      if (_finish) return;  // finish
      try {
        wait();  // wait for order
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
    // run the code
    try {
      final int n = _p.getSequenceLength();
      for (int i = _endi; i >= _starti; i--) {
        if (i + _k == n - 1) {
          _part[i][_k] = 0;
          // store the ClusterSet {{i}, {i+1},...,{n-1}}
          ClusterSet cs = new ClusterSet(new Integer(i));
          for (int ii = i + 1; ii <= n - 1; ii++) {
            Cluster c = new Cluster();
            c.add(new Integer(ii));
            cs.addCluster(c);
          }
          _partcs[i][_k] = cs;
        }
        else if (i + _k > n - 1) _part[i][_k] = Double.POSITIVE_INFINITY;
        else {
          // i+k<n-1
          double bv = Double.POSITIVE_INFINITY;
          int bind = -1;
          for (int j = i; j <= n - 2; j++) {
            // ++_numiters;
            double costij = _allclusters.getCluster(i, j).evaluateWF(_p);
            if (bv > costij + _part[j + 1][_k - 1]) {
              bv = costij + _part[j + 1][_k - 1];
              bind = j;
            }
          }
          _part[i][_k] = bv;
          // store partition cluster
          ClusterSet pj1k1 = _partcs[bind + 1][_k - 1];
          if (bind == -1) {
            // infeasible
            _partcs[i][_k] = null;
          }
          else if (pj1k1 == null) {
            // sanity check
            System.err.println("i=" + i + " k=" + _k + " bind+1=" + (bind + 1) +
                               " k-1=" + (_k - 1)+" part[i][k]="+_part[i][_k]+
                               " part[bind+1][k-1]="+_part[bind+1][_k-1]);
            // System.err.println("csij="+csij);
            throw new CException("Solver.solveDP2() internal error...");
          }
          else {
            ClusterSet csij = new ClusterSet(i, bind);
            csij.addClustersRight(pj1k1);
            _partcs[i][_k] = csij;
          }
        }
      }
    }
    catch (CException e) {
      e.printStackTrace();
    }
    // finished, reset indices
    _starti=-1; _endi=-1; _k = -1;
    notify();
  }
}
