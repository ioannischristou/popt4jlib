package utils;

import java.util.*;

/**
 * class controlling random number generation.
 * <p>Title: popt4jlib</p>
 * <p>Description: Parallel optimization library for Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: AIT</p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RndUtil {
  private static RndUtil _instance = null;
  // private static RndUtil _extras[] = null;
  private static Hashtable _extras = null;  // map<Integer id, Random r>
  private static int _curMaxId = 0;
  private Random _random = null;
  private long _seed = 0;  // default seed


  /**
   * get the default RndUtil instance in the JVM.
   * @return RndUtil
   */
  public static synchronized RndUtil getInstance() {
    if (_instance==null) {
      _instance = new RndUtil();
    }
    return _instance;
  }


/*
  public static synchronized RndUtil getInstance(int id) {
    if (_extras==null) addExtraInstances(id+1);
    else if (id >= _extras.length) {
      addExtraInstances(id-_extras.length+1);
    }
    return _extras[id];
  }


  public static synchronized void addExtraInstances(int num) {
    if (_extras==null) {
      _extras = new RndUtil[num];
      for (int i = 0; i < num; i++) {
        _extras[i] = new RndUtil();
        _extras[i]._random = new Random(_instance._seed + i + 1);
      }
    }
    else {
      RndUtil[] tmp = new RndUtil[num+_extras.length];
      int i=0;
      for (; i<_extras.length; i++) tmp[i] = _extras[i];
      for (; i<tmp.length; i++) {
        tmp[i] = new RndUtil();
        tmp[i]._random = new Random(_instance._seed + i + 1);
      }
      _extras = tmp;
    }
  }
*/


  /**
   * return the RndUtil instance with the given id. If it doesn't already exist,
   * it creates one.
   * @param id int
   * @return RndUtil
   */
  public static synchronized RndUtil getInstance(int id) {
    if (_extras==null) {
      _extras = new Hashtable();
      return addExtraInstance(id);
    }
    RndUtil ru = (RndUtil) _extras.get(new Integer(id));
    if (ru!=null) return ru;
    else return addExtraInstance(id);
  }


  /**
   * adds num exta instances.
   * @param num int
   */
  public static synchronized void addExtraInstances(int num) {
    if (_extras==null) _extras = new Hashtable(num);
    for (int i=0; i<num; i++) {
      addExtraInstance(_curMaxId+1);
    }
  }


  /**
   * initializes all Random objects corresponding to each existing instance
   * of the RndUtil class with a different seed (starting with s)
   * @param s long
   */
  public synchronized void setSeed(long s) {
    _seed = s;
    _random = new Random(_seed);
    if (_extras!=null) {
/*
      for (int i=0; i<_extras.size(); i++) {
        _extras[i]._random = new Random(_instance._seed+i+1);
      }
*/
      Iterator it = _extras.keySet().iterator();
      while (it.hasNext()) {
        Integer id = (Integer) it.next();
        RndUtil ru = (RndUtil) _extras.get(id);
				final long ruseed = getInstance().getSeed()+id.intValue()+1;  // used to be _instance.getSeed()+id.intValue()+1
        ru._seed = ruseed;
				ru._random = new Random(ruseed);  
      }
    }
  }


  /**
   * return current seed
   * @return long
   */
  public synchronized long getSeed() {
    return _seed;
  }


  /**
   * return the Random object corresponding to this RndUtil
   * @return Random
   */
  public Random getRandom() {
    return _random;
  }


  /**
   * private constructor in accordance with the Singleton(s) Design Pattern
   */
  private RndUtil() {
    _random = new Random();  // maintain default unrepeatable random behaviour
  }


  /**
   * adds an extra instance with the given id.
   * @param id int
   * @return RndUtil
   */
  private static RndUtil addExtraInstance(int id) {
    RndUtil ru = new RndUtil();
		long ruseed = id+getInstance().getSeed()+1;  // used to be id+_instance.getSeed()+1
    ru._seed = ruseed;
		ru._random = new Random(ruseed);  
    _extras.put(new Integer(id), ru);
    if (_curMaxId<id) _curMaxId = id;
    return ru;
  }
}
