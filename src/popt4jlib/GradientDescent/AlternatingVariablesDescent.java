package popt4jlib.GradientDescent;

import utils.*;
import parallel.*;
import parallel.distributed.*;
import popt4jlib.*;
import java.util.*;
import java.io.Serializable;

/**
 * Alternating Variables Optimization: the class implements a method suitable
 * for the (box-constrained) local-optimization of functions in R^n where some
 * or all of the function variables are constrained to assume values from a
 * discrete set (eg when some variables must take on integer values only, or
 * when some variables must take on values that are an integer multiple of
 * some quantity). The algorithm may run in distributed mode submitting its
 * tasks over a network of <CODE>parallel.distributed.PDBatchTaskExecInitedWrk</CODE>
 * workers if the appropriate parameters are passed, as specified in the 
 * documentation of the method <CODE>minimize()</CODE>, however this requires
 * submitting <CODE>AlternatingVariablesDescent.Opt1DTask</CODE> objects, which
 * are nested in this class definition, and for this reason, this class also
 * implements the <CODE>Serializable</CODE> interface.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
public final class AlternatingVariablesDescent extends GLockingObserverBase implements LocalOptimizerIntf, Serializable {
  private HashMap _params;
  private double _incValue=Double.MAX_VALUE;
  private VectorIntf _inc=null;  // incumbent vector
  private FunctionIntf _f;

	private RRObject _pdbtInitCmd=null;  // if running in distributed mode, 
	// this object will be sent first to server as initialization command.


  /**
   * public no-arg no-op constructor
   */
  public AlternatingVariablesDescent() {
    super();
		// no-op
  }

  /**
   * public constructor. The parameters passed in the argument are copied
   * in the data member _params so that later modifications to the argument
   * do not affect this object or its methods.
   * @param params HashMap
   */
  public AlternatingVariablesDescent(HashMap params) {
		super();
		try {
      setParams(params);
		// set the distributed computing mode init cmd, if any is specified
		_pdbtInitCmd = (RRObject) _params.get("avd.pdbtexecinitedwrkcmd");
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
    return new AlternatingVariablesDescent();
  }


  /**
   * the optimization params are set to a copy of p
   * @param p HashMap
   * @throws OptimizerException if another thread is currently executing the
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
   * <li> &lt;"avd.x0", VectorIntf x&gt; optional, the initial starting point. If this
   * pair does not exist, then it becomes mandatory that a pair
   * &lt;"gradientdescent.x0", VectorIntf x&gt; pair with a non-null x is in the
   * parameters that have been set.
   * <li> &lt;"avd.numthreads", Integer nt&gt; optional, the number of threads to use in
   * the optimization process (only if the key "avd.tryorder" does not exist in the
   * parameters). Default is 1.
   * <li> &lt;"avd.numtries", Integer ntries&gt; optional, the max number of outer
   * loops allowed. Default is Integer.MAX_VALUE.
   * <li> &lt;"avd.minstepsize", Double val&gt; optional, if it exists, indicates the
   * min step size (quantum) for any of the variables to be allowed to be a
   * multiple of. Default is 1.e-6.
   * <li> &lt;"avd.minstepsize$i$", Double val&gt; optional, if it exists indicates the
   * min step size (quantum) that the i-th variable must be a multiple of. If
   * a global step size also exists, then the maximum of the two is taken as
   * the min. step size required for the i-th variable.
   * <li> &lt;"avd.minargval", Double val&gt; optional, a double number that is a lower
   * bound for all variables of the optimization process, i.e. all variables
   * must satisfy x_i &ge; val.doubleValue(), default is -infinity.
   * <li> &lt;"avd.maxargval", Double val&gt; optional, a double number that is an upper
   * bound for all variables of the optimization process, i.e. all variables
   * must satisfy x_i &le; val.doubleValue(), default is +infinity.
   * <li> &lt;"avd.minargvals", double[] vals&gt; optional, the lower
   * bounds for each variable of the optimization process, i.e. variable
   * must satisfy x_i &ge; vals[i], default is none.
   * <li> &lt;"avd.maxargvals", double[] vals&gt; optional, the upper
   * bounds for each variable of the optimization process, i.e. variable
   * must satisfy x_i &le; vals[i], default is none.
   * <li> &lt;"avd.tryorder", int[] order&gt; optional, if present denotes the order
   * in which the variables will be optimized in each major iteration. (It is
   * NOT necessary to be a permutation of the numbers {1,...n} where n is the
   * dimensionality of the domain, but it is necessary that each value in the
   * array is in the set {1,...n} U {-1}; the value -1 indicates that a random
   * variable index should be chosen from {1,...,n}.) If it does not exist, then
   * in each major iteration, all variables are optimized (in parallel, but
   * considered in each thread one-by-one) and the optimization resulting in the
   * largest descent is chosen each time.
   * <li> &lt;"avd.niterbnd", Integer n&gt; optional, the number of inner-iterations
   * in the <CODE>OneDStepQuantumOptimizer</CODE> process before changing the 
	 * length of the step-size. Default is 5.
   * <li> &lt;"avd.multfactor", Integer n&gt; optional, the multiplication 
	 * factor when changing the inner step-size length. Default is 2.
   * <li> &lt;"avd.ftol", Double tol&gt; optional, the min. abs. value below which the
   * absolute value of the difference between two function evaluations is
   * considered to be zero. Default is 1.e-8.
	 * <li>&lt;"avd.pdbtexecinitedwrkcmd", RRObject cmd &gt; optional, the 
	 * initialization command to send to the network of workers to run minimization
	 * tasks, default is null, indicating no distributed computation.
	 * <li>&lt;"avd.pdbthost", String pdbtexecinitedhost &gt; optional, the name
	 * of the server to send 1-D minimization requests, default is localhost.
	 * <li>&lt;"avd.pdbtport", Integer port &gt; optional, the port the above 
	 * server listens to for client requests, default is 7891.
   * </ul>
   * @param f FunctionIntf the function to minimize
   * @throws OptimizerException if another thread is currently executing the
   * same method or if the method fails to find a minimizer
   * @return PairObjDouble the pair containing the arg. min (a VectorIntf) and
   * the min. value found
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("AVD.minimize(f): null f");
    try {
      synchronized (this) {
        if (_f != null)
          throw new OptimizerException("AVD.minimize(): " +
                                       "another thread is concurrently executing the method on the same object");
        _f = f;
        _inc = null;
        _incValue = Double.MAX_VALUE; // reset
      }
      // first set-up parameters from the _params hashtable
      VectorIntf x0 = null;
      try {
        x0 = (VectorIntf) _params.get("avd.x0");
        if (x0==null) {
          x0 = (VectorIntf) _params.get("gradientdescent.x0");
          if (x0==null)
            throw new OptimizerException("AVD.minimize(): no avd.x0 starting point");
        }
      }
      catch (ClassCastException e) {
        e.printStackTrace();
        throw new OptimizerException("AVD.minimize(): inappropriate value for x0 key in the params");
      }
      int numthreads = 1;
      try {
        Integer ntI = (Integer) _params.get("avd.numthreads");
        if (ntI != null && ntI.intValue() > 1) numthreads = ntI.intValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      int ntries = Integer.MAX_VALUE;
      try {
        Integer ntriesI = (Integer) _params.get("avd.numtries");
        if (ntriesI != null && ntriesI.intValue() >= 1)
          ntries = ntriesI.intValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      double minstepsize = 1.e-6;
      try {
        Double mssD = (Double) _params.get("avd.minstepsize");
        if (mssD!=null) minstepsize = mssD.doubleValue();
        if (minstepsize <= 0.0) minstepsize = 1.e-6;
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      int niterbnd = 5;
      try {
        Integer nI = (Integer) _params.get("avd.nitercnt");
        if (nI!=null) niterbnd = nI.intValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      int multfactor = 2;
      try {
        Integer nI = (Integer) _params.get("avd.multfactor");
        if (nI!=null) multfactor = nI.intValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      double minargval = Double.NEGATIVE_INFINITY;
      try {
        Double mavD = (Double) _params.get("avd.minargval");
        if (mavD!=null) minargval = mavD.doubleValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      double maxargval = Double.POSITIVE_INFINITY;
      try {
        Double MavD = (Double) _params.get("avd.maxargval");
        if (MavD!=null) maxargval = MavD.doubleValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      double ftol = 1.e-8;
      try {
        Double ftolD = (Double) _params.get("avd.ftol");
        if (ftolD!=null) ftol = ftolD.doubleValue();
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      double[] minstepsizes = new double[x0.getNumCoords()];
      for(int i=0; i<x0.getNumCoords(); i++) {
        minstepsizes[i] = minstepsize;
        try {
          Double mssD = (Double) _params.get("avd.minstepsize"+i);
          if (mssD!=null && mssD.doubleValue()<minstepsize && mssD.doubleValue()>0)
            minstepsizes[i] = mssD.doubleValue();
        }
        catch (ClassCastException e) {
          e.printStackTrace();
        }
      }
      double[] lowerbounds = null;
      try {
        lowerbounds = (double[]) _params.get("avd.minargvals");
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      if (lowerbounds==null) {
        lowerbounds = new double[x0.getNumCoords()];
        for (int i=0; i<lowerbounds.length; i++)
          lowerbounds[i] = Double.NEGATIVE_INFINITY;
      }
      for (int i=0; i<lowerbounds.length; i++) {
        if (lowerbounds[i]<minargval) lowerbounds[i] = minargval;
      }
      double[] upperbounds = null;
      try {
        upperbounds = (double[]) _params.get("avd.maxargvals");
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }
      if (upperbounds==null) {
        upperbounds = new double[x0.getNumCoords()];
        for (int i=0; i<upperbounds.length; i++)
          upperbounds[i] = Double.MAX_VALUE;
      }
      for (int i=0; i<upperbounds.length; i++) {
        if (upperbounds[i]>maxargval) upperbounds[i] = maxargval;
      }
      int[] tryorder = null;
      try {
        tryorder = (int[]) _params.get("avd.tryorder");
        if (tryorder==null) {
          // check out any tryorder points
          final int n = x0.getNumCoords();
          int[] tryorder2 = new int[n];
          boolean toexists = false;
          for (int i=0; i<n; i++) {
            Integer toi = (Integer) _params.get("avd.tryorder"+i);
            if (toi!=null) {
              toexists = true;
              tryorder2[i] = toi.intValue();
            }
            else tryorder2[i] = -1;
          }
          if (toexists) {
            _params.put("avd.tryorder",tryorder2);
            tryorder = tryorder2;
          }
        }
      }
      catch (ClassCastException e) {
        e.printStackTrace();
      }

      // the core of the method
      PairObjDouble p = null;
      if (tryorder==null) {
        // solve n 1-D problems in parallel
        Messenger.getInstance().msg("AVD.minimize(): minimizing using parallel searches",0);
        PDBatchTaskExecutor pbtexecutor=null;
				PDBTExecInitedClt pdbtclt=null;
				try {
					if (_pdbtInitCmd!=null) {  // run distributed
						String host=(String) _params.get("avd.pdbthost");
						if (host==null) host = "localhost";
						int port = 7891;
						Integer portI = (Integer) _params.get("avd.pdbtport");
						if (portI!=null) port = portI.intValue();
						pdbtclt = new PDBTExecInitedClt(host,port);
						pdbtclt.submitInitCmd(_pdbtInitCmd);
		        boolean cont=true;
			      VectorIntf x = x0.newInstance();  // used to be x0.newCopy();
				    double c = f.eval(x,_params);
					  double fbest = c;
			      int n = x0.getNumCoords();
						Opt1DTask[] batch = new Opt1DTask[n];
	          int k;
		        for (k = 0; k < ntries && cont; k++) {
			        cont = false;
						  for (int i = 0; i < n; i++) {
							  Opt1DTask ti = new Opt1DTask(f, x, _params, i, minstepsizes[i],
								                             lowerbounds[i], upperbounds[i],
									                           niterbnd, multfactor, ftol);
	              batch[i] = ti;
		          }
			        Object[] results = pdbtclt.submitWorkFromSameHost(batch,1);  // send the tasks to the server
				      // figure out best f-val and apply change to x
					    int ibest = -1;
						  double xibest = Double.NaN;
							for (int i=0; i<results.length; i++) {
								PairSer pi = (PairSer) results[i];
								Double xi = (Double) pi.getFirst();
	              if (xi.isNaN()) continue;  // no optimization took place
		            double xi_init = x.getCoord(i);
			          x.setCoord(i, xi.doubleValue());
				        double fival = ((Double) pi.getSecond()).doubleValue();  // used to be f.eval(x, _params);  
					      if (fival < fbest) {
						      fbest = fival;
							    xibest = xi.doubleValue();
								  ibest = i;
								}
								x.setCoord(i, xi_init);  // reset coord
							}
							if (ibest>=0) {  // found an improving solution
								x.setCoord(ibest, xibest);
								Messenger.getInstance().msg("AVD.minimize(x"+ibest+") new incumbent fval="+fbest,0);
								cont = true;
							}
						}
						utils.Messenger.getInstance().msg("AVD.minimize(): parallel searching yields #outer-iterations="+k,0);
						_incValue = fbest;
						_inc = x;
						p = new PairObjDouble(x, fbest);  // done
						return p;
					}  // run distributed mode
					else {  // run parallel mode
						Vector results = null;
						pbtexecutor=PDBatchTaskExecutor.newPDBatchTaskExecutor(numthreads);
		        boolean cont=true;
			      VectorIntf x = x0.newInstance();  // used to be x0.newCopy();
				    double c = f.eval(x,_params);
					  double fbest = c;
						List batch = new ArrayList();  // used to be Vector
	          int k;
		        for (k = 0; k < ntries && cont; k++) {
			        cont = false;
				      int n = x0.getNumCoords();
					    batch.clear();
						  for (int i = 0; i < n; i++) {
							  Opt1DTask ti = new Opt1DTask(f, x, _params, i, minstepsizes[i],
								                             lowerbounds[i], upperbounds[i],
									                           niterbnd, multfactor, ftol);
	              batch.add(ti);
		          }
			        results = pbtexecutor.executeBatch(batch);  // run in parallel
				      // figure out best f-val and apply change to x
					    int ibest = -1;
						  double xibest = Double.NaN;
							for (int i=0; i<results.size(); i++) {
								PairSer pi = (PairSer) results.get(i);
								Double xi = (Double) pi.getFirst();
	              if (xi.isNaN()) continue;  // no optimization took place
		            double xi_init = x.getCoord(i);
			          x.setCoord(i, xi.doubleValue());
				        double fival = ((Double) pi.getSecond()).doubleValue();  // used to be f.eval(x, _params);
					      if (fival < fbest) {
						      fbest = fival;
							    xibest = xi.doubleValue();
								  ibest = i;
								}
								x.setCoord(i, xi_init);  // reset coord
							}
							if (ibest>=0) {  // found an improving solution
								x.setCoord(ibest, xibest);
								Messenger.getInstance().msg("AVD.minimize(x"+ibest+") new incumbent fval="+fbest,0);
								cont = true;
							}
						}
						utils.Messenger.getInstance().msg("AVD.minimize(): parallel searching yields #outer-iterations="+k,0);
						_incValue = fbest;
						_inc = x;
						p = new PairObjDouble(x, fbest);  // done
						return p;
					}
        }
        catch (ParallelException e) {
          e.printStackTrace();
          throw new OptimizerException("ParallelException occured "+
                                       "executing PDBatchTaskExecutor's methods");
        }
				catch (Exception e) {
					e.printStackTrace();
					throw new OptimizerException("Exception occured: e="+e.toString());
				}
				finally {
					if (pbtexecutor!=null) {
						try {
							pbtexecutor.shutDown();
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					} else if (pdbtclt!=null) {
						try {
							pdbtclt.terminateConnection();
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
      }
      else {
        // solve a sequence of 1-D problems as defined in the tryorder array
        Messenger.getInstance().msg("AVD.minimize(): minimizing using sequential searches",0);
        OneDStepQuantumOptimizer onedopt = new OneDStepQuantumOptimizer();
        boolean cont=true;
        VectorIntf x = x0.newInstance();  // used to be x0.newCopy();
        int k;
        for(k=0; k<ntries & cont; k++) {
          cont=false;
          for (int i=0; i<tryorder.length; i++) {
            int j = tryorder[i];
            if (j==-1)  // try some random index to optimize
              j = RndUtil.getInstance().getRandom().nextInt(x0.getNumCoords());
            try {
              PairSer result = onedopt.argmin(f, x, _params, j, minstepsizes[j],
                                             lowerbounds[j], upperbounds[j],
                                             niterbnd, multfactor, ftol);
							double xj_opt = ((Double)result.getFirst()).doubleValue();
              x.setCoord(j, xj_opt);
              // double fx = f.eval(x, _params);
							// itc 20161116: no need to evaluate function again, as the result
							// is already available in the result pair!
							double fx = ((Double) result.getSecond()).doubleValue();
              if (fx < _incValue) {
                _inc = x.newInstance();  // used to be x.newCopy();
                _incValue = fx;
                Messenger.getInstance().msg("AVD.minimize(x"+j+") new incumbent fval="+_incValue,0);
                cont=true;
              }
            }
            catch (ParallelException e) {
              e.printStackTrace();
              throw new OptimizerException("AVD.minimize(): failed");
            }
          }
          if (!cont) {
            break;
          }
        }
        utils.Messenger.getInstance().msg("AVD.minimize(): sequential searching yields #outer-iterations="+k,0);
      }
      // done.
      p = new PairObjDouble(_inc, _incValue);
      return p;
    }
    finally {
      synchronized (this) {
        _f = null;
      }
    }
  }


  // GLockingObserverBase template method implementation
  /**
   * when a subject's thread calls the method notifyChange, in response, this
   * object will call its minimize() method, and add the result back into the
   * subject object.
   * @param subject SubjectIntf
   * @throws OptimizerException
   */
  protected void notifyChangeProtected(SubjectIntf subject) throws OptimizerException {
    Object arg = subject.getIncumbent();
    FunctionIntf f = subject.getFunction();
    VectorIntf x=null;
    if (arg instanceof double[]) {
      x = new DblArray1Vector((double[]) arg);
    } else if (arg instanceof VectorIntf) 
		x = new DblArray1Vector(((VectorIntf) arg).getDblArray1());  // don't know what kind of vector it is...
    else throw new OptimizerException("AVD.notifyChange(): don't know how to convert argument into VectorIntf object");
    HashMap params = subject.getParams();
    params.put("gradientdescent.x0",x);  // add the initial point
    setParams(params);  // set the params of this object
    PairObjDouble p = minimize(f);
    if (p!=null) {
      VectorIntf argmin = (VectorIntf) p.getArg();
      if (arg instanceof VectorIntf) subject.addIncumbent(this, argmin);
      else subject.addIncumbent(this, argmin.getDblArray1());
    }
  }

	
  /**
   * nested auxiliary inner-class to help with the parallel aspects of the
   * algorithm.
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011-2015</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class Opt1DTask implements TaskObject {
    private final static long serialVersionUID = 6310656709486850778L;
    private FunctionIntf _f;
    private VectorIntf _x0;
    private HashMap _params;
    private int _j;
    private double _stepsize;
    private double _lowerbound;
    private double _upperbound;
    private int _niterbnd;
    private int _multfactor;
    private double _ftol;
    private OneDStepQuantumOptimizer _opter;


    /**
     * sole class constructor.
     * @param f FunctionIntf
     * @param x0 VectorIntf
     * @param p HashMap
     * @param j int
     * @param ss double
     * @param lb double
     * @param ub double
     * @param niterbnd int
     * @param mul int
     * @param ftol double
     */
    Opt1DTask(FunctionIntf f, VectorIntf x0, HashMap p,
                     int j, double ss, double lb, double ub,
                     int niterbnd, int mul, double ftol) {
      _f = f;
      _x0 = x0.newInstance();  // used to be x0.newCopy();
      _params = new HashMap(p);
      _j = j;
      _stepsize = ss;
      _lowerbound = lb;
      _upperbound = ub;
      _niterbnd = niterbnd;
      _multfactor = mul;
      _ftol = ftol;
      _opter = new OneDStepQuantumOptimizer();
    }

    /**
     * execute the OneDStepQuantumOptimizer.argmin() method and return the arg
     * min for the given variable whose index was specified in the constructor.
     * @return Serializable a PairSer object holding the argument and the 
		 * objective value found (or null if it fails)
     */
    public Serializable run() {
      try {
        PairSer result = _opter.argmin(_f, _x0, _params, _j, _stepsize,
                                     _lowerbound, _upperbound, _niterbnd,
                                     _multfactor, _ftol);
        if (_opter.getDir()==-2) return new PairSer(new Double(Double.NaN), new Double(Double.NaN));
        else return result;
      }
      catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }

    /**
     * method returns always true.
     * @return boolean
     */
    public boolean isDone() { return true; }


    /**
     * unsupported: always throws IllegalArgumentException.
     * @param t TaskObject
     * @throws IllegalArgumentException
     */
    public void copyFrom(TaskObject t) throws IllegalArgumentException {
      throw new IllegalArgumentException("copyFrom(t) method not supported");
    }
  }

}

