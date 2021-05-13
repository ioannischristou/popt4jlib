package popt4jlib.GradientDescent;

import popt4jlib.LocalOptimizerIntf;
import java.util.*;
import utils.*;
import analysis.*;
import popt4jlib.*;

/**
 * Accelerated Gradient Descent method using the notion of "momentum" to avoid
 * oscillations that plague steepest descent after the initial stage. Works 
 * with guarantee when the objective function is an L-smooth function.
 * See: Z. Lin et al: "Accelerated Optimization for Machine Learning: First 
 * Order Algorithms", Springer, 2020.
 * <p>Notes:
 * <ul>
 * <li>2021-05-08: ensured all exceptions thrown during function evaluations are
 * handled properly.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AcceleratedGradientDescent implements LocalOptimizerIntf {
  HashMap _params;
  private double _incValue=Double.MAX_VALUE;
  private VectorIntf _inc=null;  // incumbent vector
  FunctionIntf _f;
  private transient AGDThread[] _threads=null;
  private int _numOK=0;
  private int _numFailed=0;


  /**
   * public no-arg constructor
   */
  public AcceleratedGradientDescent() {
  }


  /**
   * public constructor. The parameters passed in the argument are copied
   * in the data member _params so that later modifications to the argument
   * do not affect this object or its methods.
   * @param params HashMap
   */
  public AcceleratedGradientDescent(HashMap params) {
    try {
      setParams(params);
    }
    catch (Exception e) {
      // no-op: cannot reach this point
    }
  }


  /**
   * return a copy of the parameters. Modifications to the returned object
   * do not affect the data member.
   * @return HashMap
   */
  public synchronized HashMap getParams() {
    return new HashMap(_params);
  }


  /**
   * return a new empty ArmijoSteepestDescent optimizer object (that must be
   * configured via a call to setParams(p) before it is used.)
   * @return LocalOptimizerIntf
   */
  public LocalOptimizerIntf newInstance() {
    return new ArmijoSteepestDescent();
  }


  /**
   * the optimization params are set to a copy of p
   * @param p HashMap
   * @throws OptimizerException if another thread is currently executing the
   * <CODE>minimize(f)</CODE> method of this object.
   */
  public synchronized void setParams(HashMap p) throws OptimizerException {
    if (_f!=null) 
			throw new OptimizerException("cannot modify parameters while running");
    _params = null;
    _params = new HashMap(p);  // own the params
  }


  /**
   * the main method of the class. Before it is called, a number of parameters
   * must have been set (via the parameters passed in the constructor, or via
   * a later call to setParams(p). These are:
	 * <ul>
   * <li> &lt;"agd.numthreads", Integer nt&gt; optional, the number of threads 
	 * to use in the optimization process. Default is 1.
	 * <li>&lt;"agd.numtries", Integer ntries&gt; optional, the number of initial 
	 * starting points to use (must either exist then ntries 
	 * &lt;"agd.x$i$",VectorIntf v&gt; pairs in the parameters or a pair 
	 * &lt;"[gradientdescent.]x0",VectorIntf v&gt; pair in params). Default is 1.
   * <li> &lt;"agd.gradient", VecFunctionIntf g&gt; optional, the gradient of f,
   * the function to be minimized. If this param-value pair does not exist, the
   * gradient will be computed using Richardson finite differences extrapolation
   * <li> &lt;"agd.gtol", Double v&gt; optional, the minimum abs. value for each 
	 * of the gradient's coordinates, below which if all coordinates of the 
	 * gradient happen to be, the search stops assuming it has reached a 
	 * stationary point. Default is 1.e-6.
   * <li> &lt;"agd.maxiters", Integer miters&gt; optional, the maximum number of 
	 * major iterations of the AGD search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
	 * <li> &lt;"agd.L", double v&gt; optional, the L constant that is supposed to 
	 * be the L-smoothness factor of the objective function. Default is 1.0.
   * </ul>
   * @param f FunctionIntf the function to minimize
   * @throws OptimizerException if another thread is currently executing the
   * same method or if the method fails to find a minimizer
   * @return PairObjDouble the pair containing the arg. min (a VectorIntf) and
   * the min. value found
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) 
			throw new OptimizerException("AcceleratedGradientDescent.minimize(f): "+
				                           "null f");
    try {
      synchronized (this) {
        if (_f != null)
          throw new OptimizerException("AGD.minimize(): " +
                                       "another thread is concurrently "+
						                           "executing the method on this object");
        _f = f;
        _numOK=0;
        _numFailed=0;
        _inc = null;
        _incValue = Double.MAX_VALUE;  // reset
      }
      int numthreads = 1;
      try {
        Integer ntI = (Integer) _params.get("agd.numthreads");
        if (ntI != null && ntI.intValue() > 1) numthreads = ntI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      _threads = new AGDThread[numthreads];
      int ntries = 1;
      try {
        Integer ntriesI = (Integer) _params.get("agd.numtries");
        if (ntriesI != null && ntriesI.intValue() > 1) 
					ntries = ntriesI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      int triesperthread = ntries / numthreads;
      int rem = ntries;
      for (int i = 0; i < numthreads - 1; i++) {
        _threads[i] = new AGDThread(i, triesperthread);
        rem -= triesperthread;
      }
      _threads[numthreads - 1] = new AGDThread(numthreads - 1, rem);
      for (int i = 0; i < numthreads; i++) {
        _threads[i].start();
      }
      // wait until all threads finish
      for (int i=0; i<numthreads; i++) {
        try {
          _threads[i].join();
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt(); // recommended action
        }
      }
      synchronized (this) {  // keep FindBugs happy
        if (_inc == null) // didn't find a solution
          throw new OptimizerException("failed to find solution");
        // ok, we're done
        PairObjDouble pr = new PairObjDouble(_inc, _incValue);
        return pr;
      }
    }
    finally {
      synchronized (this) {
        _f = null;
      }
    }
  }


  /**
   * auxiliary method.
   * @return int the number of "tries" that succeeded (found a stationary point)
   */
  public synchronized int getNumOK() { return _numOK; }
  /**
   * auxiliary method.
   * @return int the number of tries that failed (did not find a saddle point)
   */
  public synchronized int getNumFailed() { return _numFailed; }


  /**
   * set the incumbent solution -if it is better than the current incumbent.
   * @param arg VectorIntf proposed arg.
   * @param val double proposed value
   */
  synchronized void setIncumbent(VectorIntf arg, double val) {
    Messenger mger = Messenger.getInstance();
		if (val<_incValue) {
      mger.msg("update incumbent w/ new best value="+val,0);
      _incValue=val;
      _inc=arg;
    }
  }


  /**
   * introduced to keep FindBugs happy...
   * @return FunctionIntf
   */
  synchronized FunctionIntf getFunction() { return _f; }


  /**
   * auxiliary method
   */
  synchronized void incOK() { _numOK++; }


  /**
   * auxiliary method
   */
  synchronized void incFailed() { _numFailed++; }

  /** 
	 * nested class implementing the threads to be used by ArmijoSteepestDescent 
	 * main class
	 */ 
  class AGDThread extends Thread {

    private int _numtries;
    private int _id;
    private int _uid;

    public AGDThread(int id, int numtries) {
      _id = id;
      _uid = (int) DataMgr.getUniqueId();
      _numtries=numtries;
    }

    public void run() {
      HashMap p = getParams();  // was new HashMap(_master._params);
      p.put("thread.localid", new Integer(_id));
      p.put("thread.id", new Integer(_uid));  // used to be _id
      VectorIntf best = null;
      double bestval = Double.MAX_VALUE;
      FunctionIntf f = getFunction();  // was _master._f;
      for (int i=0; i<_numtries; i++) {
        try {
          int index = _id*_numtries+i;  // this is the starting point soln index
          PairObjDouble pair = min(f, index, p);
          double val = pair.getDouble();
          if (val<bestval) {
            bestval=val;
            best=(VectorIntf) pair.getArg();
          }
          incOK();
        }
        catch (Exception e) {
          incFailed();
          e.printStackTrace();
          // no-op
        }
      }
      setIncumbent(best, bestval);
    }


    private PairObjDouble min(FunctionIntf f, int solindex, HashMap p) 
			throws OptimizerException {
			Messenger mger = Messenger.getInstance();
			double L = 1.0;
			if (p.containsKey("agd.L")) {
				L = ((Double)p.get("agd.L")).doubleValue();
			}
			double L_inv = 1.0/L;
      VecFunctionIntf grad = (VecFunctionIntf) p.get("agd.gradient");
      if (grad==null) grad = new GradApproximator(f);  // default: numeric 
			                                                 // gradient computation
			final VectorIntf x0 = 
				p.containsKey("agd.x"+solindex)==false ?
					p.containsKey("gradientdescent.x0") ? 
						(VectorIntf) p.get("gradientdescent.x0") : 
							p.containsKey("x0") ? (VectorIntf) p.get("x0") : null 
							// attempt to retrieve generic point
				  : (VectorIntf) p.get("agd.x"+solindex);
      if (x0==null) 
				throw new OptimizerException("no agd.x"+solindex+
					                           " initial point in _params passed");
      VectorIntf x = x0.newInstance();  // don't modify the initial soln
			VectorIntf xprev = x.newInstance();
      final int n = x.getNumCoords();
      double gtol = 1e-6;
      Double gtolD = (Double) p.get("agd.gtol");
      if (gtolD!=null && gtolD.doubleValue()>=0) gtol = gtolD.doubleValue();
			
			double thetaprev = 1.0;
			double theta = 1.0;
			double theta2 = 1.0;
			double beta = 0.0;
      double fx = Double.NaN;
      int maxiters = Integer.MAX_VALUE;
      Integer miI = (Integer) p.get("agd.maxiters");
      if (miI!=null && miI.intValue()>0)
        maxiters = miI.intValue();
      boolean found=false;
			try {
	      fx = f.eval(x, p);
			}
			catch (Exception e) {
				throw new OptimizerException("AGD.AGDThread.min(): f.eval() threw "+
					                           e.toString());
			}
      DblArray1Vector y = new DblArray1Vector(n);

			// use these variables to update L at each iteration
			VectorIntf y_prev = y.newInstance();
			VectorIntf g_prev;
			try {
				g_prev = grad.eval(y_prev, p);
			}
			catch (Exception e) {
				throw new OptimizerException("AGD.AGDThread.min(): g.eval() threw "+
					                           e.toString());
			}
			
			// main iteration loop
			
      for (int iter=0; iter<maxiters; iter++) {
	      mger.msg("AGDThread.min(): #iters="+iter+" fx="+fx,2);
				if (Double.compare(gtol,0.0)>0) {
					VectorIntf g;
					try {
						g = grad.eval(x, p);
					}
					catch (Exception e) {
						throw new OptimizerException("AGD.AGDThread.min(): g.eval() threw "+
							                           e.toString());
					}
					double normg = VecUtil.norm(g,2);
					mger.msg("AGDThread.min(): normg="+normg,2);
					final double norminfg = VecUtil.normInfinity(g);
	        if (norminfg <= gtol) {
		        mger.msg("found sol w/ norminfg="+norminfg+" in "+
							       iter+" iterations.",
							       0);
				    return new PairObjDouble(x, fx);
					}
				}
				// set new theta, thetaprev, beta, and then y and x
				beta = theta*(1-thetaprev)/thetaprev;
				thetaprev = theta;
				theta2 = theta*theta;
				theta = (Math.sqrt(theta2*(theta2 + 4))-theta2)/2.0;
				try {
					for (int i=0; i<n; i++) {  // update y 
						double yi = (beta+1)*x.getCoord(i) - beta*xprev.getCoord(i);
						y.setCoord(i, yi);
					}
					// update x, xprev
					VectorIntf g_y = grad.eval(y, p);
					// update L, and then update g_prev and y_prev
					double g_dist = VecUtil.getEuclideanDistance(g_prev, g_y);
					double y_dist = VecUtil.getEuclideanDistance(y_prev, y);
					double L_cand = g_dist/y_dist;
					if (Double.isFinite(L_cand) && Double.compare(L_cand,L)>0) {
						mger.msg("AGDThread.min(): updating L <- "+L_cand, 1);
						L = L_cand;
						L_inv = 1.0/L;
					}
					// y_prev = y
					for (int i=0; i<n; i++) y_prev.setCoord(i, y.getCoord(i));
					// g_prev = g_y
					for (int i=0; i<n; i++) g_prev.setCoord(i, g_y.getCoord(i));
					
					y.addMul(-L_inv, g_y);
					//xprev = x;
					for (int i=0; i<n; i++) xprev.setCoord(i, x.getCoord(i));
					//x = y;
					for (int i=0; i<n; i++) x.setCoord(i, y.getCoord(i));
					fx = f.eval(x, p);
				}
				catch (Exception e) {  // ignore non-quietly
					e.printStackTrace();
				}
			}
			
      // end main iteration loop

      if (found) return new PairObjDouble(x, fx);
      else throw new OptimizerException("AGDThread did not find a solution"+
                                        " satisfying tolerance criteria from "+
                                        "the given initial point x0="+x0);
    }

  }
}

