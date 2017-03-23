package popt4jlib.GradientDescent;

import popt4jlib.LocalOptimizerIntf;
import java.util.*;
import utils.*;
import parallel.*;
import analysis.*;
import popt4jlib.*;

/**
 * Single-Threaded version of the FletcherConjugateGradient class that
 * implements a single try, single threaded local optimization from a single
 * initial point to a local stationary point of the function to be optimized.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FletcherConjugateGradientST extends GLockingObserverBase implements LocalOptimizerIntf {
  HashMap _params;
  FunctionIntf _f;


  /**
   * public no-arg constructor
   */
  public FletcherConjugateGradientST() {
		super();  // no-op
  }


  /**
   * public constructor accepting the optimization parameters to the process.
   * The parameters are copied so that later modifications to the input argument
   * do not affect the parameters passed in.
   * @param params HashMap
   */
  public FletcherConjugateGradientST(HashMap params) {
		super();
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
    return new FletcherConjugateGradientST();
  }


  /**
   * the optimization params are set to p
   * @param p HashMap
   * @throws OptimizerException if another thread is currently running the
   * <CODE>minimize(f)</CODE> method of this object.
   */
  public synchronized void setParams(HashMap p) throws OptimizerException {
    if (_f!=null) throw new OptimizerException("cannot modify parameters while running");
    _params = null;
    _params = new HashMap(p);  // own the params
  }


  /**
   * the main method of the class. Before it is called, a number of parameters
   * must have been set (via the parameters passed in the constructor, or via
   * a later call to setParams(p)). These are:
   * <ul>
	 * <li>&lt;"fcg.x0", VectorIntf x&gt; optional, the initial starting point. If this
   * pair does not exist, or if x is null, then it becomes mandatory that
   * a pair &lt;"[gradientdescent.]x0", VectorIntf x&gt; pair with a non-null x is
   * in the parameters that have been set.
   * <li>&lt;"fcg.numtries", ntries&gt; optional, the number of initial starting points
   * to use (must either exist then ntries &lt;"x$i$",VectorIntf v&gt; pairs in the
   * parameters or a pair &lt;"gradientdescent.x0",VectorIntf v&gt; pair in params).
   * Default is 1.
   * <li>&lt;"fcg.gradient", VecFunctionIntf g&gt; optional, the gradient of f, the
   * function to be minimized. If this param-value pair does not exist, the
   * gradient will be computed using Richardson finite differences extrapolation
   * <li>&lt;"fcg.gtol", Double v&gt; optional, the minimum abs. value for each of the
   * gradient's coordinates, below which if all coordinates of the gradient
   * happen to be, the search stops assuming it has reached a stationary point.
   * Default is 1.e-8.
   * <li>&lt;"fcg.maxiters", Integer miters&gt; optional, the maximum number of major
   * iterations of the CG search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
   * <li>&lt;"fcg.rho", Double v&gt; optional, the value of the parameter &rho; in approximate
   * line search step-size determination obeying the two Wolfe-Powell conditions
   * Default is 0.1.
   * <li>&lt;"fcg.sigma", Double v&gt; optional, the value of the parameter &sigma; in the
   * approximate line search step-size determination obeying the Wolfe-Powell
   * conditions. Default is 0.9.
   * <li>&lt;"fcg.t1", Double v&gt; optional, the value of the parameter t_1 in the
   * Al-Baali - Fletcher bracketing-sectioning algorithm for step-size
   * determination. Default is 9.0.
   * <li>&lt;"fcg.t2", Double v&gt; optional, the value of the parameter t_2 in the
   * Al-Baali - Fletcher algorithm. Default is 0.1.
   * <li>&lt;"fcg.t3", Double v&gt; optional, the value of the parameter t_3 in the
   * Al-Baali - Fletcher algorithm. Default is 0.5
   * <li>&lt;"fcg.redrate", Double v&gt; optional, a user acceptable reduction rate on the
   * function f for stopping the Al-Baali - Fletcher algorithm in the bracketing
   * phase. Default is 2.0.
   * <li>&lt;"fcg.fbar", Double v&gt; optional, a user-specified acceptable function value
   * to stop the Al-Baali - Fletcher algorithm in the bracketing phase. Default
   * is null (with the effect of utilizing the "fcg.redrate" value for stopping
   * criterion of the bracketing phase).
   * <li>&lt;"fcg.maxbracketingiters", Integer v&gt; optional, the maximum allowed
   * iterations in the bracketing phase of the Al-Baali - Fletcher bracketing /
   * sectioning algorithm for step-size determination. Default is length(x)*100.
   * <li>&lt;"fcg.maxsectioningiters", Integer v&gt; optional, the maximum allowed
   * iterations in the sectioning phase of the Al-Baali - Fletcher bracketing /
   * sectioning algorithm for step-size determination. Default is length(x)*100.
   * </ul>
   * @param f FunctionIntf the function object to minimize
   * @throws OptimizerException if another thread is currently running this
   * method of this object, or if no stationary point is found.
   * @return PairObjDouble the result
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("FCGST.minimize(f): null f");
    try {
      synchronized (this) {
        if (_f != null)throw new OptimizerException(
            "FCGST.minimize(): another " +
            "thread is concurrently executing the method on the same object");
        _f = f;
      }
      PairObjDouble pr = min(f, getParams());  // keep FindBugs happy
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
   * object will add the best solution found by the subject, in the _observers'
   * solutions map
   * @param subject SubjectIntf
   * @throws OptimizerException
   */
  protected void notifyChangeProtected(SubjectIntf subject) throws OptimizerException {
    Object arg = subject.getIncumbent();
    FunctionIntf f = subject.getFunction();
    VectorIntf x=null;
    if (arg instanceof double[]) x = new DblArray1Vector((double[]) arg);
    else if (arg instanceof VectorIntf) x = (DblArray1Vector) arg;
    else throw new OptimizerException("OFCGST.notifyChange(): don't know how to convert argument into VectorIntf object");
    final int len = x.getNumCoords();
    HashMap params = subject.getParams();
    // add the thread.id that the local optimizer may use; actually unnecessary
    // params.put("thread.id", new Integer(-1));  // indicate no thread id knowledge
    params.put("gradientdescent.x0",x);  // add the initial point
    params.put("fcg.maxiters", new Integer(100));  // used to be 1000
    int mbiters = len*10;  // used to be 100
    int msiters = len*10;  // used to be 100
    params.put("fcg.maxbracketingiters",new Integer(mbiters));
    params.put("fcg.maxsectioningiters",new Integer(msiters));
    setParams(params);
    PairObjDouble p = minimize(f);
    if (p!=null) {
      VectorIntf argmin = (VectorIntf) p.getArg();
      if (arg instanceof DblArray1Vector) subject.addIncumbent(this, argmin);
      else subject.addIncumbent(this, argmin.getDblArray1());
    }
  }


  /**
   * auxiliary method that implements the CG method with Fletcher-Reeves update
   * of b, and Al-Baali - Fletcher bracketing/sectioning method for step-size
   * determination.
   * @param f FunctionIntf
   * @param p HashMap
   * @throws OptimizerException
   * @return PairObjDouble
   */
  private PairObjDouble min(FunctionIntf f, HashMap p) throws OptimizerException {
    VecFunctionIntf grad = (VecFunctionIntf) p.get("fcg.gradient");
    if (grad==null) grad = new GradApproximator(f);  // default: numeric computation of gradient
    final VectorIntf x0 = 
			p.containsKey("fcg.x0")==false ?
         _params.containsKey("gradientdescent.x0") ? 
			    (VectorIntf) _params.get("gradientdescent.x0") : 
			      _params.containsKey("x0") ? (VectorIntf) _params.get("x0") : null // attempt to retrieve generic point
			: (VectorIntf) _params.get("fcg.x0");
    if (x0==null) throw new OptimizerException("no fcg.x0"+
                                               " initial point in _params passed");
    VectorIntf x = x0.newInstance();  // x0.newCopy();  // don't modify the initial soln
    final int n = x.getNumCoords();
    double gtol = 1e-8;
    try {
      Double gtolD = (Double) p.get("fcg.gtol");
      if (gtolD!=null && gtolD.doubleValue()>0) gtol = gtolD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();  // no-op
    }
    double h=0;
    double rho = 0.1;
    try {
      Double rD = (Double) p.get("fcg.rho");
      if (rD != null && rD.doubleValue() > 0 && rD.doubleValue() < 1)
        rho = rD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();  // no-op
    }
    double fx = Double.NaN;
    int maxiters = Integer.MAX_VALUE;
    try {
      Integer miI = (Integer) p.get("fcg.maxiters");
      if (miI != null && miI.intValue() > 0)
        maxiters = miI.intValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();  // no-op
    }
    double sigma = 0.9;
    try {
      Double sigmaD = (Double) p.get("fcg.sigma");
      if (sigmaD != null && sigmaD.doubleValue() > rho &&
          sigmaD.doubleValue() < 1)
        sigma = sigmaD.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();  // no-op
    }
    double t1 = 9.0;
    try {
      Double t1D = (Double) p.get("fcg.t1");
      if (t1D != null && t1D.doubleValue() > 1) t1 = t1D.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();  // no-op
    }
    double t2 = 0.1;
    try {
      Double t2D = (Double) p.get("fcg.t2");
      if (t2D != null && t2D.doubleValue() <= 0.5) t2 = t2D.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();  // no-op
    }
    double t3 = 0.5;
    try {
      Double t3D = (Double) p.get("fcg.t3");
      if (t3D != null && t3D.doubleValue() <= 0.5) t3 = t3D.doubleValue();
    }
    catch (ClassCastException e) {
      e.printStackTrace();  // no-op
    }
    Double fbarD=null;
    try {
      fbarD = (Double) p.get("fcg.fbar");
    }
    catch (ClassCastException e) {
      e.printStackTrace();  // no-op
    }
    Double red_rateD=null;
    try {
      red_rateD = (Double) p.get("fcg.redrate");
    }
    catch (ClassCastException e) {
      e.printStackTrace();  // no-op
    }

    // main loop
    boolean found=false;
    double b = 0.0;
    DblArray1Vector s = new DblArray1Vector(new double[n]);
    for (int iter=0; iter<maxiters; iter++) {
      VectorIntf g = grad.eval(x, p);
      fx = f.eval(x, p);
      Messenger.getInstance().msg("FCGST.min(): In iteration "+iter+", prevh="+h+", fx="+fx,1);
      final double norminfg = VecUtil.normInfinity(g);
      final double normg = VecUtil.norm(g,2);
      if (norminfg <= gtol) {
        Messenger.getInstance().msg("found sol w/ value="+fx+" normg="+norminfg+" in "+iter+" iterations.",0);
        found = true;
        break;
      }
      if (iter % n ==0) {
        for (int i=0; i<n; i++) {
          s.setCoord(i, -g.getCoord(i)/normg);  // reset search direction
        }
      }
      else {
        for (int i=0; i<n; i++)
          s.setCoord(i,s.getCoord(i)*b-g.getCoord(i));  // s update
        double norms = VecUtil.norm2(s);
        for (int i=0; i<n; i++)
          s.setCoord(i, s.getCoord(i)/norms);  // normalize search direction
      }
      // Al-Baali-Fletcher Bracketing-Sectioning Algorithm implementation
      // determine step-size h
      double sTg = VecUtil.innerProduct(s,g);
      if (sTg>=0) {  // reset search direction
        for (int i=0; i<n; i++) s.setCoord(i, -g.getCoord(i)/normg);
        sTg = -normg;
      }
      // init. guess on acceptable lower bound along f(h)
      double red_rate = 2.0;
      if (red_rateD!=null && red_rateD.doubleValue()>1.0)
        red_rate = red_rateD.doubleValue();
      double fbar = fx > 0 ? fx/red_rate :
                            (fx==0.0 ? -1 : red_rate*fx);
      if (fbarD!=null) fbar = fbarD.doubleValue();
      h = FCGThread.findStepSize(f, grad, x, fx, s, sTg, fbar, rho, sigma, t1, t2, t3, p);
      if (h<=0) {
        Messenger.getInstance().msg("FCG will stop at x=" + x +
                                    " after "+iter+" iterations"+
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
      VectorIntf gnew = grad.eval(x, p);
      b = VecUtil.innerProduct(gnew,gnew)/(normg*normg);
    }
    // end main loop

    if (found) return new PairObjDouble(x, fx);
    else throw new OptimizerException("Thread did not find a solution"+
                                      " satisfying tolerance criteria from "+
                                      "the given initial point x0="+x0);

  }

}

