package utils;

import java.io.*;
import java.util.*;

/**
 * class controlling messaging to various output streams. Used for logging
 * purposes as an alterntative to log4j.
 * The class methods are all thread-safe (i.e. reentrant)
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
public class Messenger {
	/**
	 * _os is the <CODE>PrintStream</CODE> on which this <CODE>Messenger</CODE> 
	 * object sends its messages.
	 */
  private PrintStream _os=null;
	/**
	 * _dbgLvl declared volatile in a manner that is consistent even for JDK1.4
	 * memory model. Default value is <CODE>Integer.MAX_VALUE</CODE> for printing
	 * all messages of any debug level.
	 */
  private volatile int _dbgLvl=Integer.MAX_VALUE;  // default: print all
	/**
	 * all <CODE>Messenger</CODE> objects "live" in this map, which holds them by
	 * name. Default messenger is named "default", and goes to 
	 * <CODE>System.err</CODE> (stderr) print stream.
	 */
  private static HashMap _instances=new HashMap();  // map<String name, Messenger m>


  /**
   * private constructor in accordance with the Singleton(s) Design Pattern
   * @param os OutputStream
   * @throws IllegalArgumentException if the os argument is null
   */
  private Messenger(OutputStream os) throws IllegalArgumentException {
    if (os==null) throw new IllegalArgumentException("Messenger.ctor(): null arg");
    _os = new PrintStream(os);
  }


  /**
   * close the PrintStream stream associated with this Messenger, flushing any
   * unwritten content before.
   */
  public void close() {
    if (_os != null) {
      _os.flush();
      _os.close();
      _os = null;
    }
  }


  protected void finalize() throws Throwable {
    try {
      if (_os != null) {
        _os.flush();
        _os.close();
        _os = null;
      }
    }
    finally {
      super.finalize();  // recommended practice
    }
  }


  /**
   * get the default Messenger object
   * @return Messenger
   */
  public synchronized static Messenger getInstance() {
    Messenger instance = (Messenger) _instances.get("default");
    if (instance==null) {
      instance = new Messenger(System.err);
      _instances.put("default", instance);
    }
    return instance;
  }


  /**
   * get the Messenger associated with the given name
   * @param name String
   * @return Messenger
   */
  public synchronized static Messenger getInstance(String name) {
    Messenger instance = (Messenger) _instances.get(name);
    return instance;
  }


  /**
   * associate a Messenger with the given name with the PrintStream passed in.
   * @param name String
   * @param os OutputStream
   */
  public synchronized static void setInstance(String name, OutputStream os) {
    _instances.put(name, new Messenger(os));
  }


  /**
   * set a "debug level" to be later used for printing out messages according
   * to the debug level of the message. Not synchronized since 
	 * <CODE>_dbgLvl</CODE> is volatile, and is only read/written, not incremented
	 * or modified in other ways.
   * @param lvl int
   */
  public void setDebugLevel(int lvl) {
    _dbgLvl = lvl;
  }
	

	/**
	 * get the current "debug level" of this Messenger object.
	 * @return int
	 */
	public int getDebugLvl() {
		return _dbgLvl;
	}
	

  /**
   * sends the msg to the PrintStream of this Messenger iff the debug level lvl
   * is less than or equal to the debug level set by a prior call to
   * <CODE>setDebugLevel(dlvl)</CODE>. When the debug-level argument is not 
	 * enough to get printed, the method does not synchronize (though the access
	 * to _dbgLvl is essentially a "memory barrier".)
   * @param msg String
   * @param lvl int
   */
  public void msg(String msg, int lvl) {
    if (lvl <= _dbgLvl) {
			synchronized (this) {
				if (_os!=null) {
		      _os.println(msg);
				  _os.flush();
				}
			}
		}
  }
}

