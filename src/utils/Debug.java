package utils;

/**
 * A class to help in debugging of the code. Depending on the debug level,
 * set for each class wishing to be debugged separately, the system may
 * execute extra code to help with debugging (printing more information, or
 * execute more sanity checks etc.) The code is normally used with constants
 * bit information found in Constants class.
 * Only up to 64 bits (the length of a long) can be monitored for debugging.
 * The class is thread-safe (re-entrant)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Debug {
  private static long _debugBits=0;
  private static long _savedBits=0;
  private static long _toggle=1;


  /**
   * Query method: return non-zero iff the input debug bit is set
   * @param bit long
   * @return long
   */
  public synchronized static long debug(long bit) {
    return (_debugBits & bit);
  }


  /**
   * remove the input bit from the debug bits of this class.
   * @param bit long
   */
  public synchronized static void resetDebugBit(long bit) {
    _debugBits &= ~bit;
  }


  /**
   * reset all debug bits
   */
  public synchronized static void resetDebug() {
    _debugBits = 0;
  }


  /**
   * resume debug bits from last saved state
   */
  public synchronized static void resumeDebug() {
    _debugBits = _savedBits;
  }


  /**
   * set the particular debug bits described in the argument. However, if the
   * input argument is zero, RESET ALL debug bits to zero.
   * @param bit long
   */
  public synchronized static void setDebugBit(long bit) {
    if (bit==0) resetDebug();
    else _debugBits |= bit;
  }


  /**
   * alternate between "saving the debug bits and reseting" and "resuming debug
   * bits".
   */
  public synchronized static void toggle() {
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
  private static void saveDebugBits() { _savedBits = _debugBits; }

}

