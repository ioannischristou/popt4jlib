package popt4jlib.MonteCarlo;

import popt4jlib.*;
import utils.*;
import java.util.*;

/**
 * A parallel implementation of a Monte-Carlo Simulation-inspired random search
 * of the function domain space. The class is primarily useful only as benchmark
 * method provider to compare other algorithms against (any optimization process
 * should produce superior results than the MCS class given the same number of
 * function evaluations.)
 * <p>Notes:
 * <ul>
 * <li>2020-04-25: method seParams() became public because it was moved up from
 * LocalOptimizerIntf to the root OptimizerIntf interface class.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MCS implements OptimizerIntf {
	
  HashMap _params;
  FunctionIntf _f;
  private MCSThread[] _threads=null;
  private double _min=Double.MAX_VALUE;
  private Object _argmin=null;
	

	/**
	 * private constructor prohibits no-arg MCS object construction.
	 */
  private MCS() {
  }


  /**
   * Construct an MCS (Monte Carlo Search) Object.
   * @param p HashMap
   */
  public MCS(HashMap p) {
    _params = new HashMap(p);
  }


  /**
   * return a copy of the params
   * @return HashMap
   */
  public synchronized HashMap getParams() {
    return new HashMap(_params);
  }


  /**
   * set the optimization parameters to the arg passed in.
   * @param params HashMap see the documentation of the method
	 * <CODE>minimize(f)</CODE> ((@see MCS#minimize(FunctionIntf) minimize) for a 
	 * discussion of the pairs that must be contained in it).
   * @throws OptimizerException if another thread is concurrently running
   * <CODE>minimize(f)</CODE> of this object
   */
  public synchronized void setParams(HashMap params) throws OptimizerException {
    if (_f!=null) 
			throw new OptimizerException("cannot modify parameters while running");
    _params = new HashMap(params);
  }


  /**
   * run a Monte-Carlo simulation-style attempt to minimize the function f:
   * Produce as many random solutions as are described in the value of the key
   * "mcs.numtries" in the _params HashMap, and return the best one (the arg
   * that produces the minimum function value).
   * The parameters that must have been passed in (via the constructor or via
   * a call to the <CODE>setParams(p)</CODE> method are as follows:
	 * <ul>
   * <li> &lt;"mcs.numtries", Integer ntries&gt; mandatory, the number of random 
	 * attempts to perform in total (these attempts will be distributed among the 
	 * number of threads that will be created.)
   * <li> &lt;"mcs.randomargmaker", RandomArgMakerIntf amaker&gt; mandatory, an 
	 * object that implements the RandomArgMakerIntf interface so that it can 
	 * produce function arguments for the function f to be minimized.
   * <li> &lt;"mcs.numthreads", Integer nt&gt; optional, the number of threads 
	 * to use, default is 1.
   * <li> any other parameters required for the evaluation of the function, or
   * by the objects passed in above (e.g. the RandomArgMakerIntf object etc.)
   * </ul>
   * The method will throw OptimizerException if it is called while another
   * thread is also executing the same method on the same object.
   * @param f FunctionIntf
   * @return PairObjDouble
   * @throws OptimizerException if another thread is concurrently running
   * the same method of this object
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) throw new OptimizerException("MCS.minimize(f): null f");
    try {
      synchronized (this) {
        if (_f != null)throw new OptimizerException("MCS.minimize(): " +
            "another thread concurrently executes the method on this object");
        _f = f;
        _argmin = null;
        _min = Double.MAX_VALUE;
      }
      int numthreads = 1;
      Integer ntI = (Integer) _params.get("mcs.numthreads");
      if (ntI != null && ntI.intValue() > 1) numthreads = ntI.intValue();
      _threads = new MCSThread[numthreads];
      Integer ntriesI = (Integer) _params.get("mcs.numtries");
      int ntries = ntriesI.intValue();
      int triesperthread = ntries / numthreads;
      int rem = ntries;
      for (int i = 0; i < numthreads - 1; i++) {
        _threads[i] = new MCSThread(i, triesperthread);
        rem -= triesperthread;
      }
      _threads[numthreads - 1] = new MCSThread(numthreads - 1, rem);
      // spawn work
      for (int i = 0; i < numthreads; i++) {
        _threads[i].start();
      }
      // wait until done
      for (int i = 0; i < numthreads; i++) {
        try {
          _threads[i].join();
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt(); // recommended action
        }
      }
      // ok, we're done
      synchronized (this) {
        PairObjDouble pr = new PairObjDouble(_argmin, _min);
        return pr;
      }
    }
    finally {
      synchronized (this) {  // communicate to other threads that we're done
        _f = null;
      }
    }
  }


  /**
   * set the current incumbent value (if the arguments passed in has better 
	 * objective value than the current incumbent).
   * @param arg Object
   * @param val double
   * @throws OptimizerException if the val passed in as 2nd argument does not
   * agree with the evaluation of the function currently minimized at arg; this
   * can only happen if the function f is not reentrant (i.e. it's not thread-
   * safe). The program must be running in debug mode (the method
   * <CODE> Debug.setDebugBit(bits) </CODE> with bits equal to
   * <CODE>Constants.DMC</CODE> or some other value containing the given bit
   * must have been called before for this method to possibly throw)
   */
  private synchronized void setIncumbent(Object arg, double val)
      throws OptimizerException {
    if (val<_min) {
      if (Debug.debug(Constants.DMC)!=0) {
        // sanity check
        double incval = _f.eval(arg, _params);
        if (Math.abs(incval - val) > 1.e-25) {
          Messenger.getInstance().msg("MCS.setIncumbent(): ind-val=" + val +
                                      " fval=" + incval + " ???", 0);
          throw new OptimizerException(
              "MCS.setIncumbent(): insanity detected; " +
              "most likely evaluation function is " +
              "NOT reentrant... " +
              "Add the 'function.notreentrant,num'" +
              " pair (num=1 or 2) to run parameters");
        }
        // end sanity check
      }
      _min=val;
      _argmin=arg;
    }
  }


  /**
   * introduced to keep FindBugs happy...
   * @return FunctionIntf
   */
  private synchronized FunctionIntf getFunction() {
    return _f;
  }


	/**
	 * auxiliary nested class implementing the threads that will run the Monte-
   * Carlo search.
	 */
  final class MCSThread extends Thread {

    private int _numtries;
    private int _id;
    private int _uid;
    private HashMap _fp;

		/**
		 * compile-time constant used in benchmarking against no-pooling mechanism
		 */
		private final static boolean _USE_POOLS = true;

		
		/**
		 * sole public constructor.
		 * @param id int
		 * @param numtries int
		 */
    public MCSThread(int id, int numtries) {
      _id = id;
      _uid = (int) DataMgr.getUniqueId();
      _numtries=numtries;
    }

		
		/**
		 * implements the main loop of the thread where for as many times as were
		 * specified in the second argument of the constructor, a random argument
		 * is constructed and evaluated. The best among those tried, is then 
		 * used to update the global incumbent found by any of the participating
		 * threads.
		 */
    public void run() {
      HashMap p = getParams();
      p.put("thread.localid", new Integer(_id));
      p.put("thread.id", new Integer(_uid));  // used to be _id
      // create the _funcParams
      _fp = new HashMap();
      Iterator it = p.keySet().iterator();
      while (it.hasNext()) {
        String key = (String) it.next();
        Object val = p.get(key);
        if (val!=null) {
          Class valclass = val.getClass();
          Package pack = valclass.getPackage();
          if (pack!=null) {
            String packname = pack.getName();
            if (packname.startsWith("popt4jlib") ||
                packname.startsWith("parallel"))continue;  // don't include 
						                                               // such objects
          }
          else {
            Messenger.getInstance().msg(
							"no package info for object with key "+key,2);
          }
          _fp.put(key,val);
        }
      }
      // end creating _funcParams

      Object best = null;
      double bestval = Double.MAX_VALUE;
      RandomArgMakerIntf maker=(RandomArgMakerIntf) p.get("mcs.randomargmaker");
      if (maker==null) {
        Messenger.getInstance().msg(
					"no RandomArgMakerIntf defined in params",0);
        return;
      }
			if (maker instanceof RandomArgMakerClonableIntf) {  
        // create a fast arg-maker
				try {
					maker = ((RandomArgMakerClonableIntf) maker).newInstance(p);
				}
				catch (OptimizerException e) {
					e.printStackTrace();
					Messenger.getInstance().msg(
						"RandomArgMakerClonableIntf.newInstance(params) failed",0);
					return;					
				}
			}
      FunctionIntf f = getFunction();
      for (int i = 0; i < _numtries; i++) {
				try {
          Object argi = maker.createRandomArgument(p);
          double val = f.eval(argi, _fp);
					if (!_USE_POOLS) {  // for benchmarking against no-pooling mechanism
	          if (val < bestval) {
		          bestval = val;
							best = argi;
						}
					}
					else {  // _USE_POOLS==true
						boolean pooling = argi instanceof PoolableObjectIntf;
						if (val < bestval) {  // found better local incumbent
							bestval = val;
							if (pooling) {  // clone the argument and store the clone in best 
								best = ((PoolableObjectIntf) argi).cloneObject();
								((PoolableObjectIntf) argi).release();
							}
							else {  // if cloning of the arg-maker is supported clone it, and
								      // keep the argument of the clone (to ensure the best soln
									    // is not overwriten if argument caching is used by the 
										  // arg-maker)
								if (maker instanceof RandomArgMakerClonableIntf) {
									RandomArgMakerClonableIntf clone = 
										((RandomArgMakerClonableIntf) maker).newInstance(p);
									try {
										best = clone.getCurrentArgument();
									}
									catch (UnsupportedOperationException e) {  
                    // arg-maker doesn't support getCurrentArgument() operation
										best = argi;
									}
								} 
								else best = argi;  // catch-all clause
							}
						}
						else if (pooling) ((PoolableObjectIntf) argi).release();
					}  // else (_USE_POOLS==true)
        }
        catch (Exception e) {
          e.printStackTrace();
          // no-op
        }
      }
      try {
        setIncumbent(best, bestval);  // _f may throw, or may not be re-entrant
      }
      catch (OptimizerException e) {
        e.printStackTrace();  // no further action
      }
    }

  }  // end MCSThread class

}

