package popt4jlib.neural;

import java.util.*;
import utils.*;
import analysis.*;
import parallel.*;
import popt4jlib.*;
import popt4jlib.GradientDescent.*;

/**
 * same as <CODE>popt4jlib.GradientDescent.stochastic.Adam</CODE> but with the
 * added hard-wired knowledge of mini-batch training, so that when computing
 * derivatives, it actually maintains the same mini-batch in each derivative
 * evaluation.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Adam4FFNN implements LocalOptimizerIntf {
  HashMap _params;
  private double _incValue=Double.MAX_VALUE;
  private VectorIntf _inc=null;  // incumbent vector
  FunctionIntf _f;
  private transient Adam4FFNNThread[] _threads=null;
  private int _numOK=0;
  private int _numFailed=0;

	private int _randombatchsize = 0;
	

  /**
   * public no-arg constructor
   */
  public Adam4FFNN() {
  }


  /**
   * public constructor. The parameters passed in the argument are copied
   * in the data member <CODE>_params</CODE> so that later modifications to the 
	 * argument do not affect this object or its methods.
   * @param params HashMap
   */
  public Adam4FFNN(HashMap params) {
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
   * return a new empty Adam4FFNN optimizer object (that must be
   * configured via a call to setParams(p) before it is used.)
   * @return LocalOptimizerIntf
   */
  public LocalOptimizerIntf newInstance() {
    return new Adam4FFNN();
  }


  /**
   * the optimization params are set to a copy of p.
   * @param p HashMap
   * @throws OptimizerException if another thread is currently executing the
   * <CODE>minimize(f)</CODE> method of this object.
   */
  public synchronized void setParams(HashMap p) throws OptimizerException {
    if (_f!=null) 
			throw new OptimizerException("cannot modify parameters while running");
    _params = null;
    _params = new HashMap(p);  // own the params
		if (_params.containsKey("ffnn.randombatchsize")) {
			_randombatchsize = 
				((Integer) _params.get("ffnn.randombatchsize")).intValue();
		}
  }


  /**
   * the main method of the class. Before it is called, a number of parameters
   * must have been set (via the parameters passed in the constructor, or via
   * a later call to setParams(p)). These are:
	 * <ul>
   * <li> &lt;"adam.numthreads", Integer nt&gt; optional, the number of threads 
	 * to use in the optimization process. Default is 1.
	 * <li>&lt;"adam.numtries", Integer ntries&gt; optional, the number of initial 
	 * starting points to use (must either exist then ntries 
	 * &lt;"adam.x$i$",VectorIntf v&gt; pairs in the parameters or a pair 
	 * &lt;"[gradientdescent.]x0",VectorIntf v&gt; pair in params). Default is 1.
	 * Notice that if no such starting point "[xxx.]x0" is given, then if a number
	 * for key "adam.numdimensions" exists, a random initial starting point x0 of
	 * the specified dimensions is costructed and used; otherwise an exception is
	 * thrown.
   * <li> &lt;"adam.gradient", VecFunctionIntf g&gt; optional the gradient of f,
   * the function to be minimized. If this param-value pair does not exist, the
   * gradient will be computed via Richardson finite differences extrapolation.
   * <li> &lt;"adam.gtol", Double v&gt; optional, the minimum abs. value for 
	 * each of the gradient's coordinates, below which if all coordinates of the 
	 * gradient happen to be, the search stops assuming it has reached a 
	 * stationary point. Default is 1.e-6.
   * <li> &lt;"adam.maxiters", Integer miters&gt; optional, the maximum number 
	 * of major iterations of the search before the algorithm stops. Default is
   * Integer.MAX_VALUE.
	 * <li> &lt;"adam.a", Double val&gt; optional, the alpha step-size parameter 
	 * of the Adam algorithm. Default is 0.001.
	 * <li> &lt;"adam.b1", Double val&gt; optional the &beta;_1 exponential decay
	 * rate for moment estimates. Default is 0.9.
	 * <li> &lt;"adam.b2", Double val&gt; optional the &beta;_2 exponential decay
	 * rate for moment estimates. Default is 0.999.
	 * <li> &lt;"adam.eps", Double val&gt; optional the &epsilon; factor in update
	 * parameter estimation. This value is also used to detect convergence -and so
	 * stop the algorithm- of the variables x. Default is 1.e-8.
	 * <li> &lt;"ffnn.randombatchsize", Integer num&gt; optional, the batch size
	 * used when evaluating a weighted FFNN. Default is 0 which amounts to using
	 * the entire training set.
   * </ul>
   * @param f FunctionIntf the function to minimize
   * @throws OptimizerException if another thread is currently executing the
   * same method or if the method fails to find a minimizer
   * @return PairObjDouble the pair containing the arg. min (a VectorIntf) and
   * the min. value found
   */
  public PairObjDouble minimize(FunctionIntf f) throws OptimizerException {
		if (f==null) 
			throw new OptimizerException("Adam4FFNN.minimize(f): null f");
    try {
      synchronized (this) {
        if (_f != null)
          throw new OptimizerException("Adam4FFNN.minimize(): " +
                                       "another thread is concurrently "+
						                           "executing the method on this object");
        _f = f;
        _numOK=0;
        _numFailed=0;
        _inc = null;
        _incValue = Double.MAX_VALUE;  // reset
      }
      int numthreads = 1;
      try {
        Integer ntI = (Integer) _params.get("adam.numthreads");
        if (ntI != null && ntI.intValue() > 1) numthreads = ntI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      _threads = new Adam4FFNNThread[numthreads];
      int ntries = 1;
      try {
        Integer ntriesI = (Integer) _params.get("adam.numtries");
        if (ntriesI != null && ntriesI.intValue() > 1) 
					ntries = ntriesI.intValue();
      }
      catch (ClassCastException e) { e.printStackTrace(); }
      int triesperthread = ntries / numthreads;
      int rem = ntries;
      for (int i = 0; i < numthreads - 1; i++) {
        _threads[i] = new Adam4FFNNThread(i, triesperthread);
        rem -= triesperthread;
      }
      _threads[numthreads - 1] = new Adam4FFNNThread(numthreads - 1, rem);
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
      synchronized (this) {  // keep FindBugs happy
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
   * auxiliary method.
   * @return int the number of "tries" that succeeded (found a stationary point)
   */
  public synchronized int getNumOK() { return _numOK; }
  /**
   * auxiliary method.
   * @return int the number of tries that failed (did not find a saddle point)
   */
  public synchronized int getNumFailed() { return _numFailed; }


  /**
   * set the incumbent solution -if it is better than the current incumbent.
   * @param arg VectorIntf proposed arg.
   * @param val double proposed value
   */
  synchronized void setIncumbent(VectorIntf arg, double val) {
    if (val<_incValue) {
      Messenger.getInstance().msg("Adam4FFNN: update incumbent "+
				                          "w/ new best value="+
				                          val,0);
      _incValue=val;
      _inc=arg;
    }
  }


  /**
   * introduced to keep FindBugs happy...
   * @return FunctionIntf
   */
  synchronized FunctionIntf getFunction() { return _f; }


  /**
   * auxiliary method
   */
  synchronized void incOK() { _numOK++; }


  /**
   * auxiliary method
   */
  synchronized void incFailed() { _numFailed++; }

  // nested class implementing the threads to be used by Adam main class
  class Adam4FFNNThread extends Thread {

    private int _numtries;
    private int _id;
    private int _uid;

    Adam4FFNNThread(int id, int numtries) {
      _id = id;
      _uid = (int) DataMgr.getUniqueId();
      _numtries=numtries;
    }

    public void run() {
      HashMap p = getParams();  // was new HashMap(_master._params);
      p.put("thread.localid", new Integer(_id));
      p.put("thread.id", new Integer(_uid));  // used to be _id
      VectorIntf best = null;
      double bestval = Double.MAX_VALUE;
      FunctionIntf f = getFunction();  // was _master._f;
      for (int i=0; i<_numtries; i++) {
        try {
          int index = _id*_numtries+i;  // this is the starting point soln index
          PairObjDouble pair = min(f, index, p);
          double val = pair.getDouble();
          if (val<bestval) {
            bestval=val;
            best=(VectorIntf) pair.getArg();
          }
          incOK();
        }
        catch (Exception e) {
          incFailed();
          e.printStackTrace();
          // no-op
        }
      }
      try {
        setIncumbent(best, bestval);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }


    private PairObjDouble min(FunctionIntf f, int solindex, HashMap p) 
			throws OptimizerException {
			final double init_mul_factor = 5.0;
			Messenger mger = Messenger.getInstance();
      VecFunctionIntf grad = (VecFunctionIntf) p.get("adam.gradient");
      if (grad==null) grad = new GradApproximator(f);  
      // default: numeric computation of gradient
      VectorIntf x0 = 
			  _params.containsKey("adam.x"+solindex)==false ?
          _params.containsKey("gradientdescent.x0") ? 
			    (VectorIntf) _params.get("gradientdescent.x0") : 
			      _params.containsKey("x0") ? 
				      (VectorIntf) _params.get("x0") : null // retrieve generic point?
			    : (VectorIntf) _params.get("adam.x"+solindex);
			// if x0 is still null, check if "adam.numdimensions" is passed, in which 
			// case, create a random starting point
			if (x0==null && _params.containsKey("adam.numdimensions")) {
				int n = ((Integer) _params.get("adam.numdimensions")).intValue();
				Random r = RndUtil.getInstance(_uid).getRandom();
				x0 = new DblArray1Vector(n);
				for (int i=0; i<n; i++) {
					try {
						x0.setCoord(i, init_mul_factor*r.nextGaussian());
					}
					catch (ParallelException e) {
						// can never get here
					}
				}
			}
			if (x0==null) 
				throw new OptimizerException("no adam.x"+solindex+
					                           " initial point in _params passed");
      VectorIntf x = x0.newInstance();  // don't modify the initial soln
      final int n = x.getNumCoords();
      double gtol = 1e-6;
      Double gtolD = (Double) p.get("adam.gtol");
      if (gtolD!=null && gtolD.doubleValue()>=0) gtol = gtolD.doubleValue();
      double a = 0.001;
      Double aD = (Double) p.get("adam.a");
      if (aD!=null && aD.doubleValue()>0)
        a = aD.doubleValue();
      double b1 = 0.9;
      Double b1D = (Double) p.get("adam.b1");
      if (b1D!=null && b1D.doubleValue()>0 && b1D.doubleValue()<1.0)
        b1 = b1D.doubleValue();
			//final double b1_inv = 1.0 / b1;
			final double one_minus_b1 = 1.0 - b1; 
      double b2 = 0.999;
      Double b2D = (Double) p.get("adam.b2");
      if (b2D!=null && b2D.doubleValue()>0 && b2D.doubleValue()<=1.0)
        b2 = b2D.doubleValue();
			final double one_minus_b2 = 1.0 - b2;
			double eps = 1e-8;
			Double eD = (Double) p.get("adam.eps");
			if (eD!=null && eD.doubleValue()>0) eps = eD.doubleValue();
      double fx = Double.NaN;
      int maxiters = Integer.MAX_VALUE;
      Integer miI = (Integer) p.get("adam.maxiters");
      if (miI!=null && miI.intValue()>0)
        maxiters = miI.intValue();
      boolean found=false;

      // main iteration loop
      double[] xa = new double[n];  // xa is zero at this point
			final VectorIntf m = x0.newInstance(xa);  // first-moment est. starts at 0
			final VectorIntf v = m.newInstance();  // second-moment est. starts at 0
			final VectorIntf mhat = m.newInstance();  // same as above
			final VectorIntf vhat = m.newInstance();  // same as above
			final VectorIntf g_squared = m.newInstance();
			double b1_t = b1;
			double b2_t = b2;
			double[][] all_train_data = (double[][]) p.get("ffnn.traindata");
			double[] all_train_labels = (double[]) p.get("ffnn.trainlabels");
			double[][] batch_train_data = null;
			double[] batch_train_labels = null;
			if (_randombatchsize>0) {
				batch_train_data = new double[_randombatchsize][];
				batch_train_labels = new double[_randombatchsize];
				p.remove("ffnn.randombatchsize");
			}

			final Random r = RndUtil.getInstance(_uid).getRandom();			
			// start main iteration loop
      for (int iter=0; iter<maxiters; iter++) {
	      mger.msg("Adam4FFNNThread.min(): #iters="+iter, 2);
				if (_randombatchsize>0) { // set the training set to a random mini-batch
					double prob_enter = _randombatchsize/((double) all_train_data.length);
					int cnt = 0;
					while (cnt<_randombatchsize) {
						for (int i=0; i<all_train_data.length; i++) {
							if (prob_enter>=r.nextDouble()) {
								batch_train_data[cnt] = all_train_data[i];
								batch_train_labels[cnt++] = all_train_labels[i];
								if (cnt==_randombatchsize) break;
							}
						}
					}
					p.put("ffnn.traindata", batch_train_data);
					p.put("ffnn.trainlabels", batch_train_labels);
					mger.msg("Adam4FFNNThread.min(): p now have "+
						       "random batch of size "+batch_train_data.length, 2);
				}
				VectorIntf g = grad.eval(x, p);
		    double normg = VecUtil.norm(g,2);
				mger.msg("Adam4FFNNThread.min(): normg="+normg,2);
				//VectorIntf g_squared = VecUtil.componentProduct(g, g);
				for (int i=0; i<n; i++) {
					final double gi = g.getCoord(i);
					try {
						g_squared.setCoord(i, gi*gi);
					}
					catch (ParallelException e) {
						// can't get here
					}
				}
				fx = f.eval(x, p);
				mger.msg("Adam4FFNNThread.min(): f(x)="+fx, 2);
				mger.msg("x="+x, 2);
				final double norminfg = VecUtil.normInfinity(g);
        if (norminfg <= gtol) {
          mger.msg("Adam4FFNNThread.min(): found sol w/ norminfg="+norminfg+
						                          " in "+iter+" iterations.",0);
          found = true;
          break;
        }
        for (int i=0; i<n; i++) xa[i] = x.getCoord(i);  // set xa = x
        // Adam algorithm essense:
        // update formulas
				try {
					// m.div(b1_inv);
					m.mul(b1);
					m.addMul(one_minus_b1, g);
					// v.div(b2_inv);
					v.mul(b2);
					v.addMul(one_minus_b2, g_squared);
					b1_t *= b1;
					b2_t *= b2;
					// set mhat = m, vhat = v
					for (int i=0; i<n; i++) {
						mhat.setCoord(i, m.getCoord(i));
						vhat.setCoord(i, v.getCoord(i));
					}
					mhat.div(1.0-b1_t);
					vhat.div(1.0-b2_t);
					// update x
					for (int i=0; i<n; i++) {
						double vi = x.getCoord(i) - 
							          a*mhat.getCoord(i)/(eps+Math.sqrt(vhat.getCoord(i)));
						x.setCoord(i, vi);
					}
					// compute difference from prev x
					double diff_prev = 0.0;
					for (int j=0; j<n; j++) diff_prev += Math.abs(xa[j]-x.getCoord(j));
					mger.msg("Adam4FFNNThread.min(): ||x - x_prev||_1="+diff_prev, 2);
					if (diff_prev < eps) {
						mger.msg("Adam4FFNNThread.min(): x converged, stop iterations",0);
						found = true;
						break;
					}
				}
				catch (ParallelException e) {  // cannot get here
					e.printStackTrace();
					throw new Error("insanity");
				}				
      }
      // end main iteration loop
			
			// restore train data/labels
			p.put("ffnn.traindata", all_train_data);
			p.put("ffnn.trainlabels", all_train_labels);
			
      return new PairObjDouble(x, fx);
    }

  }
}

