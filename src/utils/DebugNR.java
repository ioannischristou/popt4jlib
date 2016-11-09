package utils;

import java.util.HashMap;

/**
 * A class to help in debugging of the code. Depending on the debug level,
 * set for each class wishing to be debugged separately, the system may
 * execute extra code to help with debugging (printing more information, or
 * execute more sanity checks etc.) The code is normally used with constants
 * bit information found in Constants class.
 * Only up to 64 bits (the length of a long) can be monitored for debugging for
 * each instance of the class.
 * The class is NOT thread-safe (non-reentrant)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DebugNR {
	private static HashMap _instances = new HashMap();  // map<String name, DebugNR obj>
  private volatile long _debugBits=0;
  private volatile long _savedBits=0;
  private volatile long _toggle=1;

	
	/**
	 * get the "default" DebugNR instance.
	 * @return DebugNR
	 */
	public static synchronized DebugNR getInstance() {
		return getInstance("default");
	}
	
	
	/**
	 * get the DebugNR instance associated with the specified name (normally, the
	 * name of a class).
	 * @param name String
	 * @return DebugNR
	 */
	public static synchronized DebugNR getInstance(String name) {
		DebugNR res = (DebugNR) _instances.get(name);
		if (res==null) {
			res = new DebugNR();
			_instances.put(name, res);
		}
		return res;
	}
	
	
	/**
	 * single constructor (private)
	 */
	private DebugNR() {
		// no-op
	}
	

  /**
   * Query method: return non-zero iff the input debug bit is set
   * @param bit long
   * @return long
   */
  public long debug(long bit) {
    return (_debugBits & bit);
  }


  /**
   * remove the input bit from the debug bits of this class.
   * @param bit long
   */
  public void resetDebugBit(long bit) {
    _debugBits &= ~bit;
  }


  /**
   * reset all debug bits
   */
  public void resetDebug() {
    _debugBits = 0;
  }


  /**
   * resume debug bits from last saved state
   */
  public void resumeDebug() {
    _debugBits = _savedBits;
  }


  /**
   * set the particular debug bits described in the argument. However, if the
   * input argument is zero, RESET ALL debug bits to zero.
   * @param bit long
   */
  public void setDebugBit(long bit) {
    if (bit==0) resetDebug();
    else _debugBits |= bit;
  }


  /**
   * alternate between "saving the debug bits and reseting" and "resuming debug
   * bits".
   */
  public void toggle() {
    if (_toggle==1) {
      // reset state
      saveDebugBits();
      resetDebug();
    }
    else resumeDebug();
    _toggle = (_toggle==1 ? 0 : 1);
  }


  /**
   * save the current debug bits. Previous saved bits are overwritten.
   */
  private void saveDebugBits() { _savedBits = _debugBits; }

}

