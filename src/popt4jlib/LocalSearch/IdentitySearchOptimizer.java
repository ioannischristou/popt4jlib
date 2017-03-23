/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.LocalSearch;

import java.util.HashMap;
import popt4jlib.FunctionIntf;
import popt4jlib.LocalOptimizerIntf;
import popt4jlib.OptimizerException;
import utils.PairObjDouble;
import utils.Params;


/**
 * class implements a no-search local search that always returns the 
 * argument passed to it, along with its evaluation result. Useful when no 
 * local search is needed.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IdentitySearchOptimizer implements LocalOptimizerIntf {
	// private final static long serialVersionUID=...L;
	private Params _params;
	private FunctionIntf _f;
	
	public LocalOptimizerIntf newInstance() {
		return new IdentitySearchOptimizer();
	}


	public HashMap getParams() {
		return null;
	}


	public synchronized void setParams(HashMap params) throws OptimizerException {
    if (_f!=null) 
			throw new OptimizerException("cannot modify parameters while running");
		_params = new Params(params);
	}


	public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) 
			throw new OptimizerException("IdentitySearchOptimizer.minimize(f): "+
				                           "null f");
    synchronized (this) {
      if (_f != null)
				throw new OptimizerException("ISO.minimize(): another thread is "+
					                           "concurrently executing the method on "+
					                           "the same object");
       _f = f;
		}
    Object x0 = _params.getObject("ids.dls.x0");
    if (x0==null) {
      throw new OptimizerException("no initial point (with key 'ids.dls.x0') "+
				                           "specified in params");
    }
		double val = _f.eval(x0, _params.getParamsMap());
		PairObjDouble p = new PairObjDouble(x0,val);
		synchronized (this) {
			_f=null;
		}
		return p; 
	}
	
}
