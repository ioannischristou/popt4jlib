package popt4jlib.GradientDescent;

import popt4jlib.LocalOptimizerIntf;
import java.util.*;
import utils.*;
import parallel.*;
import analysis.*;
import popt4jlib.*;

/**
 * Single-Threaded version of the ArmijoSteepestDescent class that implements a
 * single try, single threaded local optimization from a single initial point
 * to a local stationary point of the function to be optimized.
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
public class ArmijoSteepestDescentST extends GLockingObserverBase 
                                     implements LocalOptimizerIntf {
  HashMap _params;
  FunctionIntf _f;


  /**
   * public no-arg constructor
   */
  public ArmijoSteepestDescentST() {
    super();
		// no-op
  }


  /**
   * public constructor accepting the optimization parameters to the process.
   * The parameters are copied so that later modifications to the input argument
   * do not affect the parameters passed in.
   * @param params HashMap
   */
  public ArmijoSteepestDescentST(HashMap params) {
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
   * return an empty (no parameters) object of this class.
   * @return LocalOptimizerIntf
   */
  public LocalOptimizerIntf newInstance() {
    return new ArmijoSteepestDescentST();
  }


  /**
   * the optimization params are set to p
   * @param p HashMap
   * @throws OptimizerException if another thread is concurrently running the
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
   * &lt;"asd.x0", VectorIntf x&gt; optional, the initial starting point. If 
	 * this pair does not exist, or if x is null, then it becomes mandatory that
   * a pair &lt;"[gradientdescent.]x0", VectorIntf x&gt; pair with a non-null x 
	 * is in the parameters that have been set.
   * &lt;"asd.gradient", VecFunctionIntf g&gt; optional, the gradient of f, the
   * function to be minimized. If this param-value pair does not exist, the
   * gradient will be computed using Richardson finite differences extrapolation
   * &lt;"asd.gtol", Double v&gt; optional, the minimum abs. value for each of 
	 * the gradient's coordinates, below which if all coordinates of the gradient
   * happen to be, the search stops assuming it has reached a stationary point.
   * Default is 1.e-6.
   * &lt;"asd.maxiters", Integer miters&gt; optional, the maximum number of 
	 * major iterations of the SD search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
   * &lt;"asd.rho", Double v&gt; optional, the value for the parameter ñ in the
   * Armijo rule implementation. Default is 0.1.
   * &lt;"asd.beta", Double v&gt; optional, the value for the parameter â in the
   * Armijo rule implementation. Default is 0.8.
   * &lt;"asd.gamma", Double v&gt; optional, the value for the parameter ã in 
	 * the Armijo rule implementation. Default is 1.
   * &lt;"asd.looptol", Double v&gt; optional, the minimum step-size allowed. 
	 * Default is 1.e-21.
   *
   * @param f FunctionIntf the function to minimize
   * @throws OptimizerException if another thread is currently executing the
   * same method or if the method fails to find a minimizer
   * @return PairObjDouble the pair containing the arg. min (a VectorIntf) and
   * the min. value found
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("ArmijoSDST.minimize(f): null f");
    try {
      synchronized (this) {
        if (_f != null)
          throw new OptimizerException("ASDST.minimize(): another thread is " +
                                       "concurrently executing the method on "+
						                           "the same object");
        _f = f;
      }
      PairObjDouble pr = min(f, getParams());  // used to be _params
      return pr;
    }
    finally {
      synchronized (this) {
        _f = null;
      }
    }
  }


  // ObserverIntf methods implementation
  /**
   * when a subject's thread calls the method notifyChange, in response, this
   * object will call its minimize() method, and add the result back into the
   * subject object.
   * @param subject SubjectIntf
   * @throws OptimizerException
   */
  protected void notifyChangeProtected(SubjectIntf subject) 
		throws OptimizerException {
    Object arg = subject.getIncumbent();
    FunctionIntf f = subject.getFunction();
    VectorIntf x=null;
    if (arg instanceof double[]) {
      x = new DblArray1Vector((double[]) arg);
    } else if (arg instanceof VectorIntf) x = (DblArray1Vector) arg;
    else throw new OptimizerException("ASDST.notifyChange(): don't know "+
			                                "how to convert argument into "+
			                                "VectorIntf object");
    HashMap params = subject.getParams();
    params.put("gradientdescent.x0",x);  // add the initial point
    setParams(params);  // set the params of this object
    PairObjDouble p = minimize(f);
    if (p!=null) {
      VectorIntf argmin = (VectorIntf) p.getArg();
      if (arg instanceof DblArray1Vector) subject.addIncumbent(this, argmin);
      else subject.addIncumbent(this, argmin.getDblArray1());
    }
  }


  /**
   * the implementation of the Steepest-Descent method with Armijo rule for
   * step-size determination
   * @param f FunctionIntf the function to optimize
   * @param p HashMap the optimization and function parameters
   * @throws OptimizerException if the optimization process fails to find a
   * (near-)stationary point
   * @return PairObjDouble the argmin and min value found
   */
  private PairObjDouble min(FunctionIntf f, HashMap p) 
		throws OptimizerException {
    VecFunctionIntf grad = (VecFunctionIntf) p.get("asd.gradient");
    if (grad==null) 
			grad = new GradApproximator(f);  // default: numeric gradient computation
    final VectorIntf x0 = 
			_params.containsKey("asd.x0")==false ?
         _params.containsKey("gradientdescent.x0") ? 
			    (VectorIntf) _params.get("gradientdescent.x0") : 
			      _params.containsKey("x0") ? (VectorIntf) _params.get("x0") : null 
            // attempt to retrieve generic point
			: (VectorIntf) _params.get("asd.x0");
    if (x0==null) 
			throw new OptimizerException("no asd.x0 "+
				                           "initial point in _params passed");
    VectorIntf x = x0.newInstance();  // x0.newCopy();  
                                      // don't modify the initial soln
    final int n = x.getNumCoords();
		
    final double f0;
		try {
			f0 = f.eval(x0, p);
		}
		catch (Exception e) {
			throw new OptimizerException("ASDST.min(): f.eval() threw "+e.toString());
		}
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
    for (int iter=0; iter<maxiters; iter++) {
      h=0;
      VectorIntf g;
			try {
				g = grad.eval(x, p);
				fx = f.eval(x, p);  // was _f
			}
			catch (Exception e) {
				throw new OptimizerException("ASDST.min(): f or g evaluation threw "+e);
			}
      final double norminfg = VecUtil.normInfinity(g);
      final double normg = VecUtil.norm(g,2);
      if (norminfg <= gtol) {
        Messenger.getInstance().msg("found sol w/ norminfg="+norminfg+" in "+
					                          iter+" iterations.",0);
        found = true;
        break;
      }
      VectorIntf s = g.newCopyMultBy(-1.0/normg);  // normalize s
      double[] xa = new double[n];
      for (int i=0; i<n; i++) xa[i] = x.getCoord(i);
      // Armijo Rule implementation
      // determine step-size h
      double rprev = -rho*gamma*normg;
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
					fval = f.eval(x,p);  // was _f
				}  
				catch (Exception e) {
					throw new OptimizerException("ASDST.min(): f.eval() threw "+e);
				}
        if (fval <= fx + rprev) {
          h = Math.pow(beta,m)*gamma;
          fx = fval;
          break;
        }
        rprev = beta*rprev;
        m++;
      }
      if (h<=0) 
				throw new OptimizerException("ASDST could not find a valid h for x="+x+
					                           " after "+m+" iterations...");
      // set new x
      for (int i=0; i<n; i++) {
        try {
          x.setCoord(i, xa[i] + h * s.getCoord(i));
        }
        catch (parallel.ParallelException e) {  // can never get here
          e.printStackTrace();
        }
      }
    }
    if (found) return new PairObjDouble(x, fx);
    else if (fx < f0) {
      double imprv = fx==0 ? Double.MAX_VALUE : 100.0*(f0-fx)/Math.abs(fx);
      Messenger.getInstance().msg("ArmijoSteepestDescentST didn't find a "+
                                  "stationary point but improved "+imprv+
                                  "% upon init. soln",0);
      return new PairObjDouble(x, fx);
    }
    else throw new OptimizerException("Thread did not find a solution"+
                                      " satisfying tolerance criteria from "+
                                      "the given initial point x0="+x0);
  }

}

