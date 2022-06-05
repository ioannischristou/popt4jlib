package popt4jlib.NumberPartitioning;

import java.io.Serializable;
//import popt4jlib.PoolableObjectIntf;
import parallel.TaskObject;
import parallel.ThreadSpecificComparableTaskObject;


/**
 * Encapsulates the requirement for calling the <CODE>release()</CODE> method of
 * the <CODE>FNPTask2</CODE> object, from a specific thread (the 
 * one that created the <CODE>FNPTask2</CODE> object that it holds, as well as 
 * the object itself). Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class ReleaseFNPTask2Object implements ThreadSpecificComparableTaskObject {
  // private final static long serialVersionUID=123456789L;
	private FNPTask2 _obj;  // the FNPTask2 object that must be released
	private transient int _tid;  // the thread-id from which it must be released  
	                             // (as it belongs to the ThreadLocal object pool 
	                             // of that thread) is made transient since there
	                             // is no transfer of threads between JVMs
	
	private boolean _isDone=false;  // unused
	
	// pool-related data
	// itc-20211009: modified below two fields even though such objects are never
	// meant to be serialized.
	private transient ReleaseFNPTask2ObjectPool _pool=null;
  private transient boolean _isUsed=true;  // itc-20211009: used to be false

	private static long _totalNumObjs = 0;

	
  /**
   * returns a managed object if space exists for the current thread, else
   * creates a new unmanaged object. This method forms the only public
   * interface method for getting a new object (the programmer has no control
   * over whether the object will come from the current thread's local pool
   * or via a call to <CODE>new ReleaseFNPTask2Object(o,tid);</CODE>.)
   * @param fnptask FNPTask2
   * @param tid int
   * @return ReleaseFNPTask2Object
   */
  static ReleaseFNPTask2Object newInstance(FNPTask2 fnptask, int tid) {
    return ReleaseFNPTask2ObjectPool.getObject(fnptask, tid);
  }

	
	ReleaseFNPTask2Object(FNPTask2 obj, int threadid) {
		_obj = obj;
		_tid = threadid;
		if (FNPTask2._DO_COLLECT_STATS) {
			// /*
			synchronized (ReleaseFNPTask2Object.class) {
				++_totalNumObjs;
			}
			// */
		}
	}
	
	
	void setData(FNPTask2 obj, int threadid) {
    if (_isUsed) {
			_obj = obj;
			_tid = threadid;
		}
    else {  
      /* replace NullPointerException with Error
      // force a NullPointerException for debugging the pooling mechanism
      Integer null_y=null;
      _tid = null_y.intValue();
			*/
			throw new Error("ReleaseFNPTask2Object.setData(): _isUsed is false?");
    }
	}
	
	
	FNPTask2 getFNPTask2() {
		return _obj;
	}
	
	
	public int getThreadIdToRunOn() {
		return _tid;
	}
	
	
  /**
   * indicate item is available for re-use by Object-Pool to which it belongs,
   * and resets its data IFF it is a managed object.
	 */
	public void release() {
    if (_pool!=null) {
			if (_isUsed) {
				_isUsed=false;
				_isDone = false;
				// indicate to pool the "return" of the object
				_pool.returnObjectToPool(this);
			}
	    else {  
        /* itc-20211009: replace NullPointerException with Error
        // force a NullPointerException for debugging the pooling mechanism
		    Integer null_y=null;
			  _tid = null_y.intValue();
				*/
				throw new Error("ReleaseFNPTask2Object.release(): _isUsed is false?");
			}
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
  void setIsUsed() {
    _isUsed=true;
  }
  /**
   * this constructor is to be used only from the 
	 * <CODE>ReleaseFNPTask2ObjectPool</CODE> for constructing managed objects 
	 * (re-claimable ones).
   * @param pool ReleaseFNPTask2ObjectPool
   */
  ReleaseFNPTask2Object(ReleaseFNPTask2ObjectPool pool) {
    _pool=pool;
    _isUsed=false;
		_tid = 0;
		_obj=null;
		if (FNPTask2._DO_COLLECT_STATS) {
			// /*
			synchronized (ReleaseFNPTask2Object.class) {
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
		if (other instanceof FNPTask2) return 1;  // FNPTask2 objects have priority
		/*
		if (other instanceof FNPTask2) {
			FNPTask2 f = (FNPTask2) other;
			return -f.compareTo(this);
		}
		*/
		else {
			ReleaseFNPTask2Object orfnptask = (ReleaseFNPTask2Object) other;
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
    if (other==null || other instanceof ReleaseFNPTask2Object == false) 
			return false;
    ReleaseFNPTask2Object o = (ReleaseFNPTask2Object) other;
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
			int tid = 
				(int)((popt4jlib.IdentifiableIntf) Thread.currentThread()).getId();
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
			// 1. release the FNPTask2
			_obj.release();
			// 2. release myself!
			release();
		}
		catch (Exception e) {
			System.err.println("ReleaseFNPTask2Object is running on a Thread other "+
				                 "than the one in which it was created");
		}
		return null;
	}

	
  static synchronized long getTotalNumObjs() {
    return _totalNumObjs;
  }

}
