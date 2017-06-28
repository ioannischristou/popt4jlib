package popt4jlib.GradientDescent.stochastic;

import java.util.*;
import utils.*;
import analysis.*;
import parallel.*;
import popt4jlib.*;
import popt4jlib.GradientDescent.*;

/**
 * A multi-threaded implementation of the Adam algorithm for stochastic 
 * optimization. See D.P. Kingma and J.L. Ba (2015): "ADAM: A method for 
 * stochastic optimization", arXiv:1412.6980v8 [cs.LG]. The authors of the 
 * paper claim their method to be particularly well-suited for large-scale
 * online sparse learning problems. The method is supposed to work when 
 * the function to be optimized is a probability distribution function, whose
 * arguments are the parameters è of the distribution, and the objective is to
 * find those parameters that minimize the expected value E[f(è)] of this
 * distribution. 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Adam implements LocalOptimizerIntf {
  HashMap _params;
  private double _incValue=Double.MAX_VALUE;
  private VectorIntf _inc=null;  // incumbent vector
  FunctionIntf _f;
  private transient AdamThread[] _threads=null;
  private int _numOK=0;
  private int _numFailed=0;


  /**
   * public no-arg constructor
   */
  public Adam() {
  }


  /**
   * public constructor. The parameters passed in the argument are copied
   * in the data member <CODE>_params</CODE> so that later modifications to the 
	 * argument do not affect this object or its methods.
   * @param params HashMap
   */
  public Adam(HashMap params) {
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
   * return a new empty Adam optimizer object (that must be
   * configured via a call to setParams(p) before it is used.)
   * @return LocalOptimizerIntf
   */
  public LocalOptimizerIntf newInstance() {
    return new Adam();
  }


  /**
   * the optimization params are set to a copy of p.
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
   * <li> &lt;"adam.numthreads", Integer nt&gt; optional, the number of threads 
	 * to use in the optimization process. Default is 1.
	 * <li>&lt;"adam.numtries", Integer ntries&gt; optional, the number of initial 
	 * starting points to use (must either exist then ntries 
	 * &lt;"adam.x$i$",VectorIntf v&gt; pairs in the parameters or a pair 
	 * &lt;"[gradientdescent.]x0",VectorIntf v&gt; pair in params). Default is 1.
   * <li> &lt;"adam.gradient", VecFunctionIntf g&gt; optional the gradient of f,
   * the function to be minimized. If this param-value pair does not exist, the
   * gradient will be computed via Richardson finite differences extrapolation.
   * <li> &lt;"adam.gtol", Double v&gt; optional, the minimum abs. value for 
	 * each of the gradient's coordinates, below which if all coordinates of the 
	 * gradient happen to be, the search stops assuming it has reached a 
	 * stationary point. Default is 1.e-6.
   * <li> &lt;"adam.maxiters", Integer miters&gt; optional, the maximum number 
	 * of major iterations of the search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
	 * <li> &lt;"adam.a", Double val&gt; optional, the alpha step-size parameter 
	 * of the Adam algorithm. Default is 0.001.
	 * <li> &lt;"adam.b1", Double val&gt; optional the &beta_1 exponential decay
	 * rate for moment estimates. Default is 0.9.
	 * <li> &lt;"adam.b2", Double val&gt; optional the &beta_2 exponential decay
	 * rate for moment estimates. Default is 0.999.
	 * <li> &lt;"adam.eps", Double val&gt; optional the &epsilon factor in update
	 * parameter estimation. Default is 1.e-8.
   * </ul>
   * @param f FunctionIntf the function to minimize
   * @throws OptimizerException if another thread is currently executing the
   * same method or if the method fails to find a minimizer
   * @return PairObjDouble the pair containing the arg. min (a VectorIntf) and
   * the min. value found
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) 
			throw new OptimizerException("Adam.minimize(f): null f");
    try {
      synchronized (this) {
        if (_f != null)
          throw new OptimizerException("Adam.minimize(): " +
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
        Integer ntI = (Integer) _params.get("adam.numthreads");
        if (ntI != null && ntI.intValue() > 1) numthreads = ntI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      _threads = new AdamThread[numthreads];
      int ntries = 1;
      try {
        Integer ntriesI = (Integer) _params.get("asd.numtries");
        if (ntriesI != null && ntriesI.intValue() > 1) ntries = ntriesI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      int triesperthread = ntries / numthreads;
      int rem = ntries;
      for (int i = 0; i < numthreads - 1; i++) {
        _threads[i] = new AdamThread(i, triesperthread);
        rem -= triesperthread;
      }
      _threads[numthreads - 1] = new AdamThread(numthreads - 1, rem);
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
   * @throws OptimizerException in case of insanity (may only happen if the
   * function to be minimized is not reentrant and the debug bit
   * <CODE>Constants.ASD</CODE> is set in the <CODE>Debug</CODE> class)
   */
  synchronized void setIncumbent(VectorIntf arg, double val) 
		throws OptimizerException {
    if (val<_incValue) {
      Messenger.getInstance().msg("Adam: update incumbent w/ new best value="+
				                          val,0);
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

  // nested class implementing the threads to be used by Adam main class
  class AdamThread extends Thread {

    private int _numtries;
    private int _id;
    private int _uid;

    AdamThread(int id, int numtries) {
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
      try {
        setIncumbent(best, bestval);
      }
      catch (OptimizerException e) {
        e.printStackTrace();
      }
    }


    private PairObjDouble min(FunctionIntf f, int solindex, HashMap p) 
			throws OptimizerException {
      VecFunctionIntf grad = (VecFunctionIntf) p.get("adam.gradient");
      if (grad==null) grad = new GradApproximator(f);  
      // default: numeric computation of gradient
      final VectorIntf x0 = 
			  _params.containsKey("adam.x"+solindex)==false ?
          _params.containsKey("gradientdescent.x0") ? 
			    (VectorIntf) _params.get("gradientdescent.x0") : 
			      _params.containsKey("x0") ? 
				      (VectorIntf) _params.get("x0") : null // retrieve generic point?
			    : (VectorIntf) _params.get("adam.x"+solindex);
      if (x0==null) 
				throw new OptimizerException("no adam.x"+solindex+
					                           " initial point in _params passed");
      VectorIntf x = x0.newInstance();  // don't modify the initial soln
      final int n = x.getNumCoords();
      double gtol = 1e-6;
      Double gtolD = (Double) p.get("adam.gtol");
      if (gtolD!=null && gtolD.doubleValue()>0) gtol = gtolD.doubleValue();
      double a = 0.001;
      Double aD = (Double) p.get("adam.a");
      if (aD!=null && aD.doubleValue()>0)
        a = aD.doubleValue();
      double b1 = 0.9;
      Double b1D = (Double) p.get("adam.b1");
      if (b1D!=null && b1D.doubleValue()>0 && b1D.doubleValue()<1.0)
        b1 = b1D.doubleValue();
			final double b1_inv = 1.0/b1;
			final double one_minus_b1 = 1.0 - b1; 
      double b2 = 0.999;
      Double b2D = (Double) p.get("adam.b2");
      if (b2D!=null && b2D.doubleValue()>0 && b2D.doubleValue()<=1.0)
        b2 = b2D.doubleValue();
			final double b2_inv = 1.0/b2;
			final double one_minus_b2 = 1.0 - b2;
			double eps = 1e-8;
			Double eD = (Double) p.get("adam.eps");
			if (eD!=null && eD.doubleValue()>0) eps = eD.doubleValue();
      double fx = Double.NaN;
      int maxiters = Integer.MAX_VALUE;
      Integer miI = (Integer) p.get("adam.maxiters");
      if (miI!=null && miI.intValue()>0)
        maxiters = miI.intValue();
      boolean found=false;

      // main iteration loop
      double[] xa = new double[n];
			VectorIntf m = x0.newInstance(xa);  // first-moment estimate
			VectorIntf v = m.newInstance();  // second-moment estimate
			VectorIntf mhat = m.newInstance();
			VectorIntf vhat = m.newInstance();
			double b1_t = b1;
			double b2_t = b2;
      for (int iter=0; iter<maxiters; iter++) {
	      Messenger.getInstance().msg("AdamThread.min(): #iters="+iter,2);
				VectorIntf g = grad.eval(x, p);
		    double normg = VecUtil.norm(g,2);
				Messenger.getInstance().msg("AdamThread.min(): normg="+normg,2);
				VectorIntf g_squared = VecUtil.componentProduct(g, g);				
				fx = f.eval(x, p);
				final double norminfg = VecUtil.normInfinity(g);
        if (norminfg <= gtol) {
          Messenger.getInstance().msg("found sol w/ norminfg="+norminfg+
						                          " in "+iter+" iterations.",0);
          found = true;
          break;
        }
        for (int i=0; i<n; i++) xa[i] = x.getCoord(i);
        // Adam algorithm essense:
        // update formulas
				try {
					m.div(b1_inv);
					m.addMul(one_minus_b1, g);
					v.div(b2_inv);
					v.addMul(one_minus_b2, g_squared);
					b1_t *= b1;
					b2_t *= b2;
					mhat.div(1.0-b1_t);
					vhat.div(1.0-b2_t);
					// update x
					for (int i=0; i<n; i++) {
						double vi = x.getCoord(i) - 
							          a*mhat.getCoord(i)/(eps+Math.sqrt(vhat.getCoord(i)));
						x.setCoord(i, vi);
					}
				}
				catch (ParallelException e) {  // cannot get here
					e.printStackTrace();
					throw new Error("insanity");
				}				
      }
      // end main iteration loop

      if (found) return new PairObjDouble(x, fx);
      else throw new OptimizerException("AdamThread did not find a solution"+
                                        " satisfying tolerance criteria from "+
                                        "the given initial point x0="+x0);
    }

  }
}

