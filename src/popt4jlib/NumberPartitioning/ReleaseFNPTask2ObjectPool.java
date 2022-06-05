package popt4jlib.NumberPartitioning;


/**
 * implements a thread-local pool of <CODE>ReleaseFNPTask2Object</CODE> objects.
 * In this way, there will be no need for invoking the new operator within the
 * running threads requiring such objects, unless a thread at some
 * point runs out of space in its thread-local pool. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class ReleaseFNPTask2ObjectPool {
  /**
   * the maximum number of objects this pool can handle.
   */
  private static int _NUMOBJS = 100000;
  private ReleaseFNPTask2Object[] _pool;
  private int _topAvailPos=-1;


  /**
   * sole public constructor. Creates <CODE>_NUMOBJS</CODE> "empty"
   * <CODE>ReleaseFNPTask2Object</CODE> objects.
   */
  ReleaseFNPTask2ObjectPool() {
    _pool = new ReleaseFNPTask2Object[_NUMOBJS];
		/* itc: 2015-01-15: moved code below to initialize() method
    for (int i=0; i<_NUMOBJS; i++) {
      _pool[i] = new ReleaseFNPTask2Object(this);
    }
		_topAvailPos = _NUMOBJS-1; 
		*/
  }


  /**
   * factory object creation method, first tries to return a managed object from
   * the pool, and if it cannot find one, creates a new (unmanaged) one. The
   * returned object, always has its data members (values) correctly set.
   * @param data FNPTask2 
   * @param threadid int
   * @return ReleaseFNPTask2Object
   */
  static ReleaseFNPTask2Object getObject(FNPTask2 data, int threadid) {
    ReleaseFNPTask2ObjectPool pool = 
			ReleaseFNPTask2ObjectThreadLocalPools.getThreadLocalPool();
    ReleaseFNPTask2Object p = pool.getObjectFromPool();
    if (p!=null) {  // ok, return managed object
      p.setData(data, threadid);
      return p;
    } else  // oops, create new unmanaged object
      return new ReleaseFNPTask2Object(data, threadid);
  }

	
	/**
	 * method is only called from 
	 * <CODE>ReleaseFNPTask2ObjectThreadLocalPools</CODE> right after object 
	 * construction to avoid escaping "this" in the constructor.
	 */
	void initialize() {
    for (int i=0; i<_NUMOBJS; i++) {
      _pool[i] = new ReleaseFNPTask2Object(this);
    }
		_topAvailPos = _NUMOBJS-1; 		
	}
	

  /**
   * return a managed "free" object from the pool, or null if it cannot find
   * one.
   * @return ReleaseFNPTask2Object
   */
  ReleaseFNPTask2Object getObjectFromPool() {
    if (_topAvailPos>=0) { 
			ReleaseFNPTask2Object obj = _pool[_topAvailPos--];
			obj.setIsUsed();
			return obj;
		}
    return null;
  }


  /**
   * only called from <CODE>ReleaseFNPTask2Object.release()</CODE> method.
   * @param dlp ReleaseFNPTask2Object
   */
  void returnObjectToPool(ReleaseFNPTask2Object dlp) {
		_pool[++_topAvailPos] = dlp;
    return;
  }
	
	
	/**
	 * sets the number of objects in the thread-local pools to the size specified.
	 * Must only be called once, before any pool is actually constructed (should
	 * only be called from the ReleaseFNPTask2ObjectThreadLocalPools class).
	 * @param num int
	 * @throws IllegalArgumentException if the argument is &le; 0
	 */
	static void setPoolSize(int num) throws IllegalArgumentException {
		if (num <= 0) 
			throw new IllegalArgumentException("setPoolSize(n): n<=0");
		_NUMOBJS = num;
	}

}
