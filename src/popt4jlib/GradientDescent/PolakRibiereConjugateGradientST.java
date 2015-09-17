package popt4jlib.GradientDescent;

import java.util.*;
import utils.*;
import parallel.*;
import analysis.*;
import popt4jlib.*;

/**
 * Single-Threaded version of the PolakRibiereConjugateGradient class that
 * implements a single try, single threaded local optimization from a single
 * initial point to a local stationary point of the function to be optimized.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PolakRibiereConjugateGradientST extends GLockingObserverBase implements LocalOptimizerIntf {
  HashMap _params;
  FunctionIntf _f;


  /**
   * public no-arg constructor
   */
  public PolakRibiereConjugateGradientST() {
    super();  // no-op
  }


  /**
   * public constructor accepting the parameters to use in the optimization
   * process (can be nullified with a call to <CODE>setParams(p)</CODE>).
   * @param params HashMap the parameters to use. A local copy is made so that
   * later modifications of the argument do no affect the parameters passed in
   */
  public PolakRibiereConjugateGradientST(HashMap params) {
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
   * return an empty instance of this class.
   * @return LocalOptimizerIntf
   */
  public LocalOptimizerIntf newInstance() {
    return new PolakRibiereConjugateGradientST();
  }


  /**
   * the optimization params are set to p
   * @param p HashMap
   * @throws OptimizerException if another thread is concurrently running the
   * <CODE>minimize(f)</CODE> method of this object.
   */
  public synchronized void setParams(HashMap p) throws OptimizerException {
    if (_f!=null) throw new OptimizerException("cannot modify parameters while running");
    _params = null;
    _params = new HashMap(p);  // own the params
  }


  /**
   * the main method of the class. Some parameters must have been passed in
   * before calling this method, either in the constructor, or via a later call
   * to <CODE>setParams(p)</CODE>. These are:
   * &lt;"prcg.x0", VectorIntf x&gt; optional, the initial starting point. If this
   * pair does not exist, or if x is null, then it becomes mandatory that
   * a pair &lt;"gradientdescent.x0", VectorIntf x&gt; pair with a non-null x is
   * in the parameters that have been set.
   * &lt;"prcg.gradient", VecFunctionIntf g&gt; optional, the gradient of f, the
   * function to be minimized. If this param-value pair does not exist, the
   * gradient will be computed using Richardson finite differences extrapolation
   * &lt;"prcg.gtol", Double v&gt; optional, the minimum abs. value for each of the
   * gradient's coordinates, below which if all coordinates of the gradient
   * happen to be, the search stops assuming it has reached a stationary point.
   * Default is 1.e-8.
   * &lt;"prcg.maxiters", Integer miters&gt; optional, the maximum number of major
   * iterations of the CG search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
   * &lt;"prcg.rho", Double v&gt; optional, the value of the parameter &rho; in the Armijo
   * rule. Default is 0.1.
   * &lt;"prcg.beta", Double v&gt; optional, the value of the parameter &beta; in the
   * approximate line search step-size determination obeying the Armijo rule
   * conditions. Default is 0.9.
   * &lt;"prcg.gamma", Double v&gt; optional, the value of the parameter &gamma; in the
   * approximate line search step-size determination obeying the Armijo rule
   * conditions. Default is 1.0.
   * &lt;"prcg.looptol", Double v&gt; optional, the minimum step-size allowed. Default
   * is 1.e-21.
   *
   * @param f FunctionIntf the function to minimize
   * @throws OptimizerException if another thread is concurrently executing the
   * same method or if the optimization process fails
   * @return PairObjDouble the argmin found together with the min value found.
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("PRCGST.minimize(f): null f");
    try {
      synchronized (this) {
        if (_f != null)throw new OptimizerException(
            "PRCGST.minimize(): another " +
            "thread is concurrently executing the method on the same object");
        _f = f;
      }
      PairObjDouble pr = min(f, _params);  // atomic access to _params
                                           // FindBugs complains unjustly
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
   * object will set the best solution found by the subject, as the initial
   * point to start the PRCG optimization process, and upon completion of the
   * optimization process, it will add back the new solution to the subject by
   * calling the subject's <CODE>addIncumbent(this,sol)</CODE> method.
   * @param subject SubjectIntf
   * @throws OptimizerException
   */
  protected void notifyChangeProtected(SubjectIntf subject) throws OptimizerException {
    Object arg = subject.getIncumbent();
    FunctionIntf f = subject.getFunction();
    VectorIntf x=null;
    if (arg instanceof double[]) {
      x = new DblArray1Vector((double[]) arg);
    } else if (arg instanceof VectorIntf) x = (DblArray1Vector) arg;
    else throw new OptimizerException("OPRCGST.notifyChange(): don't know how to convert argument into VectorIntf object");
    HashMap params = subject.getParams();
    params.put("gradientdescent.x0",x);  // add the initial point
    params.put("prcg.maxiters", new Integer(1000));
    setParams(params);
    PairObjDouble p = minimize(f);
    if (p!=null) {
      VectorIntf argmin = (VectorIntf) p.getArg();
      if (arg instanceof DblArray1Vector) subject.addIncumbent(this, argmin);
      else subject.addIncumbent(this, argmin.getDblArray1());
    }
  }


  /**
   * the implementation of the Polak-Ribiere CG method with Armijo-rule for
   * step-size determination.
   * @param f FunctionIntf
   * @param p HashMap
   * @throws OptimizerException if the optimization process fails to find a
   * (near-)stationary point
   * @return PairObjDouble the arg.min and the min. value found.
   */
  private PairObjDouble min(FunctionIntf f, HashMap p) throws OptimizerException {
    VecFunctionIntf grad = (VecFunctionIntf) p.get("prcg.gradient");
    if (grad==null) grad = new GradApproximator(f);  // default: numeric computation of gradient
    final VectorIntf x0 = p.get("prcg.x0") == null ?
                          (VectorIntf) p.get("gradientdescent.x0") :  // attempt to retrieve generic point
                          (VectorIntf) p.get("prcg.x0");
    if (x0==null) throw new OptimizerException("no prcg.x0"+
                                               " initial point in _params passed");
    VectorIntf x = x0.newInstance();  // x0.newCopy();  // don't modify the initial soln
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
      VectorIntf g = grad.eval(x, p);
      fx = f.eval(x, p);  // was _f
      final double norminfg = VecUtil.normInfinity(g);
      final double normg = VecUtil.norm2(g);
      if (norminfg <= gtol) {
        Messenger.getInstance().msg("found sol w/ norminfg="+norminfg+" in "+iter+" iterations.",0);
        found = true;
        break;
      }
      for (int i=0; i<n; i++) {
        xa[i] = x.getCoord(i);
        if (iter % n == 0) s.setCoord(i, -g.getCoord(i));  // reset search direction
        else
          s.setCoord(i,s.getCoord(i)*b-g.getCoord(i));  // s update
      }
      double norms = VecUtil.norm2(s);
      for (int i=0; i<n; i++) s.setCoord(i, s.getCoord(i)/norms);  // normalize s
      // Armijo Rule implementation
      // determine step-size h
      double sTg = VecUtil.innerProduct(s,g);
      if (sTg>=0) {  // reset search direction
        for (int i=0; i<n; i++) s.setCoord(i, -g.getCoord(i)/normg);
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
        double fval = f.eval(x,p);  // was _f
        if (fval <= fx + rprev) {
          h = Math.pow(beta,m)*gamma;
          break;
        }
        rprev = beta*rprev;
        m++;
      }
      if (h<=0) throw new OptimizerException("PRCGST could not find a valid h "+
                                             "for x="+x+
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
      // update b
      VectorIntf gnew = grad.eval(x, p);
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

