/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib;
import utils.PairObjDouble;
import java.io.Serializable;
import java.util.HashMap;

/**
 * interface for executing a local-search as a (remote) task.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class LocalSearchFunctionEvaluationTask extends FunctionEvaluationTask {
	private LocalOptimizerIntf _locOpt;
	private Object _argMin;

	
	/**
	 * sole constructor.
	 * @param localoptimizer LocalOptimizerIntf
	 * @param f FunctionIntf 
	 * @param x0 Object
	 * @param params HashMap
	 */
	public LocalSearchFunctionEvaluationTask(LocalOptimizerIntf localoptimizer, 
		                                       FunctionIntf f,
		                                       Object x0, 
																					 HashMap params) {
		super(f,x0,params);
		_locOpt = localoptimizer;
	}
	
	
	/**
	 * executes the local-search and stores the results in this object.
	 * @return Serializable  // this
	 */
	public Serializable run() {
		try {
			HashMap p = new HashMap(getParams());
			p.put("x0", getArg());  // set the starting point of the local-search
			_locOpt.setParams(p);
			PairObjDouble res = _locOpt.minimize(getFunction());
			synchronized (this) {
				_argMin = res.getArg();
				setObjValue(res.getDouble());
				setDone(true);
			}
			return this;
		}
		catch (Exception e) {
			// e.printStackTrace();
			utils.Messenger mger = utils.Messenger.getInstance();
			mger.msg("LocalSearchFunctionEvaluationTask.run(): local optimizer "+
				       "threw exception '"+e+"'; will return null instead.", 0);
			return null;
		}
	}
	
	
	/**
	 * get the current minimizer.
	 * @return Object 
	 */
	public synchronized Object getArgMin() {
		return _argMin;
	}
		
}
