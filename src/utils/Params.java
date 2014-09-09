package utils;

import java.util.*;


/**
 * helper class that allows searching parameters hierarchically by name
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Params {
  private Hashtable _p;


  /**
   * mainly used from sub-classes.
   * @param params Hashtable
   * @param lightweight boolean if true, no copy of the params arg. will be made
   */
  protected Params(Hashtable params, boolean lightweight) {
    if (lightweight) _p = params;
    else _p = new Hashtable(params);
  }


  /**
   * public constructor. Makes a copy of the Hashtable
   * @param p Hashtable
   */
  public Params(Hashtable p) {
    _p = new Hashtable(p);
  }


  /**
   * get an Integer object corresponding to the passed <name> argument.
   * It searches the keys it knows for an appearance of <name>, but if it
   * cannot find one, it starts dropping parts of the <name> (separated by a
   * "." character) from left to right, until it finds a match. This match must
   * then be "mappable" to an int, else it throws IllegalArgumentException. If
   * no match can be found, it returns null.
   * @param name String
   * @throws IllegalArgumentException
   * @return Integer
   */
  public Integer getInteger(String name) throws IllegalArgumentException {
    Object val = _p.get(name);
    if (val==null) {
      StringTokenizer st = new StringTokenizer(name, ".");
      Vector comps = new Vector();
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        for (int i=0; i<comps.size(); i++) {
          String sname = (String) comps.elementAt(i);
          sname += "."+token;
          comps.set(i, sname);
        }
        comps.addElement(token);
      }
      for (int j=0; j<comps.size(); j++) {
        val = _p.get((String) comps.elementAt(j));
        if (val!=null) break;
      }
    }
    if (val==null) return null;
    if (val instanceof Integer) return (Integer) val;
    else if (val instanceof Double) {
      double v = ((Double) val).doubleValue();
      if (Math.rint(v)==v) return new Integer((int) Math.round(v));
      else throw new IllegalArgumentException("double argument is not an int");
    }
    else throw new IllegalArgumentException("argument is not an int");
  }


  /**
   * same logic as in getInteger(name) but for doubles this time.
   * @param name String
   * @throws IllegalArgumentException
   * @return Double
   */
  public Double getDouble(String name) throws IllegalArgumentException {
    Object val = _p.get(name);
    if (val==null) {
      StringTokenizer st = new StringTokenizer(name, ".");
      Vector comps = new Vector();
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        for (int i=0; i<comps.size(); i++) {
          String sname = (String) comps.elementAt(i);
          sname += "."+token;
          comps.set(i, sname);
        }
        comps.addElement(token);
      }
      for (int j=0; j<comps.size(); j++) {
        val = _p.get((String) comps.elementAt(j));
        if (val!=null) break;
      }
    }
    if (val==null) return null;
    if (val instanceof Integer) return new Double(((Integer) val).doubleValue());
    else if (val instanceof Double) {
      return (Double) val;
    }
    else throw new IllegalArgumentException("argument is not a double");
  }


  /**
   * same logic as in getInteger(name) but for booleans this time.
   * @param name String
   * @throws IllegalArgumentException
   * @return Boolean
   */
  public Boolean getBoolean(String name) throws IllegalArgumentException {
    Object val = _p.get(name);
    if (val==null) {
      StringTokenizer st = new StringTokenizer(name, ".");
      Vector comps = new Vector();
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        for (int i=0; i<comps.size(); i++) {
          String sname = (String) comps.elementAt(i);
          sname += "."+token;
          comps.set(i, sname);
        }
        comps.addElement(token);
      }
      for (int j=0; j<comps.size(); j++) {
        val = _p.get((String) comps.elementAt(j));
        if (val!=null) break;
      }
    }
    if (val==null) return null;
    else if (val instanceof Boolean) return (Boolean) val;
    else if (val instanceof Integer) {
      return ((Integer) val).intValue()==0 ? new Boolean(false) : new Boolean(true);
    }
    else throw new IllegalArgumentException("argument is not a boolean");
  }


  /**
   * same logic as in getInteger(name), but this time it just returns whatever
   * object best matches the input <name>.
   * @param name String
   * @return Object
   */
  public Object getObject(String name) {
    Object val = _p.get(name);
    if (val==null) {
      StringTokenizer st = new StringTokenizer(name, ".");
      Vector comps = new Vector();
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        for (int i=0; i<comps.size(); i++) {
          String sname = (String) comps.elementAt(i);
          sname += "."+token;
          comps.set(i, sname);
        }
        comps.addElement(token);
      }
      for (int j=0; j<comps.size(); j++) {
        val = _p.get((String) comps.elementAt(j));
        if (val!=null) break;
      }
    }
    return val;
  }
}
