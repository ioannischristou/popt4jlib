package parallel;

import java.util.ArrayList;

/**
 * The class is responsible for maintaining a sufficiently large array of
 * <CODE>RegisteredParcel</CODE> objects, and there will be a pool for each
 * thread of execution.
 * In this way, there will be no need for invoking the new operator within the
 * running threads requiring RegisteredParcel objects, unless a thread at some
 * point runs out of space in its thread-local pool. This pool can only be 
 * safely used with the <CODE>BlockingFasterMsgPassingCoordinator</CODE> since
 * the methods of this class do not "release" objects that were initially added
 * in the shared queue from another thread's pool. Even though the other 
 * msg-passing coordinator classes have all their methods synchronized, the
 * idiom cannot be safely used with them: one thread T1 could for example call 
 * on two different <CODE>SimpleFasterMsgPassingCoordinator</CODE>s the 
 * <CODE>sendData()</CODE> method sequentially on each of them, and at the same
 * time two other threads T2 and T3, could be calling in parallel the 
 * <CODE>recvData()</CODE> of each one coordinator respectively. In this case 2 
 * <CODE>RegisteredParcel</CODE> objects belonging to T1's pool would be 
 * simultaneously and without any synchronization be calling the T1 pool's 
 * <CODE>returnObjectToPool()</CODE> method, corrupting at least the pool's
 * <CODE>_min/maxUsedPos</CODE> data members. See the 
 * <CODE>RegisteredParcelPoolFailTest</CODE> for further details on how to show
 * this phenomenon.
 * Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
final class RegisteredParcelPool {
  
	/**
	 * compile-time constant used to check against invalid object releases, in 
	 * debugging.
	 */
	private final static boolean _DO_RELEASE_SANITY_TEST = true;
	/**
	 * compile-time constant used to ensure that no object returned, was "used"
	 * at the time of return.
	 */
	private static final boolean _DO_GET_SANITY_TEST=true;
  private static boolean _ignoreReleaseSanityTest = false;	// auxiliary member to disable test in certain cases
	/**
   * the maximum number of objects this pool can handle.
   */
	private static int _NUMOBJS = 1000;
  private ArrayList _pool;
  private int _maxUsedPos=-1;
	private int _minUsedPos=_NUMOBJS;


  /**
   * sole public constructor. Creates <CODE>_NUMOBJS</CODE> "empty"
   * <CODE>RegisteredParcel</CODE> objects.
   */
  RegisteredParcelPool() {
    _pool = new ArrayList(_NUMOBJS);
		/* itc: 2015-01-15: moved code below to initialize() method
    for (int i=0; i<_NUMOBJS; i++) {
      _pool.add(new RegisteredParcel(this, i));
    }
		*/
  }


  /**
   * factory object creation method, first tries to return a managed object from
   * the pool, and if it cannot find one, creates a new (unmanaged) one. The
   * returned object, always has its data members (values) correctly set.
   * @param fromId int
   * @param toId int
   * @param data Object
   * @return RegisteredParcel
   */
  static RegisteredParcel getObject(int fromid, int toid, Object data) {
    RegisteredParcelPool pool = RegisteredParcelThreadLocalPools.getThreadLocalPool();
    RegisteredParcel p = pool.getObjectFromPool();
    if (p!=null) {  // ok, return managed object
      p.setFromId(fromid);
      p.setToId(toid);
      p.setData(data);
      return p;
    } else  // oops, create new unmanaged object
      return new RegisteredParcel(fromid, toid, data);
  }

	
	/**
	 * method is only called from RegisteredParcelThreadLocalPools right after
	 * this object is constructed to avoid escaping "this" in the constructor.
	 */
	void initialize() {
    for (int i=0; i<_NUMOBJS; i++) {
      _pool.add(new RegisteredParcel(this, i));
    }
	}

	
  /**
   * return an unmanaged "free" object from the pool, or null if it cannot find
   * one.
   * @return RegisteredParcel
   */
  RegisteredParcel getObjectFromPool() {
    if (_maxUsedPos<_NUMOBJS-1) {  // try the right end
      _maxUsedPos++;
      RegisteredParcel dlp = (RegisteredParcel) _pool.get(_maxUsedPos);
			if (_DO_GET_SANITY_TEST && dlp.isUsed()) {
				Integer yI = null;
				System.err.println("getObjectFromPool(): right doesn't work: null ref yI="+yI.intValue());  // force NullPointerException
			}
      dlp.setIsUsed();
			if (_minUsedPos>_maxUsedPos) _minUsedPos = _maxUsedPos;
      return dlp;
    } else {  // try the left end
			if (_minUsedPos>0) {
				_minUsedPos--;
				RegisteredParcel ind = (RegisteredParcel) _pool.get(_minUsedPos);
				if (_DO_GET_SANITY_TEST && ind.isUsed()) {
					Integer yI = null;
					System.err.println("getObjectFromPool(): left doesn't work: null ref yI="+yI.intValue());  // force NullPointerException
				}
				ind.setIsUsed();
				if (_minUsedPos>_maxUsedPos) _maxUsedPos = _minUsedPos;
				return ind;
			}
		}
    return null;
  }


  /**
   * only called from <CODE>RegisteredParcel.release()</CODE> method.
   * @param dlp RegisteredParcel
   */
  void returnObjectToPool(RegisteredParcel dlp) {
		if (_DO_RELEASE_SANITY_TEST && !_ignoreReleaseSanityTest) {
			RegisteredParcelPool pool = RegisteredParcelThreadLocalPools.getThreadLocalPool();
			if (pool!=this) {
				Integer yI = null;
				System.err.println("null ref yI="+yI.intValue());  // force NullPointerException
			}
		}
		// corner case: the returned object was the only one "out-of-the-pool"
		if (_maxUsedPos==_minUsedPos) {
			if (_DO_RELEASE_SANITY_TEST && !_ignoreReleaseSanityTest) {
				if (dlp.getPoolPos()!=_minUsedPos) {
					Integer yI = null;
					System.err.println("null ref yI="+yI.intValue());  // force NullPointerException					
				}
			}
			_maxUsedPos = -1;
			_minUsedPos = _NUMOBJS;
			return;
		}
    if (dlp.getPoolPos()==_maxUsedPos) {
      --_maxUsedPos;
      while (_maxUsedPos>=0 &&
             ((RegisteredParcel)_pool.get(_maxUsedPos)).isUsed()==false)
        --_maxUsedPos;
    }
		if (dlp.getPoolPos()==_minUsedPos) {
			++_minUsedPos;
			while (_minUsedPos<_NUMOBJS &&
						 ((RegisteredParcel)_pool.get(_minUsedPos)).isUsed()==false)
				++_minUsedPos;
		}
    return;
  }
	
	
	/**
	 * sets the number of objects in the thread-local pools to the size specified.
	 * Must only be called once, before any pool is actually constructed (should
	 * only be called from the RegisteredParcelThreadLocalPools class).
	 * @param num int
	 * @throws IllegalArgumentException if the argument is &lte; 0
	 */
	static void setPoolSize(int num) throws IllegalArgumentException {
		if (num <= 0) 
			throw new IllegalArgumentException("setPoolSize(n): n<=0");
		_NUMOBJS = num;
	}
	
	
	/**
	 * must be called before any threads start using the RP-pools. Only used for
	 * debugging purposes.
	 * @param val 
	 */
	static void setIgnoreReleaseSanityTest(boolean val) {
		_ignoreReleaseSanityTest = val;
	}

}

