package popt4jlib;

import java.util.HashMap;
import java.util.ArrayList;


/**
 * The class is a wrapper for function objects that maintains a finite cache
 * that kicks out the oldest (argument,value) pair that was inserted in the 
 * cache if there is no more space available. Whenever the argument is found in 
 * the cache, the associated value is returned, otherwise it is computed using
 * the wrapped function, and stored in a ThreadLocal map.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FiniteLRUFunctionCache implements FunctionIntf {
	final private FunctionIntf _f;
	final private HashMap _params;
	
	final private int _maxSz;
	
	// statistics: 
	private long _hits = 0;
	private long _misses = 0;
	private final boolean _KEEP_STATS = true;
	
	
	private static ThreadLocal _tlArgsArr = new ThreadLocal() {
	  protected ArrayList initialValue() {
			return new ArrayList();
		}
	};
	
	private static ThreadLocal _tlArgsArrInd = new ThreadLocal() {
	  protected Integer initialValue() {
			return new Integer(-1);
		}
	};
	
	private static ThreadLocal _tlCacheMap = new ThreadLocal() {
	  protected HashMap initialValue() {
			return new HashMap();
		}
	};

	
	/**
	 * sole constructor.
	 * @param f FunctionIntf the function to wrap.
	 * @param params HashMap the parameters to use for each function evaluation
	 * @param maxCacheSize int the maximum cache size that each thread may hold
	 */
	public FiniteLRUFunctionCache(FunctionIntf f, HashMap params, 
		                            int maxCacheSize) {
		_f = f;
		_params = params;
		_maxSz = maxCacheSize;	
	}
	

  /**
	 * returns the value stored in the cache if the computation for the arg has
	 * been done before, else calls the wrapped function, and stores the key and
	 * value pair in the cache, kicking out the oldest existing key-value pair if
	 * necessary.
   * @param arg Object
   * @param unusedParams HashMap unused
   * @return double 
   */
  public double eval(Object arg, HashMap unusedParams) {
		if (_maxSz==0) {
			incrementMisses();
			return _f.eval(arg, _params);
		}
		HashMap cache = (HashMap) _tlCacheMap.get();
		if (cache.containsKey(arg)) {
			incrementHits();
			return ((Double)cache.get(arg)).doubleValue();
		}
		// else must do the computation
		incrementMisses();
		double res = _f.eval(arg, _params);
		int insInd = nextPos(((Integer)_tlArgsArrInd.get()).intValue());
		ArrayList args = (ArrayList)_tlArgsArr.get();
		if (insInd>=args.size()) {
			args.add(arg);
		}
		else {
			Object oldarg = args.get(insInd);
			cache.remove(oldarg);
			args.set(insInd, arg);
		}
		cache.put(arg, new Double(res));
		return res;
	}
	
	
	private void incrementHits() {
		if (_KEEP_STATS) {
			synchronized(this) {
				++_hits;
			}
		}
	}
	
	
	private void incrementMisses() {
		if (_KEEP_STATS) {
			synchronized(this) {
				++_misses;
			}
		}
	}
	
	
	public synchronized long getHits() {
		return _hits;
	}
	
	
	public synchronized long getMisses() {
		return _misses;
	}
	
	
	private int nextPos(int i) {
		if (i==_maxSz-1) return 0;
		else return i+1;
	}
}

