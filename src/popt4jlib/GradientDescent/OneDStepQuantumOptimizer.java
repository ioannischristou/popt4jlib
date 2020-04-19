package popt4jlib.GradientDescent;


import popt4jlib.*;
import parallel.ParallelException;
import utils.PairSer;
import java.util.HashMap;

/**
 * the class implements a minimization method for a function alone one of its
 * variables, using an initial starting point x0, and a quantum step-size that
 * the variable must be constrained to take values on multiples of. The class
 * is not thread-safe (reentrant), but the main method <CODE>argmin()</CODE>
 * may be called concurrently from multiple threads as long as it is called on
 * different objects.
 * <p>Notes:
 * <ul>
 * <li>2018-12-29: class is useful enough to be made public.
 * <li>2020-04-18: restored debug messages, added maxcount check for iterating
 * over an interval where the function remains essentially constant.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final public class OneDStepQuantumOptimizer {
  private int _dir=0;
	private double _val=Double.NaN;
	
	private int _remaining_funcevals;
	

  /**
   * public no-arg no-op constructor.
   */
  public OneDStepQuantumOptimizer() {
  }


  /**
   * if the return value is zero, it means that the optimization process did not
   * produce a new result: the starting point was already (near-) minimal.
   * @return int
   */
  int getDir() {
    return _dir;
  }


	/**
	 * the best objective value found.
	 * @return double
	 */
	double getVal() {
		return _val;
	}


  /**
   * computes a local minimum of the restriction of the function f in the j-th
   * variable, under the box-constraints lowerbound &le; x_j &le; upperbound,
   * and also under the constraint that x_j = x0_j + k*stepquantum where k is an
   * integer quantity.
   * @param f FunctionIntf the function to optimize must accept arguments of 
	 * type VectorIntf
   * @param x0 VectorIntf the initial point
   * @param fparams HashMap the function params 
   * @param varindex int the dimension along which to minimize 
	 * (range in [0,...n))
   * @param stepquantum double
   * @param lowerbound double
   * @param upperbound double
   * @param niterbnd int how many iterations to proceed in the same direction
   * before incrementing the current step-size
   * @param multfactor int the mult factor by which to multiply the current
   * step-size after a number of iterations are all in the same direction
   * @param ftol double the function tolerance below which two function evals
   * are considered the same
	 * @param maxiterswithsamefunctionval int the number of function evaluations
	 * performed in the <CODE>detdir()</CODE> auxiliary method to determine the
	 * direction of descent when function values are "almost" (within ftol) the
	 * same
	 * @param maxnumfuncevals int the maximum number of function evaluations this
	 * call is allowed to perform befofe calling it quits.
   * @throws OptimizerException
   * @throws ParallelException
   * @return PairSer  // PairSer&lt;Double arg, Double val&gt;
   */
  public PairSer argmin(FunctionIntf f, VectorIntf x0, HashMap fparams,
                 int varindex, double stepquantum,
                 double lowerbound, double upperbound,
                 int niterbnd, int multfactor, double ftol, 
								 int maxiterswithsamefunctionval,
								 int maxnumfuncevals) 
		throws OptimizerException, ParallelException {
		utils.Messenger mger = utils.Messenger.getInstance();
    mger.msg("OneDStepQuantumOptimizer.argmin(): optimizing var x"+varindex+"="+
             x0.getCoord(varindex)+" in ["+lowerbound+","+upperbound+"]",3);
    if (niterbnd<=0) niterbnd = 5;
    if (multfactor<=0) multfactor = 2;
    double step = stepquantum;
    VectorIntf x = x0.newCopy();
    int cnt = niterbnd;
    double s = x.getCoord(varindex);
    double sqt = s;
    int prevdir = 0;
		_remaining_funcevals = maxnumfuncevals;
    while (_remaining_funcevals>0) {
			mger.msg("OneDStepQuantumOptimizer.argmin() x"+varindex+"="+s, 3);
      if (--cnt==0) {
        step *= multfactor;
        cnt = niterbnd;
      }
      x.setCoord(varindex, s);
      double news = detdir(f, fparams, x, varindex, stepquantum, 
				                   lowerbound, upperbound, ftol,
													 maxiterswithsamefunctionval);
      if (_dir==0) {
        sqt = news;
        break;
      }
      if (_dir>0) {
        if (news>=upperbound) {  // return closest feasible point to upperbound
          long k = (long) Math.floor((upperbound-x0.getCoord(varindex)) / 
						                         stepquantum);
          double xvarindex = x0.getCoord(varindex) + k*stepquantum;
					x.setCoord(varindex, xvarindex);
					_val = f.eval(x, fparams);
					--_remaining_funcevals;
					if (x instanceof PoolableObjectIntf) {
						((PoolableObjectIntf) x).release();
					}
					return new PairSer(new Double(xvarindex),new Double(_val));
        }
        s += step;
        if (prevdir<0 && step>stepquantum) {
          step /= multfactor;
          cnt = niterbnd;
        }
      }
      else {  // _dir<0
        if (news<=lowerbound) {  // return closest feasible point to lowerbound
          long k = (long) Math.ceil((x0.getCoord(varindex)-lowerbound) /
						                        stepquantum);
					double xvarindex = x0.getCoord(varindex) - k*stepquantum;
					x.setCoord(varindex, xvarindex);
					_val = f.eval(x, fparams);
					--_remaining_funcevals;
          if (x instanceof PoolableObjectIntf) {
						((PoolableObjectIntf) x).release();
					}
          return new PairSer(new Double(xvarindex), new Double(_val));
        }
        s -= step;
        if (prevdir>0 && step>stepquantum) {
          step /= multfactor;
          cnt = niterbnd;
        }
      }
      prevdir = _dir;
    }  // while there remain func evals to perform: used to be while true
    if (Math.abs(sqt-x0.getCoord(varindex))<=ftol) 
			_dir = -2;  // indicate no change
    if (Double.isNaN(_val)) {  // must evaluate at sqt
			x.setCoord(varindex, sqt);
			_val = f.eval(x, fparams);
		}
		if (x instanceof PoolableObjectIntf) {
			((PoolableObjectIntf) x).release();
		}
    return new PairSer(new Double(sqt), new Double(_val));
  }


  /**
   * determine in which direction (up or down) the function f decreases, along
   * the j-th variable, or return zero.
   * @param f FunctionIntf the function
   * @param params HashMap parameters of the function
   * @param x VectorIntf the argument
   * @param j int the index of the variable in question (in {0,...n-1})
   * @param eps double
   * @param lb double
   * @param ub double
   * @param ftol double
	 * @param maxiterswithsamefunctionval int
   * @throws ParallelException
   * @throws IllegalArgumentException
   * @return double
   */
  private double detdir(FunctionIntf f, HashMap params, VectorIntf x,
                        int j, double eps, double lb, double ub, double ftol,
												int maxiterswithsamefunctionval) 
		throws ParallelException, IllegalArgumentException {
		utils.Messenger mger = utils.Messenger.getInstance();
    try {
      final double s = x.getCoord(j);
      final double c = f.eval(x, params);
			--_remaining_funcevals;
      mger.msg("detdir: starting with x["+j+"]="+s+
				       " c="+c+" lb="+lb+" ub="+ub,3);
      x.setCoord(j, s + eps);
      if (ftol < 0) ftol = 0.0;
      double cup = f.eval(x, params);
			--_remaining_funcevals;
      if (Double.compare(c, cup + ftol) > 0) {
        _dir = 1;
				_val = cup;
        return s + eps;  // itc 20161116: used to be return s;
      }
      else if (Math.abs(c - cup) <= ftol) { // Double.compare(c,cup)==0
        double s2 = s;
        x.setCoord(j, s2);
        double cnew = f.eval(x, params);
				--_remaining_funcevals;
        for (int cnt=0; 
					   Math.abs(cnew - c) <= ftol && s2 < ub &&
					   cnt < maxiterswithsamefunctionval;
						 cnt++) {  
          // used to be while Double.compare(cnew,c)==0
          s2 += eps;
          x.setCoord(j, s2);
          cnew = f.eval(x, params);
					--_remaining_funcevals;
          mger.msg("ODSQO.detdir(): f(x" + j + "=" +
						       s2 + ")=" + cnew+" eps3="+eps, 4);
        }
        if (Double.compare(cnew, c - ftol) < 0) {
          _dir = 1;
					_val = cnew;
          return s2;
        }
        else { // cnew >= c - ftol OR s2 >= ub OR too many "sameval" iterations
          if (Double.compare(s2, ub) >= 0) { 
            // irrespective of "trend to decrease further or not"
            _dir = 0;
						_val = Double.NaN;  // not specified at this point
            return ub;
          }
          // ok, cnew >= c - ftol (OR too many "sameval" iterations), 
					// so start from the starting point, and go left
          s2 = s;
          x.setCoord(j, s2);
          cnew = c;  // itc 20161116: used to be cnew = f.eval(x, params);
          for (int cnt=0;
						   Math.abs(c - cnew) <= ftol && s2 > lb &&
						   cnt < maxiterswithsamefunctionval;
							 cnt++) {  
            // used to be while Double.compare(cnew,c)==0
            s2 -= eps;
            x.setCoord(j, s2);
            cnew = f.eval(x, params);
						--_remaining_funcevals;
            mger.msg("ODSQO.detdir(): f(x"+j+"="+s2+")="+cnew,4);
          }
          if (Double.compare(cnew, c - ftol) < 0) {
            _dir = -1;
						_val = cnew;
            return s2;
          }
          else { // cnew >= c + ftol OR s2 <= lb OR too many "sameval" iters
            _dir = 0;
            if (Double.compare(s2,lb) <= 0) {
              _val = Double.NaN;  // value unspecified
							return lb;
						}
						_val = c;
            return s;
          }
        }
      }
      // should try the down direction
      x.setCoord(j, s - eps);
      double cdown = f.eval(x, params);
			--_remaining_funcevals;
      if (Double.compare(c, cdown + ftol) > 0) {
        _dir = -1;
				_val = cdown;
        return s - eps;  // itc 20161116: used to be return s;
      }
      else if (Math.abs(c - cdown) <= ftol) {  // Double.compare(c,cdown)==0
        double s2 = s;
        x.setCoord(j, s2);
        double cnew = f.eval(x, params);
				--_remaining_funcevals;
        for (int cnt=0;
					   Math.abs(cnew - c) <= ftol && s2 > lb &&
					   cnt < maxiterswithsamefunctionval;
						 cnt++) {  // used to be while Double.compare(cnew,c)==0
          s2 -= eps;
          x.setCoord(j, s2);
          cnew = f.eval(x, params);
					--_remaining_funcevals;
          mger.msg("ODSQO.detdir(): f(x" + j + "=" +
                    s2 + ")=" + cnew+" eps="+eps, 4);
        }
        if (Double.compare(cnew, c - ftol) < 0) {
          _dir = -1;
					_val = cnew;
          return s2;
        }
        else { // cnew > c - ftol OR s2<=lb OR too many "sameval" iterations
          if (Double.compare(s2, lb) <= 0) { 
            // irrespective of "trend to decrease further or not"
            _dir = 0;
						_val = Double.NaN;
            return lb;
          }
          // ok, cnew > c - ftol, (OR too many "sameval" iterations) 
					// so go back to the starting point, and go right
          s2 = s;
          x.setCoord(j, s2);
          cnew = c; // itc 20161116: used to be cnew = f.eval(x, params);
          for (int cnt=0;
						   Math.abs(cnew - c) <= ftol && s2 < ub &&
						   cnt < maxiterswithsamefunctionval;
							 cnt++) {  // used to be while Double.compare(cnew,c)==0
            s2 += eps;
            x.setCoord(j, s2);
            cnew = f.eval(x, params);
						--_remaining_funcevals;
            mger.msg("ODSQO.detdir(): f(x" + j + "=" +
                     s2 + ")=" + cnew+" eps2="+eps, 4);
          }
          if (Double.compare(cnew, c - ftol) < 0) {
            _dir = 1;
						_val = cnew;
            return s2;
          }
          else {  // cnew > c (OR too many "sameval" iterations)
            _dir = 0;
            if (Double.compare(s2, ub) >= 0) {
              _val = Double.NaN;
							return ub;
						}
						_val = c;
            return s;
          }
        }
      }
      // if we reach here, we're optimal
      _dir = 0;
			_val = c;
      return s;
    }
    finally {
      mger.msg("ODSQO.detdir(): done",3);
    }
  }

}

