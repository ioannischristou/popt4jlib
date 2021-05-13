package popt4jlib.GradientDescent;

import popt4jlib.LocalOptimizerIntf;
import java.util.*;
import cern.colt.matrix.*;
import cern.jet.math.*;
import cern.colt.matrix.impl.*;
import analysis.*;
import utils.*;
import popt4jlib.*;

/**
 * Quasi-Newton optimization using BFGS for inverse Hessian updates and Armijo
 * rule for step-size determination.
 * <p>Notes:
 * <ul>
 * <li>2021-05-08: ensured all exceptions thrown during function evaluation are
 * handled properly.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class ArmijoBFGS implements LocalOptimizerIntf {
  HashMap _params;
  private double _incValue=Double.MAX_VALUE;
  private VectorIntf _inc=null;  // incumbent vector
  FunctionIntf _f;
  transient private ABFGSThread[] _threads=null;


  /**
   * public default no-arg constructor
   */
  public ArmijoBFGS() {
  }

  /**
   * public constructor. The parameters passed in the argument are copied
   * in the data member _params so that later modifications to the argument
   * do not affect this object or its methods.
   * @param params HashMap
   */
  public ArmijoBFGS(HashMap params) {
    try {
      setParams(params);
    }
    catch (Exception e) {
      // no-op: cannot reach this point
    }
  }


  /**
   * return a new empty ArmijoBFGS optimizer object (that must be configured
   * via a call to setParams(p) before it is used).
   * @return LocalOptimizerIntf
   */
  public LocalOptimizerIntf newInstance() {
    return new ArmijoBFGS();
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
   * the optimization params are set to a copy of p
   * @param p HashMap
   * @throws OptimizerException if another thread is currently executing the
   * <CODE>minimize(f)</CODE> method of this object
   */
  public synchronized void setParams(HashMap p) throws OptimizerException {
    if (_f!=null) 
			throw new OptimizerException("cannot modify parameters while running");
    _params = null;
    _params = new HashMap(p);  // own the params
  }


  /**
   * the main method of the class. Optimizes the function f using Newton-type
   * search with BFGS updates of the inverse Hessian and Armijo rule step-size
   * determination. The method is re-entrant in that it is thread-safe: if some
   * thread calls the method <CODE>minimize(f)</CODE> on this object while 
	 * another thread is executing the <CODE>minimize(.)</CODE> method, the second 
	 * thread to call the method will throw an OptimizerException.
   * <p>Prior to this call, some parameters for the optimization process may be
   * set-up. These are:
	 * <ul>
   * <li>&lt;"abfgs.numtries", ntries&gt; optional, the number of initial 
	 * starting points to use (must either exist then ntries 
	 * &lt;"x$i$",VectorIntf v&gt; pairs in the parameters or a pair 
	 * &lt;"[gradientdescent.]x0",VectorIntf v&gt; pair in params). Default is 1.
   * <li>&lt;"abfgs.numthreads", Integer nt&gt; optional, the number of threads 
	 * to use. Default is 1.
   * <li>&lt;"abfgs.gradient", VecFunctionIntf g&gt; optional, the gradient of f 
	 * the function to be minimized. If this param-value pair does not exist, the
   * gradient will be computed using Richardson finite differences extrapolation
   * <li>&lt;"abfgs.gtol", Double v&gt; optional, the minimum absolute value for 
	 * each of the gradient's coordinates, below which if all coordinates of the 
	 * gradient happen to be, the search stops assuming it has reached a 
	 * stationary point. Default is 1.e-6.
   * <li>&lt;"abfgs.maxiters", Integer miters&gt; optional, the maximum number 
	 * of major iterations of the Newton-type search before the algorithm stops. 
	 * Default is Integer.MAX_VALUE.
   * <li> &lt;"abfgs.rho", Double v&gt; optional, the value for the parameter 
	 * &rho; in the Armijo rule implementation. Default is 0.1.
   * <li>&lt;"abfgs.beta", Double v&gt; optional, the value for the parameter 
	 * &beta; in the Armijo rule implementation. Default is 0.2.
   * <li>&lt;"abfgs.gamma", Double v&gt; optional, the value for the parameter 
	 * &gamma; in the Armijo rule implementation. Default is 1.
   * <li>&lt;"abfgs.maxarmijoiters", Integer v&gt; optional, the maximum number 
	 * of Armijo rule iterations allowed. Default is Integer.MAX_VALUE.
   *</ul>
   * @param f FunctionIntf
   * @throws OptimizerException if another thread is concurrently running the
   * <CODE>minimize(f)</CODE> method of this object or if the optimization
   * process fails.
   * @return PairObjDouble an object holding the argmin of the function
   * and the minimal value found
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("ArmijoBFGS.minimize(f): null f");
    try {
      synchronized (this) {
        if (_f != null)
          throw new OptimizerException("ArmijoBFGS.minimize(): " +
                                       "another thread is concurrently running"+
                                       " the method on the same object");
        _f = f;
        _inc = null;
        _incValue = Double.MAX_VALUE;
      }
      int numthreads = 1;
      Integer ntI = (Integer) _params.get("abfgs.numthreads");
      if (ntI != null && ntI.intValue() > 1) numthreads = ntI.intValue();
      _threads = new ABFGSThread[numthreads];
      int ntries = 1;
      try {
        Integer ntriesI = (Integer) _params.get("abfgs.numtries");
        if (ntriesI!=null) ntries = ntriesI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      int triesperthread = ntries / numthreads;
      int rem = ntries;
      for (int i = 0; i < numthreads - 1; i++) {
        _threads[i] = new ABFGSThread(i, triesperthread);
        rem -= triesperthread;
      }
      _threads[numthreads - 1] = new ABFGSThread(numthreads - 1, rem);
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
   * introduced to keep FindBugs happy...
   * @return FunctionIntf
   */
  synchronized FunctionIntf getFunction() { return _f; }


  /**
   * optimize the function f starting from the point that is found in the
   * parameters with key "abfgs.x$solindex$" (or, alternatively, the point
   * with key in the parameters "[gradientdescent.]x0")
   * @param f FunctionIntf the function to minimize
   * @param solindex int the index of the starting point
   * @throws OptimizerException if the process fails
   * @return PairObjDouble the pair of the arg. min and the min. value found
   */
  private PairObjDouble min(FunctionIntf f, int solindex) 
		throws OptimizerException {
    VecFunctionIntf grad = (VecFunctionIntf) _params.get("abfgs.gradient");
    if (grad==null) 
			grad = new GradApproximator(f);  // default: numeric gradient computation
    final VectorIntf x0 = 
			_params.containsKey("abfgs.x"+solindex)==false ?
         _params.containsKey("gradientdescent.x0") ? 
			    (VectorIntf) _params.get("gradientdescent.x0") : 
			      _params.containsKey("x0") ? (VectorIntf) _params.get("x0") : null 
            // attempt to retrieve generic point
			: (VectorIntf) _params.get("abfgs.x"+solindex);
    if (x0==null) 
			throw new OptimizerException("no abfgs.x"+solindex+
				                           " initial point in _params passed");
    VectorIntf x = x0.newInstance();  // x0.newCopy();  
                                      // don't modify the initial soln
    final int n = x.getNumCoords();
    double gtol = 1e-6;
    try {
      Double gtolD = (Double) _params.get("abfgs.gtol");
      if (gtolD != null && gtolD.doubleValue() > 0) gtol = gtolD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    double fx = Double.NaN;
    int maxiters = Integer.MAX_VALUE;
    try {
      Integer miI = (Integer) _params.get("abfgs.maxiters");
      if (miI != null && miI.intValue() > 0)
        maxiters = miI.intValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();
    }
    boolean found=false;
    DoubleMatrix2D G = new cern.colt.matrix.impl.DenseDoubleMatrix2D(n, n);
    // G is initially zero matrix
    for (int i=0; i<n; i++) G.setQuick(i,i,1.0);
    // G is the Identity matrix
    // BFGS loop
    for (int iter=0; iter<maxiters; iter++) {
      VectorIntf g;
			try { 
				g = grad.eval(x, _params);
				fx = f.eval(x, _params);  // used to be _f.eval(x, _params);
			}
			catch (Exception e) {
				throw new OptimizerException("ABFGS.min(): f or g evaluation threw "+
					                           e.toString());
			}
      double norminfg = VecUtil.normInfinity(g);
      if (norminfg <= gtol) {
        Messenger.getInstance().msg("found sol w/ normg="+norminfg+" in "+iter+
					                          " iterations.",0);
        found = true;
        break;
      }
      double[] xa = new double[n];
      for (int i=0; i<n; i++) xa[i] = x.getCoord(i);
      DoubleMatrix1D gk = new DenseDoubleMatrix1D(g.getDblArray1());
      DoubleMatrix1D sk = G.zMult(gk,null);
      for (int i=0; i<n; i++) sk.setQuick(i, -sk.getQuick(i));
      // find h
      double hk = min1D(f, x, x.newInstance(sk.toArray()));
      // compute delta
      DoubleMatrix1D d = new DenseDoubleMatrix1D(n);
      for (int i=0; i<n; i++) d.setQuick(i, hk*sk.getQuick(i));
      // compute new x
      try {
        for (int i = 0; i < n; i++) x.setCoord(i,
                                               x.getCoord(i) +
                                               hk * sk.getQuick(i));
      }
      catch (parallel.ParallelException e) {  // can never get here
        e.printStackTrace();
      }
      // compute gamma
      VectorIntf gx;
			try { 
				gx = grad.eval(x, _params);
			}
			catch (Exception e) {
				throw new OptimizerException("ABFGS.min(): g.eval() threw "+
					                           e.toString());
			}
      DoubleMatrix1D gm = new DenseDoubleMatrix1D(gx.getDblArray1());
      for (int i=0; i<n; i++) gm.setQuick(i,gm.getQuick(i)-g.getCoord(i));
      // compute gamma'delta
      double gTd = gm.zDotProduct(d);
      // compute gamma'Ggamma
      DoubleMatrix1D Ggamma = G.zMult(gm,null);
      double gTGg = Ggamma.zDotProduct(gm);
      // compute first factor
      double f1 = (1 + gTGg/gTd)/gTd;
      DoubleMatrix2D d2 = new DenseDoubleMatrix2D(n,1);
      for (int i=0; i<n; i++) d2.setQuick(i,0,d.getQuick(i));
      DoubleMatrix2D ddT = d2.zMult(d2,null,1,0,true,false);
      Mult mult = Mult.mult(f1);
      ddT.assign(mult);
      // compute dgTG
      DoubleMatrix2D gm2 = new DenseDoubleMatrix2D(n,1);
      for (int i=0; i<n; i++) gm2.setQuick(i,0,gm.getQuick(i));
      DoubleMatrix2D dgT = d2.zMult(gm2,null,1,0,true,false);
      DoubleMatrix2D dgTG = dgT.zMult(G,null);
      // compute G3 = -(GgdT+dgTG)/gTd
      DoubleMatrix2D GgdT = G.zMult(dgT,null,1,0,true,false);
      PlusMult pmult = PlusMult.plusMult(1);
      dgTG.assign(GgdT,pmult);  // dgTG is now dgTG + GgdT
      Mult divgTd = Mult.div(-gTd);
      dgTG.assign(divgTd);
      // compute G + f1*ddT + G3
      G.assign(ddT,pmult);
      G.assign(dgTG,pmult);
    }
    if (found) return new PairObjDouble(x, fx);
    else throw new OptimizerException("Thread did not find a solution"+
                                      " satisfying tolerance criteria from "+
                                      "the given initial point.");
  }


  /**
   * set the incumbent.
   * @param arg VectorIntf
   * @param val double
   * @throws OptimizerException if the debug bit Constants.ABFGS is set and
   * the insanity of the arg not being evaluated to the value val is detected,
   * caused by the function f being minimized not being reentrant.
   */
  synchronized void setIncumbent(VectorIntf arg, double val) 
		throws OptimizerException {
    if (val<_incValue) {
      if (Debug.debug(popt4jlib.Constants.ABFGS)!=0) {
        // sanity check
        double incval = Double.MAX_VALUE;
				try {
					incval = _f.eval(arg, _params);
				}
				catch (Exception e) {
					throw new OptimizerException("ABFGS.setIncumbent(): _f.eval() threw "+
						                           e.toString());
				}
        if (Math.abs(incval - _incValue) > 1.e-25) {
          Messenger.getInstance().msg("ABFGS.setIncumbent(): arg-val "+
						                          "originally=" + _incValue + 
						                          " fval=" + incval + " ???", 0);
          throw new OptimizerException(
              "ABFGS.setIncumbent(): insanity detected; " +
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
   * Armijo rule implementation: given the initial point x and the search dir
   * s, it computes the step-size h according to the Armijo rule.
   * @param f FunctionIntf the function to minimize
   * @param x VectorIntf the initial point
   * @param s VectorIntf the search direction
   * @throws OptimizerException if no appropriate step-size is found within the
   * required number of Armijo iterations.
   * @return double
   */
  private double min1D(FunctionIntf f, VectorIntf x, VectorIntf s) 
		throws OptimizerException {
    VecFunctionIntf grad = (VecFunctionIntf) _params.get("abfgs.gradient");
    if (grad==null) 
			grad = new GradApproximator(f);  // default: numeric gradient computation
    final int n = x.getNumCoords();
    double h=0;
    double rho = 0.1;
    Double rD = (Double) _params.get("abfgs.rho");
    if (rD!=null && rD.doubleValue()>0)
      rho = rD.doubleValue();
    double beta = 0.2;
    Double bD = (Double) _params.get("abfgs.beta");
    if (bD!=null && bD.doubleValue()>0)
      beta = bD.doubleValue();
    double gamma = 1;
    Double gD = (Double) _params.get("abfgs.gamma");
    if (gD!=null && gD.doubleValue()>0)
      gamma = gD.doubleValue();
    double fx = Double.NaN;
    int maxiters = Integer.MAX_VALUE;
    Integer miI = (Integer) _params.get("abfgs.maxarmijoiters");
    if (miI!=null && miI.intValue()>0)
      maxiters = miI.intValue();
    boolean found=false;
    VectorIntf g;
		try {
			g = grad.eval(x, _params);
	    fx = f.eval(x, _params);  // used to be _f
		}
    catch (Exception e) {
			throw new OptimizerException("ArmijoBFGS.min1D(): f or g eval threw "+
				                           e.toString());
		}
    double[] xa = new double[n];
    for (int i=0; i<n; i++) xa[i] = x.getCoord(i);
    // Armijo Rule implementation
    // determine step-size h
    double rprev = rho*gamma*innerProd(s,g);
    //System.err.println(iter+" iter, now computing h");
    for (int m=0; m<maxiters; m++) {
      for (int i=0; i<n; i++) {
        try {
          x.setCoord(i, xa[i] + Math.pow(beta, m) * gamma * s.getCoord(i));
        }
        catch (parallel.ParallelException e) {  // can never get here
          e.printStackTrace();
        }
      }
      double fval = Double.MAX_VALUE;
			try {
				fval = f.eval(x,_params);  // used to be _f
			}
	    catch (Exception e) {
				throw new OptimizerException("ArmijoBFGS.min1D(): f.eval() threw "+
					                           e.toString());
			}  
      if (fval <= fx + rprev) {
        h = Math.pow(beta,m)*gamma;
        found=true;
        break;
      }
      rprev = beta*rprev;
    }
    if (found) return h;
    else throw new OptimizerException("Thread did not find a minimizer"+
                                      " of the 1-D problem using "+
                                      "the Armijo Rule.");
  }


  /**
   * compute inner-product of two vectors. Obsolete as it also exists in the
   * VecUtils class.
   * @param x VectorIntf
   * @param y VectorIntf
   * @throws IllegalArgumentException
   * @return double
   */
  private double innerProd(VectorIntf x, VectorIntf y) 
		throws IllegalArgumentException {
    if (x==null || y==null) 
			throw new IllegalArgumentException("null args passed");
    if (x.getNumCoords()!=y.getNumCoords()) 
			throw new IllegalArgumentException("args have different dimensions");
    double res = 0.0;
    final int n = x.getNumCoords();
    for (int i=0; i<n; i++) res += x.getCoord(i)*y.getCoord(i);
    return res;
  }

	/**
	 * inner class implementing the threads that will do the concurrent 
	 * computations from different starting points. Not part of the public API.
	 */
	class ABFGSThread extends Thread {

    private int _numtries;
    private int _id;
    private int _uid;

		
		/**
		 * sole public constructor.
		 * @param id int thread-id
		 * @param numtries int the number of attempts to execute (from different 
		 * starting points)
		 */
    public ABFGSThread(int id, int numtries) {
      _id = id;
      _uid = (int) DataMgr.getUniqueId();
      _numtries=numtries;
    }


		/**
		 * implements the main loop of the thread.
		 */
    public void run() {
      HashMap p = new HashMap(_params);
      p.put("thread.localid", new Integer(_id));
      p.put("thread.id", new Integer(_uid));  // used to be _id
      VectorIntf best = null;
      double bestval = Double.MAX_VALUE;
      FunctionIntf f = getFunction();  // used to be _master._f;
      for (int i=0; i<_numtries; i++) {
        try {
          int index = _id*_numtries+i;  // this is the starting point soln index
          PairObjDouble pair = min(f, index);
          double val = pair.getDouble();
          if (val<bestval) {
            bestval=val;
            best=(VectorIntf) pair.getArg();
          }
        }
        catch (Exception e) {
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
  }  // end nested class ABFGSThread
}

