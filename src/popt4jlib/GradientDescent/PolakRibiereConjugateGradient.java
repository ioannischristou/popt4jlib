package popt4jlib.GradientDescent;

import popt4jlib.LocalOptimizerIntf;
import java.util.*;
import utils.*;
import analysis.*;
import popt4jlib.*;


/**
 * class that implements the Conjugate-Gradient method for (unconstrained)
 * nonlinear optimization using the Polak-Ribiere formula for updating b, and
 * the Armijo rule for step-size determination.
 * <p>Notes:
 * <ul>
 * <li>2021-05-08: ensured all exceptions thrown within function evaluation are
 * properly handled.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PolakRibiereConjugateGradient implements LocalOptimizerIntf {
  HashMap _params;
  private double _incValue=Double.MAX_VALUE;
  private VectorIntf _inc=null;  // incumbent vector
  FunctionIntf _f;
  private transient PRCGThread[] _threads=null;
  private int _numOK=0;
  private int _numFailed=0;


  /**
   * public no-arg constructor
   */
  public PolakRibiereConjugateGradient() {
    // no-op
  }


  /**
   * public constructor, accepting the parameters to the optimization. The
   * params are copied to a local member, so later modification of the input
   * argument does not modify the optimization parameters.
   * @param params HashMap
   */
  public PolakRibiereConjugateGradient(HashMap params) {
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
   * returns a new empty (no parameters defined) object of this class.
   * @return LocalOptimizerIntf
   */
  public LocalOptimizerIntf newInstance() {
    return new PolakRibiereConjugateGradient();
  }


  /**
   * the optimization params are set to p
   * @param p HashMap
   * @throws OptimizerException if another thread is currently running the
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
   * a later call to <CODE>setParams(p)</CODE>). These are:
   * <ul>
	 * <li>&lt;"prcg.numtries", ntries&gt; optional, the number of initial 
	 * starting points to use (must either exist then ntries 
	 * &lt;"prcg.x$i$",VectorIntf v&gt; pairs in the parameters or a pair 
	 * &lt;"[gradientdescent.]x0",VectorIntf v&gt; pair in params). Default is 1.
   * <li>&lt;prcg.numthreads", Integer nt&gt; optional, the number of threads to 
	 * use. Default is 1.
   * <li>&lt;"prcg.gradient", VecFunctionIntf g&gt; optional, the gradient of f, 
	 * the function to be minimized. If this param-value pair does not exist, the
   * gradient will be computed using Richardson finite differences extrapolation
   * <li>&lt;"prcg.gtol", Double v&gt; optional, the minimum abs. value for each 
	 * of the gradient's coordinates, below which if all coordinates of the 
	 * gradient happen to be, the search stops assuming it has reached a 
	 * stationary point. Default is 1.e-8.
   * <li>&lt;"prcg.maxiters", Integer miters&gt; optional, the maximum number of 
	 * major iterations of the CG search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
   * <li>&lt;"prcg.rho", Double v&gt; optional, the value of the parameter &rho; 
	 * in the Armijo rule. Default is 0.1.
   * <li>&lt;"prcg.beta", Double v&gt; optional, the value of the parameter 
	 * &beta; in the approximate line search step-size determination obeying the 
	 * Armijo rule conditions. Default is 0.9.
   * <li>&lt;"prcg.gamma", Double v&gt; optional, the value of the parameter 
	 * &gamma; in the approximate line search step-size determination obeying the 
	 * Armijo rule conditions. Default is 1.0.
   * <li>&lt;"prcg.looptol", Double v&gt; optional, the minimum step-size 
	 * allowed. Default is 1.e-21.
   * </ul>
   * @param f FunctionIntf the function to minimize
   * @throws OptimizerException if another thread is currently executing the
   * same method of this object or if the method fails to find a stationary
   * point, or if f is null.
   * @return PairObjDouble the pair containing the arg. min (a VectorIntf) and
   * the min. value found.
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("PRCG.minimize(f): null f");
    try {
      synchronized (this) {
        if (_f != null)
					throw new OptimizerException("PRCG.minimize(): another thread is "+
						                           "concurrently running this method on "+
						                           "the same object");
        _f = f;
        _inc = null;
        _incValue = Double.MAX_VALUE;
        _numOK = 0;
        _numFailed = 0;
      }
      int numthreads = 1;
      try {
        Integer ntI = (Integer) _params.get("prcg.numthreads");
        if (ntI != null && ntI.intValue() > 1) numthreads = ntI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      _threads = new PRCGThread[numthreads];
      int ntries = 1;
      try {
        Integer ntriesI = (Integer) _params.get("prcg.numtries");
        if (ntriesI != null && ntriesI.intValue() > 1)
          ntries = ntriesI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      int triesperthread = ntries / numthreads;
      int rem = ntries;
      for (int i = 0; i < numthreads - 1; i++) {
        _threads[i] = new PRCGThread(i, triesperthread, this);
        rem -= triesperthread;
      }
      _threads[numthreads - 1] = new PRCGThread(numthreads - 1, rem, this);
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
      synchronized (this) {
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
   * return the number of successful optimization attempts thus far
   * @return int
   */
  public synchronized int getNumOK() { return _numOK; }


  /**
   * return the number of failed optimization attempts thus far
   * @return int
   */
  public synchronized int getNumFailed() { return _numFailed; }


  /**
   * sets the passed VectorIntf arg as the incumbent solution iff its val value
   * is better than the current one so far registered
   * @param arg VectorIntf
   * @param val double
   * @throws OptimizerException if the val passed in as 2nd argument does not
   * agree with the evaluation of the function currently minimized at arg; this
   * can only happen if the function f is not reentrant (i.e. it's not thread-
   * safe). The program must be running in debug mode (the method
   * <CODE> Debug.setDebugBit(bits) </CODE> with bits equal to
   * <CODE>Constants.PRCG</CODE> or some other value containing the given bit
   * must have been called before for this method to possibly throw)
   */
  synchronized void setIncumbent(VectorIntf arg, double val) 
		throws OptimizerException {
    if (val<_incValue) {
      if (Debug.debug(Constants.PRCG)!=0) {
        // sanity check
        double incval;
				try { 
					incval = _f.eval(arg, _params);
				}
				catch (Exception e) {
					throw new OptimizerException("PRCG.setIncumbent(): f.eval() threw "+
						                           e.toString());
				}
        if (Math.abs(incval - _incValue) > 1.e-25) {
          Messenger.getInstance().msg("PRCG.setIncumbent(): arg-val "+
						                          "originally=" +_incValue + 
						                          " fval=" + incval + " ???", 0);
          throw new OptimizerException(
              "PRCG.setIncumbent(): insanity detected; " +
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

}


/**
 * auxiliary class implementing the CG method using the Polak-Ribiere formula
 * for b updates and Armijo rule for step-size determination. Not part of the
 * public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class PRCGThread extends Thread {

  private PolakRibiereConjugateGradient _master;
  private int _numtries;
  private int _id;
  private int _uid;

  public PRCGThread(int id, int numtries, 
		                PolakRibiereConjugateGradient master) {
    _id = id;
    _uid = (int) DataMgr.getUniqueId();
    _master=master;
    _numtries=numtries;
  }


  /**
   * the run() method of the thread: calls the private method min(f,index,p) to
   * compute a minimizer from the starting point x$index$ for as many index
   * values as there are tries assigned to this thread.
   */
  public void run() {
    HashMap p = _master.getParams();
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


  /**
   * the implementation of the CG method using Polak-Ribiere update for the b
   * parameter, and the Armijo rule for step-size determination.
   * @param f FunctionIntf
   * @param solindex int
   * @param p HashMap
   * @throws OptimizerException
   * @return PairObjDouble
   */
  private PairObjDouble min(FunctionIntf f, int solindex, HashMap p) 
		throws OptimizerException {
    VecFunctionIntf grad = (VecFunctionIntf) p.get("prcg.gradient");
    if (grad==null) 
			grad = new GradApproximator(f);  // default: numeric gradient computation
    final VectorIntf x0 = 
			p.containsKey("prcg.x"+solindex)==false ?
         p.containsKey("gradientdescent.x0") ? 
			    (VectorIntf) p.get("gradientdescent.x0") : 
			      p.containsKey("x0") ? (VectorIntf) p.get("x0") : null 
            // attempt to retrieve generic point
			: (VectorIntf) p.get("prcg.x"+solindex);
    if (x0==null) 
			throw new OptimizerException("no prcg.x"+solindex+
				                           " initial point in _params passed");
    VectorIntf x = x0.newInstance();  // x0.newCopy();  
                                      // don't modify the initial soln
    final int n = x.getNumCoords();
    double gtol = 1e-8;
    try {
      Double gtolD = (Double) p.get("prcg.gtol");
      if (gtolD != null && gtolD.doubleValue() > 0) gtol = gtolD.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    double h=0;
    double rho = 0.1;
    try {
      Double rD = (Double) p.get("prcg.rho");
      if (rD != null && rD.doubleValue() > 0)
        rho = rD.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    double beta = 0.8;
    try {
      Double bD = (Double) p.get("prcg.beta");
      if (bD != null && bD.doubleValue() > 0 && bD.doubleValue() < 1)
        beta = bD.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    double gamma = 1;
    try {
      Double gD = (Double) p.get("prcg.gamma");
      if (gD != null && gD.doubleValue() > 0)
        gamma = gD.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    double fx = Double.NaN;
    int maxiters = Integer.MAX_VALUE;
    try {
      Integer miI = (Integer) p.get("prcg.maxiters");
      if (miI != null && miI.intValue() > 0)
        maxiters = miI.intValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    double looptol = 1.e-21;
    try {
      Double ltD = (Double) p.get("prcg.looptol");
      if (ltD != null && ltD.doubleValue() > 0) looptol = ltD.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }

    // main loop
    boolean found=false;
    double b = 0.0;
    DblArray1Vector s = new DblArray1Vector(new double[n]);
    double[] xa = new double[n];
    for (int iter=0; iter<maxiters; iter++) {
      h=0;
      VectorIntf g;
			try {
				g = grad.eval(x, p);
				fx = f.eval(x, p);  // was _master._f
			}
			catch (Exception e) {
				throw new OptimizerException("PRCGThread.min(): f or g evaluation "+
					                           "threw "+e.toString());
			}
      final double norminfg = VecUtil.normInfinity(g);
      final double normg = VecUtil.norm(g,2);
      if (norminfg <= gtol) {
        Messenger.getInstance().msg("found sol w/ value="+fx+" normg="+norminfg+
					                          " in "+iter+" iterations.",0);
        found = true;
        break;
      }
      for (int i=0; i<n; i++) {
        xa[i] = x.getCoord(i);
        if (iter % n == 0) 
					s.setCoord(i, -g.getCoord(i));  // reset search direction
        else
          s.setCoord(i,s.getCoord(i)*b-g.getCoord(i));  // s update
      }
      double norms = VecUtil.norm2(s);
      for (int i=0; i<n; i++) 
				s.setCoord(i, s.getCoord(i)/norms);  // normalize s
      // Armijo Rule implementation
      // determine step-size h
      double sTg = VecUtil.innerProduct(s,g);
      if (sTg>=0) {  // reset search direction
        for (int i=0; i<n; i++) 
					s.setCoord(i, -g.getCoord(i)/normg);  // normalize s
        sTg = -normg;
      }
      double rprev = rho*gamma*sTg;
      int m=0;
      while (rprev < -looptol) {
        for (int i=0; i<n; i++) {
          try {
            x.setCoord(i, xa[i] + Math.pow(beta, m) * gamma * s.getCoord(i));
          }
          catch (parallel.ParallelException e) {  // can never get here
            e.printStackTrace();
          }
        }
        double fval; 
				try {
					fval = f.eval(x,p);  // was _master._f
				}  
				catch (Exception e) {
					throw new OptimizerException("PRCGThread.min(): f.eval() threw "+e);
				}
        if (fval <= fx + rprev) {
          h = Math.pow(beta,m)*gamma;
          break;
        }
        rprev = beta*rprev;
        m++;
      }
      if (h<=0) 
				throw new OptimizerException("PRCG could not find a valid h from x="+
                                     x+" after "+m+" iterations...");
      // set new x
      for (int i=0; i<n; i++) {
        try {
          x.setCoord(i, xa[i] + h * s.getCoord(i));
        }
        catch (parallel.ParallelException e) {  // can never get here
          e.printStackTrace();
        }
      }
      // update b
      VectorIntf gnew;
			try {
				gnew = grad.eval(x, p);
			}
			catch (Exception e) {
				throw new OptimizerException("PRCGThread.min(): g.eval() "+
					                           "threw "+e.toString());
			}
      VectorIntf gdiff = gnew.newCopy();
      for (int i=0; i<n; i++) {
        try {
          gdiff.setCoord(i,gdiff.getCoord(i)-g.getCoord(i));
        }
        catch (parallel.ParallelException e) {  // can never get here
          e.printStackTrace();
        }
      }
      b = VecUtil.innerProduct(gdiff,gnew)/(normg*normg);
      if (b<0) b = 0;  // automatic direction reset
			if (gdiff instanceof PoolableObjectIntf) {
				((PoolableObjectIntf) gdiff).release();
			}
    }
    // end main loop

    if (found) return new PairObjDouble(x, fx);
    else throw new OptimizerException("Thread did not find a solution"+
                                      " satisfying tolerance criteria from "+
                                      "the given initial point x0="+x0);
  }

}

