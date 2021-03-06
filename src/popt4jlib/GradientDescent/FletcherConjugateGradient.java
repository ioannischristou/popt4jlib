package popt4jlib.GradientDescent;

import popt4jlib.*;
import analysis.*;
import utils.*;
import parallel.ParallelException;
import java.util.*;


/**
 * Implementation of the Conjugate-Gradient method for nonlinear optimization,
 * using the Fletcher-Reeves formula for updating the b parameter, and
 * the Al-Baali - Fletcher bracketing &amp; sectioning method for step-size
 * determination. All the above are described in:
 * Fletcher R.(1987) Practical Methods of Optimization 2nd ed. Wiley, Chichester
 * The class allows multiple optimization runs from different starting points to
 * be excecuted in parallel (in multiple threads).
 * <p>Notes:
 * <ul>
 * <li>2021-05-08: ensured all exceptions thrown during function evaluation are
 * properly handled.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FletcherConjugateGradient implements LocalOptimizerIntf, 
	                                                IncumbentProviderIntf {
  HashMap _params;
  private double _incValue=Double.MAX_VALUE;
  private VectorIntf _inc=null;  // incumbent vector
  FunctionIntf _f;
  private transient FCGThread[] _threads=null;
  private int _numOK=0;
  private int _numFailed=0;

  /**
   * public no-arg constructor
   */
  public FletcherConjugateGradient() {
  }


  /**
   * public constructor, accepting the parameters to the optimization. The
   * params are copied to a local member, so later modification of the input
   * argument does not modify the optimization parameters.
   * @param params HashMap
   */
  public FletcherConjugateGradient(HashMap params) {
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
    return new FletcherConjugateGradient();
  }


  /**
   * the optimization params are set to p.
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
   * a later call to <CODE>setParams(p)</CODE>). These are:
	 * <ul>
   * <li>&lt;"fcg.numtries", ntries&gt; optional, the number of initial starting 
	 * points to use (must either exist then ntries &lt;"x$i$",VectorIntf v&gt; 
	 * pairs in the parameters or a pair 
	 * &lt;"[gradientdescent.]x0",VectorIntf v&gt; 
	 * pair in params). Default is 1.
	 * Notice that when no such point x0 is contained in the parameters, a random
	 * starting point is created. In this case, the parameters 
	 * "fcg.functionarg[max|min]val[$i$]" and "fcg.numdimensions" must be present.
   * <li>&lt;fcg.numthreads", Integer nt&gt; optional, the number of threads to 
	 * use. Default is 1.
   * <li>&lt;"fcg.gradient", VecFunctionIntf g&gt; optional, the gradient of f, 
	 * the function to be minimized. If this param-value pair does not exist, the
   * gradient will be computed using Richardson finite differences extrapolation
   * <li>&lt;"fcg.gtol", Double v&gt; optional, the minimum abs. value for each 
	 * of the gradient's coordinates, below which if all coordinates of gradient
   * happen to be, the search stops assuming it has reached a stationary point.
   * Default is 1.e-8.
   * <li>&lt;"fcg.maxiters", Integer miters&gt; optional, the maximum number of 
	 * major iterations of the CG search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
   * <li>&lt;"fcg.rho", Double v&gt; optional, the value of the parameter &rho; 
	 * in approximate line search step-size determination obeying the two 
	 * Wolfe-Powell conditions. Default is 0.1.
   * <li>&lt;"fcg.sigma", Double v&gt; optional, the value of the parameter 
	 * &sigma; in the approximate line search step-size determination obeying the 
	 * Wolfe-Powell conditions. Default is 0.9.
   * <li>&lt;"fcg.t1", Double v&gt; optional, the value of the parameter t_1 in 
	 * the Al-Baali - Fletcher bracketing-sectioning algorithm for step-size
   * determination. Default is 9.0.
   * <li>&lt;"fcg.t2", Double v&gt; optional, the value of the parameter t_2 in 
	 * the Al-Baali - Fletcher algorithm. Default is 0.1.
   * <li>&lt;"fcg.t3", Double v&gt; optional, the value of the parameter t_3 in 
	 * the Al-Baali - Fletcher algorithm. Default is 0.5.
   * <li>&lt;"fcg.redrate", Double v&gt; optional, a user acceptable reduction 
	 * rate on the function f for stopping the Al-Baali - Fletcher algorithm in 
	 * the bracketing phase. Default is 2.0.
   * <li>&lt;"fcg.fbar", Double v&gt; optional, a user-specified acceptable 
	 * function value to stop the Al-Baali - Fletcher algorithm in the bracketing 
	 * phase. Default is null (with the effect of utilizing the "fcg.redrate" 
	 * value for stopping criterion of the bracketing phase).
   * </ul>
   * @param f FunctionIntf the function to minimize
   * @throws OptimizerException if another thread is currently executing the
   * same method of this object or if the method fails to find a stationary
   * point.
   * @return PairObjDouble the pair containing the arg. min (a VectorIntf) and
   * the min. value found.
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("FCG.minimize(f): null f");
    try {
      synchronized (this) {
        if (_f != null)
					throw new OptimizerException("FCG.minimize(): another thread is "+
						                           "concurrently executing the method on "+
						                           "this object");
        _f = f;
        _inc = null;
        _incValue = Double.MAX_VALUE;
        _numOK=0;
        _numFailed=0;
      }
      int numthreads = 1;
      try {
        Integer ntI = (Integer) _params.get("fcg.numthreads");  // _params and
                                                                // other _XXX
                                                                // fields are
                                                                // atomically
                                                                // accessed.
        if (ntI != null && ntI.intValue() > 1) numthreads = ntI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      _threads = new FCGThread[numthreads];
      int ntries = 1;
      try {
        Integer ntriesI = (Integer) _params.get("fcg.numtries");
        if (ntriesI != null && ntriesI.intValue() > 1)
          ntries = ntriesI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      int triesperthread = ntries / numthreads;
      int rem = ntries;
      for (int i = 0; i < numthreads - 1; i++) {
        _threads[i] = new FCGThread(i, triesperthread, this);
        rem -= triesperthread;
      }
      _threads[numthreads - 1] = new FCGThread(numthreads - 1, rem, this);
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
      synchronized (this) {  // not needed, just to keep FindBugs happy
        if (_inc == null) // didn't find a solution
          throw new OptimizerException("failed to find solution");
        // ok, we're done
        PairObjDouble pr = new PairObjDouble(_inc, _incValue);
        return pr;
      }
    }
    finally {
      synchronized (this) {  // this synch is needed to ensure _f value
                             // is reflected in other threads' reads
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
	 * implementation of <CODE>IncumbentProviderIntf</CODE> method.
	 * @return double  // may be Double.MAX_VALUE
	 */
	public synchronized double getIncumbentValue() {
		return _incValue;
	}
	
	
	/**
	 * implementation of <CODE>IncumbentProviderIntf</CODE> method.
	 * @return Object  // VectorIntf may be null
	 */
	public synchronized Object getIncumbent() {
		return _inc;
	}
	

  /**
   * sets the passed VectorIntf arg as the incumbent solution iff its val value
   * is better than the current one so far registered
   * @param arg VectorIntf
   * @param val double
   * @throws OptimizerException if the val passed in as 2nd argument does not
   * agree with the evaluation of the function currently minimized at arg; this
   * can only happen if the function f is not reentrant (i.e. it's not thread-
   * safe). The program must be running in debug mode (the method
   * <CODE> Debug.setDebugBit(bits)</CODE> with bits equal to
   * <CODE>Constants.FCG</CODE> or some other value containing the given bit
   * must have been called before for this method to possibly throw)
   */
  synchronized void setIncumbent(VectorIntf arg, double val) 
		throws OptimizerException {
    if (val<_incValue) {
      if (Debug.debug(Constants.FCG)!=0) {
        // sanity check
        double incval;
				try { 
					incval = _f.eval(arg, _params);
				}
				catch (Exception e) {
					throw new OptimizerException("FCG.setIncumbent(): f.eval() threw "+e);
				}
        if (Math.abs(incval - _incValue) > 1.e-25) {
					Messenger mger = Messenger.getInstance();
          mger.msg("FCG.setIncumbent(): arg-val originally=" +
                   _incValue + " fval=" + incval + " ???", 0);
          throw new OptimizerException(
              "FCG.setIncumbent(): insanity detected; " +
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
   * auxiliary method: increments the number of successful optimization tries.
   */
  synchronized void incOK() { _numOK++; }


  /**
   * auxiliary method: increments the number of failed optimization tries.
   */
  synchronized void incFailed() { _numFailed++; }


  synchronized FunctionIntf getFunction() { return _f; }  // may be null

}


/**
 * auxiliary class implementing the threads that run each optimization attempt
 * of the main <CODE>FletcherConjugateGradient.optimize(f)</CODE> method. Not
 * part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class FCGThread extends Thread {

  private FletcherConjugateGradient _master;
  private int _numtries;
  private int _id;
  private int _uid;

	
  public FCGThread(int id, int numtries, FletcherConjugateGradient master) {
    _id = id;
    _uid = (int) DataMgr.getUniqueId();
    _master=master;
    _numtries=numtries;
  }

	
  public void run() {
    HashMap p = _master.getParams();
    p.put("thread.localid", new Integer(_id));
    p.put("thread.id", new Integer(_uid));  // used to be _id
    VectorIntf best = null;
    double bestval = Double.MAX_VALUE;
    FunctionIntf f = _master.getFunction();  // keep FindBugs happy
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
   * the implementation method of the class implements the CG algorithm with 
	 * Fletcher step-size updates.
   * @param f FunctionIntf
   * @param solindex int
   * @param p HashMap
   * @throws OptimizerException
   * @return PairObjDouble
   */
  private PairObjDouble min(FunctionIntf f, int solindex, HashMap p) 
		throws OptimizerException {
    VecFunctionIntf grad = (VecFunctionIntf) p.get("fcg.gradient");
    if (grad==null) 
			grad = new GradApproximator(f);  // default: numeric gradient computation
    VectorIntf x0 = 
			p.containsKey("fcg.x"+solindex)==false ?
         p.containsKey("gradientdescent.x0") ? 
			    (VectorIntf) p.get("gradientdescent.x0") : 
			      p.containsKey("x0") ? (VectorIntf) p.get("x0") : null // x0 exists?
			: (VectorIntf) p.get("fcg.x"+solindex);
			// if x0 is still null, check if "adam.numdimensions" is passed, in which 
			// case, create a random starting point
		if (x0==null && p.containsKey("fcg.numdimensions")) {
      final double maxargval = p.containsKey("fcg.functionargmaxval") ?
				((Double) p.get("fcg.functionargmaxval")).doubleValue() : 1.0;
		  final double minargval = p.containsKey("fcg.functionargminval") ?
				((Double) p.get("fcg.functionargminval")).doubleValue() : -1.0;
			int n = ((Integer) p.get("fcg.numdimensions")).intValue();
			x0 = new DblArray1Vector(n);
			final Random r = RndUtil.getInstance(_uid).getRandom();
			for (int j=0; j<n; j++) {
        double maxargvalj = maxargval;
	      Double maxargvaljD = (Double) p.get("fcg.functionargmaxval"+j);
		    if (maxargvaljD!=null && maxargvaljD.doubleValue()<maxargval)
			    maxargvalj = maxargvaljD.doubleValue();
				double minargvalj = minargval;
				Double minargvaljD = (Double) p.get("fcg.functionargminval"+j);
				if (minargvaljD!=null && minargvaljD.doubleValue()>minargval)
					minargvalj = minargvaljD.doubleValue();
				double val = minargvalj + r.nextDouble()*(maxargvalj-minargvalj);
				try {
					x0.setCoord(j, val);
				}
				catch (ParallelException e) {  // cannot get here
					e.printStackTrace();
				}
			}
		}
    if (x0==null) 
			throw new OptimizerException("no fcg.x"+solindex+
				                           " initial point in _params passed");
    VectorIntf x = x0.newInstance();  // don't modify the initial soln
    final int n = x.getNumCoords();
    double gtol = 1e-8;
    try {
      Double gtolD = (Double) p.get("fcg.gtol");
      if (gtolD != null && gtolD.doubleValue() > 0) gtol = gtolD.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    double h=0;
    double rho = 0.1;
    try {
      Double rD = (Double) p.get("fcg.rho");
      if (rD != null && rD.doubleValue() > 0 && rD.doubleValue() < 1.0)
        rho = rD.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    double fx = Double.NaN;
    int maxiters = Integer.MAX_VALUE;
    try {
      Integer miI = (Integer) p.get("fcg.maxiters");
      if (miI != null && miI.intValue() > 0)
        maxiters = miI.intValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    double sigma = 0.9;
    try {
      Double sigmaD = (Double) p.get("fcg.sigma");
      if (sigmaD != null &&
          sigmaD.doubleValue() > rho && sigmaD.doubleValue() < 1.0)
        sigma = sigmaD.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    double t1 = 9.0;
    try {
      Double t1D = (Double) p.get("fcg.t1");
      if (t1D != null && t1D.doubleValue() > 1) t1 = t1D.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    double t2 = 0.1;
    try {
      Double t2D = (Double) p.get("fcg.t2");
      if (t2D != null && t2D.doubleValue() <= 0.5) t2 = t2D.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    double t3 = 0.5;
    try {
      Double t3D = (Double) p.get("fcg.t3");
      if (t3D != null && t3D.doubleValue() <= 0.5) t3 = t3D.doubleValue();
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    Double fbarD = null;
    try {
      fbarD = (Double) p.get("fcg.fbar");
    }
    catch (ClassCastException e) { e.printStackTrace(); }
    Double red_rateD=null;
    try {
      red_rateD = (Double) p.get("fcg.redrate");
    }
    catch (ClassCastException e) { e.printStackTrace(); }
		
    // main loop
		
    boolean found=false;
    double b = 0.0;
    DblArray1Vector s = new DblArray1Vector(new double[n]);
		Messenger mger = Messenger.getInstance();
		VectorIntf gnew = null;
    for (int iter=0; iter<maxiters; iter++) {
      mger.msg("FCGThread.min(): Thread-id="+_id+" In iteration "+iter+
				       ", prevh="+h+", fx="+fx,1);
      VectorIntf g;
			try {
				g = gnew==null ? grad.eval(x, p) : gnew;
				fx = f.eval(x, p);
			}
			catch (Exception e) {
				throw new OptimizerException("FCGthread.min(): f or g eval() threw "+e);
			}
			_master.setIncumbent(x.newCopy(), fx);  // call incumbent update
      final double norminfg = VecUtil.normInfinity(g);
      final double normg = VecUtil.norm(g,2);
			mger.msg("FCGThread.min(): normg="+normg+" normg_inf="+norminfg,1);
      if (Double.compare(norminfg, gtol) <= 0) {
        mger.msg("FCGThread.min(): found sol w/ value="+fx+" normg="+norminfg+
					       " in "+iter+" iterations.",0);
        found = true;
        break;
      }
      if (iter % n ==0) {  // reset search direction: conjugacy is likely lost
        for (int i=0; i<n; i++) {
          // s.setCoord(i, -g.getCoord(i)/normg);
					s.setCoord(i, -g.getCoord(i));
        }
      }
      else {
        for (int i=0; i<n; i++)
          s.setCoord(i, s.getCoord(i)*b - g.getCoord(i));  // s update
        /*
				double norms = VecUtil.norm2(s);
        for (int i=0; i<n; i++)
          s.setCoord(i, s.getCoord(i)/norms);  // normalize search direction
				*/
      }
      // Al-Baali-Fletcher Bracketing-Sectioning Algorithm implementation
      // determine step-size h
      double sTg = VecUtil.innerProduct(s,g);
      if (Double.compare(sTg,0) >= 0) {  // reset search direction
        mger.msg("FCGThread.min(): Thread-id="+_id+
                 " resetting search direction",0);
        for (int i=0; i<n; i++) // s.setCoord(i, -g.getCoord(i)/normg);
					s.setCoord(i, -g.getCoord(i));
        // sTg = -normg;
				sTg = -normg*normg;
      }
      // initial guess on acceptable lower bound along f(h)
      double red_rate = 2.0;
      if (red_rateD!=null && red_rateD.doubleValue()>1.0)
        red_rate = red_rateD.doubleValue();
      double fbar = Double.compare(fx, 0)>0 ? 
				              fx/red_rate :
                      (Double.compare(fx, 0.0)==0 ? -1 : red_rate*fx);
      if (fbarD!=null) fbar = fbarD.doubleValue();
      h = findStepSize(f, grad, x, fx, s, sTg, fbar, rho, sigma, t1, t2, t3, p);
      if (Double.compare(h, 0)<= 0) {
        mger.msg("FCGTHread.min(): FCG will stop at x=" + x +
                                    " as it could not find a valid h", 0);
        return new PairObjDouble(x, fx);
      }
      // set new x
      for (int i=0; i<n; i++) {
        try {
          x.setCoord(i, x.getCoord(i) + h * s.getCoord(i));
        }
        catch (parallel.ParallelException e) {  // can never get here
          e.printStackTrace();
        }
      }
      // update b according to Fletcher-Reeves formula
			try {
				gnew = grad.eval(x, p);
			}
			catch (Exception e) {
				throw new OptimizerException("FCGThread.min(): g.eval() threw "+e);
			}			
      b = VecUtil.innerProduct(gnew,gnew)/(normg*normg);
    }
    // end main loop

    if (found) return new PairObjDouble(x, fx);
    else throw new OptimizerException("Thread did not find a solution"+
                                      " satisfying tolerance criteria from "+
                                      "the given initial point x0="+x0);
  }


  /**
   * Al-Baali - Fletcher 2-phase (bracketing-sectioning) method for step-size
   * determination in approximate line search. The method is used by other
   * classes as well, namely the PolakRibiereConjugateGradient classes.
   * @param f FunctionIntf the function to minimize
   * @param grad VecFunctionIntf the gradient of the function
   * @param x VectorIntf the current iterate point
   * @param fx double the value of f at the current iterate point
   * @param s VectorIntf the chosen search direction (must be descent direction)
   * @param sTg double (the inner product &lt;s,grad(x)&gt;)
   * @param fbar double a user-defined threshold for accepting a value in the
   * bracketing phase of the algorithm
   * @param rho double the parameter &rho; in the Wolfe-Powell conditions
   * @param sigma double the parameter &sigma; in the Wolfe-Powell conditions
   * @param t1 double the parameter t1 in the Al-Baali - Fletcher algorithm
   * @param t2 double the parameter t2 in the Al-Baali - Fletcher algorithm
   * @param t3 double the parameter t3 in the Al-Baali - Fletcher algorithm
   * @param p HashMap the parameters for function evaluation as well as
   * any constraint values for the computation of the algorithm
   * @return double the step-size to use in the determination of the next
   * iterate point, or -1 if the computation fails within the given parameter
   * constraints (passed in the last argument)
   */
  static double findStepSize(FunctionIntf f, VecFunctionIntf grad, VectorIntf x,
                              double fx, VectorIntf s, double sTg,
                              double fbar, double rho, double sigma,
                              double t1, double t2, double t3,
                              HashMap p) {
    final double miu = (fbar - fx)/(rho*sTg);
    final int n = x.getNumCoords();
    double alpha = 0.99*miu;
    double alpha_prev = 0.0;
    double a = alpha;
    double b = Double.NaN;
    double faprev = fx;
		
    // 1. bracketing phase: compute [a,b] or return with appropriate h
		
    int count=Integer.MAX_VALUE;
    Integer cI = (Integer) p.get("fcg.maxbracketingiters");
    if (cI!=null) count = cI.intValue();
    Messenger mger = Messenger.getInstance();
		mger.msg("FCGThread.findStepSize(): max iter count in bracketing phase="+
			       count,2);
    while (true) {
      if (--count==0) {
        mger.msg("FCGThread.findStepSize(): max allowed count exceeded "+
                 "in bracketing phase. "+
                 "(alpha="+alpha+", a="+a+", b="+b+")",0);
        return -1;
      }
      // compute f(x+alpha*s)
      VectorIntf xa = x.newCopy();
      for (int i=0; i<n; i++) {
        try {
          xa.setCoord(i, x.getCoord(i)+alpha*s.getCoord(i));
        }
        catch (parallel.ParallelException e) {  // can never get here
          e.printStackTrace();
        }
      }
      double fa;
			try { 
				fa = f.eval(xa, p);
			}
			catch (Exception e) {
				mger.msg("FCGThread.findStepSize(): f.eval() threw "+e.toString(), 0);
				return -1;
			}
      if (Double.compare(fa, fbar) <= 0) {
				if (xa instanceof PoolableObjectIntf) {
					((PoolableObjectIntf) xa).release();
				}
				return alpha;
			}
      if (Double.compare(fa, fx+rho*alpha*sTg) > 0 || 
				  Double.compare(fa, faprev) >= 0) {
        a=alpha_prev;
				b=alpha;
				if (xa instanceof PoolableObjectIntf) {
					((PoolableObjectIntf) xa).release();
				}
        break;
      }
      // evaluate f'(alpha)
      VectorIntf ga;
			try { 
				ga = grad.eval(xa,p);
			}
			catch (Exception e) {
				mger.msg("FCGThread.findStepSize(): g.eval() threw "+e.toString(), 0);
				return -1;
			}
      double fpa = VecUtil.innerProduct(s,ga);
      if (Double.compare(Math.abs(fpa), -sigma*sTg) <= 0) {
				if (xa instanceof PoolableObjectIntf) {
					((PoolableObjectIntf) xa).release();
				}
				return alpha;
			}
      if (Double.compare(fpa, 0) >= 0) {
        a = alpha; b = alpha_prev;
				if (xa instanceof PoolableObjectIntf) {
					((PoolableObjectIntf) xa).release();
				}
        break;
      }
      double a2aprev = 2.0*alpha - alpha_prev;
      if (Double.compare(miu, a2aprev) <= 0) {
        alpha_prev = alpha;
        alpha = miu;
      }
      else {
        double min_miu_aaprev = Math.min(miu, alpha+t1*(alpha-alpha_prev));
        alpha_prev = alpha;
        alpha = (a2aprev + min_miu_aaprev) / 2.0;
      }
      faprev = fa;
			if (xa instanceof PoolableObjectIntf) {
				((PoolableObjectIntf) xa).release();
			}
    }
		
    // 2. sectioning phase: find the right h to return

		final double abtol = 1.e-8;
    count = Integer.MAX_VALUE;
    cI = (Integer) p.get("fcg.maxsectioningiters");
    if (cI!=null) count = cI.intValue();
    mger.msg("FCGThread.findStepSize(): max iter count in sectioning phase="+
			       count,2);
    while (true) {
      if (--count==0) {
        mger.msg("FCGThread.findStepSize(): max allowed count exceeded "+
                 "in sectioning phase. "+
                 "(alpha="+alpha+", a="+a+", b="+b+")",0);
				// if a and b are close, return their average
				if (Double.compare(Math.abs(b-a), abtol) < 0) return (a+b)/2.0;
        return -1;
      }
      alpha = (a + t2*(b - a) + b - t3*(b - a)) / 2.0;
      // compute f(x+alpha*s)
      VectorIntf xa = x.newCopy();
      for (int i = 0; i < n; i++) {
        try {
          xa.setCoord(i, x.getCoord(i) + alpha * s.getCoord(i));
        }
        catch (parallel.ParallelException e) {  // can never get here
          e.printStackTrace();
        }
      }
      double falpha;
			try { 
				falpha = f.eval(xa, p);
			}
			catch (Exception e) {
				mger.msg("FCGThread.findStepSize(): f.eval() threw "+e.toString(), 0);
				return -1;				
			}
      // compute f(x+a*s)
      VectorIntf xaj = x.newCopy();
      for (int i = 0; i < n; i++) {
        try {
          xaj.setCoord(i, x.getCoord(i) + a * s.getCoord(i));
        }
        catch (parallel.ParallelException e) {  // can never get here
          e.printStackTrace();
        }
      }
      double faj;
			try {
				faj = f.eval(xaj, p);
			}
			catch (Exception e) {
				mger.msg("FCGThread.findStepSize(): f.eval() threw "+e.toString(), 0);
				return -1;				
			}
      if (Double.compare(falpha, fx + rho * alpha * sTg) > 0 || 
				  Double.compare(falpha, faj) >= 0) {
        b = alpha;
        //aprev = a;
      }
      else {
        // evaluate f'(alpha)
        VectorIntf ga;
				try { 
					ga = grad.eval(xa,p);
				}
				catch (Exception e) {
					mger.msg("FCGThread.findStepSize(): g.eval() threw "+e.toString(), 0);
					return -1;				
				}
        double fpa = VecUtil.innerProduct(s,ga);
        if (Double.compare(Math.abs(fpa), -sigma*sTg) <= 0) {
					if (xa instanceof PoolableObjectIntf) {
						((PoolableObjectIntf) xa).release();
					}		
					if (xaj instanceof PoolableObjectIntf) {
						((PoolableObjectIntf) xaj).release();
					}
					return alpha;
				}
        // aprev = a;
        if (Double.compare((b-a)*fpa, 0) >= 0) {
          b = a;
        }
				a = alpha;
      }
    }
  }

}

