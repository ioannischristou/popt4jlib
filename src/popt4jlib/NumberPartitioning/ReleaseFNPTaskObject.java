/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.NumberPartitioning;

import java.io.Serializable;
import popt4jlib.PoolableObjectIntf;
import parallel.TaskObject;
import parallel.ThreadSpecificComparableTaskObject;


/**
 * Encapsulates the requirement for calling the <CODE>release()</CODE> method of
 * the <CODE>PoolableObjectIntf</CODE> interface, from a specific thread (the 
 * one that created the <CODE>FNPTask</CODE> object that it holds, as well as 
 * the object itself). Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class ReleaseFNPTaskObject implements ThreadSpecificComparableTaskObject, PoolableObjectIntf {
  // private final static long serialVersionUID=123456789L;
	private FNPTask _obj;  // the FNPTask object that must be released
	private int _tid;  // the thread-id from which it must be released (as it 
	                   // belongs to the ThreadLocal object pool of that thread) 
	
	private boolean _isDone=false;  // unused
	
	// pool-related data
	// private final static boolean _USE_POOLS=true;  // compile-time flag indicates use of pools or not
  private final static boolean _DO_RESET_ON_RELEASE=false;  // compile-time flag for resetting elems on release
	private ReleaseFNPTaskObjectPool _pool=null;
  private int _poolPos=-1;
  private boolean _isUsed=false;  // redundant init.

	private static long _totalNumObjs = 0;

	
  /**
   * returns a managed object if space exists for the current thread, else
   * creates a new unmanaged object. This method forms the only public
   * interface method for getting a new object (the programmer has no control
   * over whether the object will come from the current thread's local pool
   * or via a call to <CODE>new ReleaseFNPTaskObject(o,tid);</CODE>.)
   * @param fnptask FNPTask
   * @param tid int
   * @return ReleaseFNPTaskObject
   */
  static ReleaseFNPTaskObject newInstance(FNPTask fnptask, int tid) {
    return ReleaseFNPTaskObjectPool.getObject(fnptask, tid);
  }

	
	ReleaseFNPTaskObject(FNPTask obj, int threadid) {
		_obj = obj;
		_tid = threadid;
		if (FNPTask._DO_COLLECT_STATS) {
			// /*
			synchronized (ReleaseFNPTaskObject.class) {
				++_totalNumObjs;
			}
			// */
		}
	}
	
	
	void setData(FNPTask obj, int threadid) {
    if (_isUsed) {
			_obj = obj;
			_tid = threadid;
		}
    else {  // force a NullPointerException for debugging the pooling mechanism
      Integer null_y=null;
      _tid = null_y.intValue();
    }
	}
	
	
	FNPTask getFNPTask() {
		return _obj;
	}
	
	
	public int getThreadIdToRunOn() {
		return _tid;
	}
	
	// PoolableObjectIntf methods below
	
  /**
   * indicate item is available for re-use by Object-Pool to which it belongs,
   * and resets its data IFF it is a managed object, and the 
	 * <CODE>_DO_RESET_ON_RELEASE</CODE> flag is true.
   */
  public void release() {
    if (_pool!=null) {
      _isUsed=false;
			if (_DO_RESET_ON_RELEASE) {
				_obj = null;
				_tid = 0;
			}
			_isDone = false;
      // indicate to pool the "return" of the object
      _pool.returnObjectToPool(this);
    }
  }
  /**
   * return true IFF the object is managed and "currently used", or un-managed.
   * @return boolean
   */
  boolean isUsed() {
    return _isUsed;
  }
	/**
	 * true IFF this object belongs to some pool.
	 * @return true IFF this object belongs to some pool.
	 */
	public boolean isManaged() {
		return _pool!=null;
	}
	/**
	 * returns an unmanaged (ie not belonging to the thread-local object pool)
	 * <CODE>ReleaseFNPTaskObject</CODE> object that is a cloned (deep) copy of 
	 * the data in this object.
	 * Note: this implementation does not support this operation.
	 * @return null
	 */
	public PoolableObjectIntf cloneObject() {
		return null;
	}
  void setIsUsed() {
    _isUsed=true;
  }
  int getPoolPos() {
    return _poolPos;
  }
  /**
   * this constructor is to be used only from the 
	 * <CODE>ReleaseFNPTaskObjectPool</CODE> for constructing managed objects 
	 * (re-claimable ones).
   * @param pool ReleaseFNPTaskObjectPool
   * @param poolpos int
   */
  ReleaseFNPTaskObject(ReleaseFNPTaskObjectPool pool, int poolpos) {
    _pool=pool;
    _poolPos=poolpos;
    _isUsed=false;
		_tid = 0;
		_obj=null;
		if (FNPTask._DO_COLLECT_STATS) {
			// /*
			synchronized (ReleaseFNPTaskObject.class) {
				++_totalNumObjs;
			}
			// */
		}
  }

	
  /**
   * @param other Object
   * @return int
   */
  public int compareTo(Object other) {
		if (other==null) throw new NullPointerException("null arg passed.");
		if (other instanceof FNPTask) return 1;  // FNPTask objects have priority
		/*
		if (other instanceof FNPTask) {
			FNPTask f = (FNPTask) other;
			return -f.compareTo(this);
		}
		*/
		else {
			ReleaseFNPTaskObject orfnptask = (ReleaseFNPTaskObject) other;
			if (_obj.getId() < orfnptask._obj.getId()) return -1;
			else if (_obj.getId()==orfnptask._obj.getId()) return 0;
			else return 1;
		}
  }


  /**
   * required to be compatible with compareTo() so that different objects
   * are not "lost" when inserted into a TreeSet.
   * @param other Object
   * @return boolean
   */
  public boolean equals(Object other) {
    if (other==null || other instanceof ReleaseFNPTaskObject == false) return false;
    ReleaseFNPTaskObject o = (ReleaseFNPTaskObject) other;
    return _obj == o._obj;
  }


  /**
   * returns the _tid value.
   * @return int
   */
  public int hashCode() {
    return _tid;
  }

	// TaskObject methods
	
	/**
   * throws exception (unsupported).
   * @param other TaskObject
   * @throws IllegalArgumentException
   */
  public void copyFrom(TaskObject other) throws IllegalArgumentException {
    throw new IllegalArgumentException("unsupported");
  }

	
	/**
	 * not used.
	 * @return false always.
	 */
	public synchronized boolean isDone() {
		return false;
	}
	
	
	public Serializable run() {
		/*
		synchronized (this) {
			_isDone = true;
		}
		*/
		try {
			// 0. sanity test
			int tid = (int)((popt4jlib.IdentifiableIntf) Thread.currentThread()).getId();
			if (tid!=_tid) throw new parallel.ParallelException("tid!=_tid?");
			// 0.5 wait until task is actually done
			synchronized (_obj) {
				while (_obj.isDone()==false) {
					try {
						_obj.wait();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();  // recommended practice
					}
				}
			}
			// 1. release the FNPTask
			_obj.release();
			// 2. release myself!
			release();
		}
		catch (Exception e) {
			System.err.println("ReleaseFNPTaskObject is running on a Thread other than the one in which it was created");
		}
		return null;
	}

	
  static synchronized long getTotalNumObjs() {
    return _totalNumObjs;
  }

}
