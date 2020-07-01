package popt4jlib.GradientDescent;

import popt4jlib.LocalOptimizerIntf;
import java.util.*;
import utils.*;
import analysis.*;
import popt4jlib.*;

/**
 * Steepest Descent using Armijo rule for step-size determination.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ArmijoSteepestDescent implements LocalOptimizerIntf {
  HashMap _params;
  private double _incValue=Double.MAX_VALUE;
  private VectorIntf _inc=null;  // incumbent vector
  FunctionIntf _f;
  private transient ASDThread[] _threads=null;
  private int _numOK=0;
  private int _numFailed=0;


  /**
   * public no-arg constructor
   */
  public ArmijoSteepestDescent() {
  }


  /**
   * public constructor. The parameters passed in the argument are copied
   * in the data member _params so that later modifications to the argument
   * do not affect this object or its methods.
   * @param params HashMap
   */
  public ArmijoSteepestDescent(HashMap params) {
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
   * <li> &lt;"asd.numthreads", Integer nt&gt; optional, the number of threads 
	 * to use in the optimization process. Default is 1.
	 * <li>&lt;"asd.numtries", Integer ntries&gt; optional, the number of initial 
	 * starting points to use (must either exist then ntries 
	 * &lt;"asd.x$i$",VectorIntf v&gt; pairs in the parameters or a pair 
	 * &lt;"[gradientdescent.]x0",VectorIntf v&gt; pair in params). Default is 1.
   * <li> &lt;"asd.gradient", VecFunctionIntf g&gt; optional, the gradient of f,
   * the function to be minimized. If this param-value pair does not exist, the
   * gradient will be computed using Richardson finite differences extrapolation
   * <li> &lt;"asd.gtol", Double v&gt; optional, the minimum absolute value for 
	 * each of the gradient's coordinates, below which if all coordinates of the 
	 * gradient happen to be, the search stops assuming it has reached a 
	 * stationary point. Default is 1.e-6.
   * <li> &lt;"asd.maxiters", Integer miters&gt; optional, the maximum number of 
	 * major iterations of the SD search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
   * <li> &lt;"asd.rho", Double v&gt; optional, the value for the parameter ñ in 
	 * the Armijo rule implementation. Default is 0.1.
   * <li> &lt;"asd.beta", Double v&gt; optional, the value for the parameter â 
	 * in the Armijo rule implementation. Default is 0.8.
   * <li> &lt;"asd.gamma", Double v&gt; optional, the value for the parameter ã 
	 * in the Armijo rule implementation. Default is 1.
   * <li> &lt;"asd.looptol", Double v&gt; optional, the minimum step-size 
	 * allowed. Default is 1.e-21.
   * </ul>
   * @param f FunctionIntf the function to minimize
   * @throws OptimizerException if another thread is currently executing the
   * same method or if the method fails to find a minimizer
   * @return PairObjDouble the pair containing the arg. min (a VectorIntf) and
   * the min. value found
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) 
			throw new OptimizerException("ArmijoSteepestDescent.minimize(f): null f");
    try {
      synchronized (this) {
        if (_f != null)
          throw new OptimizerException("ASD.minimize(): " +
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
        Integer ntI = (Integer) _params.get("asd.numthreads");
        if (ntI != null && ntI.intValue() > 1) numthreads = ntI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      _threads = new ASDThread[numthreads];
      int ntries = 1;
      try {
        Integer ntriesI = (Integer) _params.get("asd.numtries");
        if (ntriesI != null && ntriesI.intValue() > 1) 
					ntries = ntriesI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      int triesperthread = ntries / numthreads;
      int rem = ntries;
      for (int i = 0; i < numthreads - 1; i++) {
        _threads[i] = new ASDThread(i, triesperthread, this);
        rem -= triesperthread;
      }
      _threads[numthreads - 1] = new ASDThread(numthreads - 1, rem, this);
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
      Messenger.getInstance().msg("update incumbent w/ new best value="+val,0);
      if (Debug.debug(Constants.ASD)!=0) {
        // sanity check
        double incval = _f.eval(arg, _params);
        if (Math.abs(incval - _incValue) > 1.e-25) {
          Messenger.getInstance().msg("ASD.setIncumbent(): arg-val originally="+
                                      _incValue + " fval=" + incval + " ???", 
						                          0);
          throw new OptimizerException(
              "ASD.setIncumbent(): insanity detected; " +
              "most likely evaluation function is " +
              "NOT reentrant... " +
              "Add the 'function.notreentrant,num'" +
              " pair (num=1 or 2) to run parameters");
        }
        // end sanity check
      }

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

  // nested class implementing the threads to be used by ArmijoSteepestDescent
  // main class
  class ASDThread extends Thread {

    private ArmijoSteepestDescent _master;
    private int _numtries;
    private int _id;
    private int _uid;

    public ASDThread(int id, int numtries, ArmijoSteepestDescent master) {
      _id = id;
      _uid = (int) DataMgr.getUniqueId();
      _master=master;
      _numtries=numtries;
    }

    public void run() {
      HashMap p = _master.getParams();  // was new HashMap(_master._params);
      p.put("thread.localid", new Integer(_id));
      p.put("thread.id", new Integer(_uid));  // used to be _id
      VectorIntf best = null;
      double bestval = Double.MAX_VALUE;
      FunctionIntf f = _master.getFunction();  // was _master._f;
      for (int i=0; i<_numtries; i++) {
        try {
          int index = _id*_numtries+i;  // this is the starting point soln index
          PairObjDouble pair = min(f, index, p);
          double val = pair.getDouble();
          if (val<bestval) {
            bestval=val;
            best=(VectorIntf) pair.getArg();
          }
          _master.incOK();
        }
        catch (Exception e) {
          _master.incFailed();
          e.printStackTrace();
          // no-op
        }
      }
      try {
        _master.setIncumbent(best, bestval);
      }
      catch (OptimizerException e) {
        e.printStackTrace();
      }
    }


    private PairObjDouble min(FunctionIntf f, int solindex, HashMap p) 
			throws OptimizerException {
      VecFunctionIntf grad = (VecFunctionIntf) p.get("asd.gradient");
      if (grad==null) 
				grad = new GradApproximator(f);  // default: numeric grad computation
    final VectorIntf x0 = 
			_params.containsKey("asd.x"+solindex)==false ?
         _params.containsKey("gradientdescent.x0") ? 
			    (VectorIntf) _params.get("gradientdescent.x0") : 
			      _params.containsKey("x0") ? (VectorIntf) _params.get("x0") : null 
            // attempt to retrieve generic point
			: (VectorIntf) _params.get("asd.x"+solindex);
      if (x0==null) 
				throw new OptimizerException("no asd.x"+solindex+
					                           " initial point in _params passed");
      VectorIntf x = x0.newInstance();  // don't modify the initial soln
      final int n = x.getNumCoords();
      double gtol = 1e-6;
      Double gtolD = (Double) p.get("asd.gtol");
      if (gtolD!=null && gtolD.doubleValue()>0) gtol = gtolD.doubleValue();
      double h=0;
      double rho = 0.1;
      Double rD = (Double) p.get("asd.rho");
      if (rD!=null && rD.doubleValue()>0)
        rho = rD.doubleValue();
      double beta = 0.8;
      Double bD = (Double) p.get("asd.beta");
      if (bD!=null && bD.doubleValue()>0 && bD.doubleValue()<1)
        beta = bD.doubleValue();
      double gamma = 1.0;
      Double gD = (Double) p.get("asd.gamma");
      if (gD!=null && gD.doubleValue()>0)
        gamma = gD.doubleValue();
      double fx = Double.NaN;
      int maxiters = Integer.MAX_VALUE;
      Integer miI = (Integer) p.get("asd.maxiters");
      if (miI!=null && miI.intValue()>0)
        maxiters = miI.intValue();
      double looptol = 1.e-21;
      Double ltD = (Double) p.get("asd.looptol");
      if (ltD!=null && ltD.doubleValue() > 0) looptol = ltD.doubleValue();
      boolean found=false;

      // main iteration loop
      VectorIntf g = grad.eval(x, p);
      double normg = VecUtil.norm(g,2);
			Messenger.getInstance().msg("ASDThread.min(): normg="+normg,2);
      fx = f.eval(x, p);  // was _master._f
			double f_best = fx;  // maintain best fval found so far
      VectorIntf s = g.newCopyMultBy(-1.0/normg);  // normalize s
      double[] xa = new double[n];
			final Messenger mger = Messenger.getInstance();
      for (int iter=0; iter<maxiters; iter++) {
	      mger.msg("ASDThread.min(): #iters="+iter,2);
				h=0;
        final double norminfg = VecUtil.normInfinity(g);
        if (norminfg <= gtol) {
          mger.msg("ASDThread.min(): found sol w/ norminfg="+
						       norminfg+" in "+iter+" iterations.",0);
          found = true;
          break;
        }
        for (int i=0; i<n; i++) xa[i] = x.getCoord(i);
        // Armijo Rule implementation
        // determine step-size h
        double rprev = -rho*gamma*normg;
        int m=0;
        double fval=fx;
        while (Double.compare(rprev, -looptol)<0) {
          for (int i=0; i<n; i++) {
            try {
              x.setCoord(i, xa[i] + Math.pow(beta, m) * gamma * s.getCoord(i));
            }
            catch (parallel.ParallelException e) {  // can never get here
              e.printStackTrace();
            }
          }
          fval = f.eval(x,p);  // was _master._f
					if (Double.compare(fval, f_best)<0) {
						f_best = fval;
						mger.msg("ASDThread.min(): found better fval="+f_best,2);
					}
					if (Double.isNaN(fval) || Double.isInfinite(fval) || 
							fval==Double.MAX_VALUE) {
						throw new OptimizerException("ASD evaluates function to "+
							                           "NaN/Infinity/MAX_VALUE, aborting...");
					}
					double fxprprev = fx + rprev;
					mger.msg("ASDThread.min(): in step-size determination fval="+fval+
							     " fx+rprev="+fxprprev+" cond = " + (fval<=fxprprev),3);
          if (Double.compare(fval,fxprprev)<=0) {
            h = Math.pow(beta,m)*gamma;
            break;
          }
          rprev *= beta;
          m++;
        }
        if (h<=0) 
					throw new OptimizerException("ASD could not find a valid h from x="+
                                       x+" after "+m+" iterations...");
        // set new x, fx, g, normg, s
        for (int i=0; i<n; i++) {
          try {
            x.setCoord(i, xa[i] + h * s.getCoord(i));
          }
          catch (parallel.ParallelException e) {  // can never get here
            e.printStackTrace();
          }
        }
        fx = fval;
        g = grad.eval(x,p);
        normg = VecUtil.norm2(g);
        for (int i=0; i<n; i++) {
          try {
            s.setCoord(i, -g.getCoord(i) / normg); // what if normg==0 ?
          }
          catch (parallel.ParallelException e) {  // can never get here
            e.printStackTrace();
          }
        }
      }
      // end main iteration loop

      if (found) return new PairObjDouble(x, fx);
      else throw new OptimizerException("ASDThread did not find a solution"+
                                        " satisfying tolerance criteria from "+
                                        "the given initial point x0="+x0);
    }

  }
}

