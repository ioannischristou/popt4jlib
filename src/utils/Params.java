package utils;

import java.util.*;
import java.io.Serializable;

/**
 * helper class allows searching parameters in a map hierarchically by name.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
public class Params implements Serializable {
	// private static final long serialVersionUID=...;
  private HashMap _p;


  /**
   * mainly used from sub-classes.
   * @param params HashMap
   * @param lightweight boolean if true, no copy of the params arg. will be made
   */
  protected Params(HashMap params, boolean lightweight) {
    if (lightweight) _p = params;
    else _p = new HashMap(params);
  }


  /**
   * public constructor. Makes a copy of the HashMap.
   * @param p HashMap
   */
  public Params(HashMap p) {
    _p = new HashMap(p);
  }


	/**
	 * return the underlying map of this object.
	 * @return HashMap
	 */
	public HashMap getParamsMap() {
		return _p;
	}
	

  /**
   * get an Integer object corresponding to the passed &lt;name&gt; argument.
   * It searches the keys it knows for an appearance of &lt;name&gt;, but if it
   * cannot find one, it starts dropping parts of the &lt;name&gt; (separated by
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
      List comps = new ArrayList();
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        for (int i=0; i<comps.size(); i++) {
          String sname = (String) comps.get(i);
          sname += "."+token;
          comps.set(i, sname);
        }
        comps.add(token);
      }
      for (int j=0; j<comps.size(); j++) {
        val = _p.get((String) comps.get(j));
        if (val!=null) break;
      }
    }
    if (val==null) return null;
    if (val instanceof Integer) return (Integer) val;
		else if (val instanceof Long) return new Integer((int) val);
		else if (val instanceof Double) {
      double v = ((Double) val).doubleValue();
      if (Double.compare(Math.rint(v),v)==0) 
				return new Integer((int) Math.round(v));
      else throw new IllegalArgumentException("double argument is not an int");
    }
    else throw new IllegalArgumentException("argument is not an int");
  }

	
  /**
   * same logic as in <CODE>getInteger(name)</CODE> but for longs this time.
   * @param name String
   * @throws IllegalArgumentException
   * @return Long
   */
  public Long getLong(String name) throws IllegalArgumentException {
    Object val = _p.get(name);
    if (val==null) {
      StringTokenizer st = new StringTokenizer(name, ".");
      List comps = new ArrayList();
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        for (int i=0; i<comps.size(); i++) {
          String sname = (String) comps.get(i);
          sname += "."+token;
          comps.set(i, sname);
        }
        comps.add(token);
      }
      for (int j=0; j<comps.size(); j++) {
        val = _p.get((String) comps.get(j));
        if (val!=null) break;
      }
    }
    if (val==null) return null;
    if (val instanceof Long || val instanceof Integer) return (Long) val;
		else if (val instanceof Double) {
      double v = ((Double) val).doubleValue();
      if (Double.compare(Math.rint(v),v)==0) {
				long r = Math.round(v);
				return new Long(r);
			}
      else throw new IllegalArgumentException("double argument is not a long");			
		}
    else throw new IllegalArgumentException("argument is not a long");
  }


  /**
   * same logic as in <CODE>getInteger(name)</CODE> but for doubles this time.
	 * Any number object is converted to double.
   * @param name String
   * @throws IllegalArgumentException
   * @return Double
   */
  public Double getDouble(String name) throws IllegalArgumentException {
    Object val = _p.get(name);
    if (val==null) {
      StringTokenizer st = new StringTokenizer(name, ".");
      List comps = new ArrayList();
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        for (int i=0; i<comps.size(); i++) {
          String sname = (String) comps.get(i);
          sname += "."+token;
          comps.set(i, sname);
        }
        comps.add(token);
      }
      for (int j=0; j<comps.size(); j++) {
        val = _p.get((String) comps.get(j));
        if (val!=null) break;
      }
    }
    if (val==null) return null;
    if (val instanceof Number) return new Double(((Number) val).doubleValue());
    else throw new IllegalArgumentException("argument is not a double");
  }


  /**
   * same logic as in <CODE>getInteger(name)</CODE> but for booleans this time.
	 * Integer values are also treated as boolean (0 meaning false).
   * @param name String
   * @throws IllegalArgumentException
   * @return Boolean
   */
  public Boolean getBoolean(String name) throws IllegalArgumentException {
    Object val = _p.get(name);
    if (val==null) {
      StringTokenizer st = new StringTokenizer(name, ".");
      List comps = new ArrayList();
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        for (int i=0; i<comps.size(); i++) {
          String sname = (String) comps.get(i);
          sname += "."+token;
          comps.set(i, sname);
        }
        comps.add(token);
      }
      for (int j=0; j<comps.size(); j++) {
        val = _p.get((String) comps.get(j));
        if (val!=null) break;
      }
    }
    if (val==null) return null;
    else if (val instanceof Boolean) return (Boolean) val;
    else if (val instanceof Integer) {
      return ((Integer) val).intValue()==0 ? 
				       new Boolean(false) : new Boolean(true);
    }
    else throw new IllegalArgumentException("argument is not a boolean");
  }

	
  /**
   * same logic as in <CODE>getInteger(name)</CODE> but for strings this time.
   * @param name String
   * @throws IllegalArgumentException
   * @return String
   */
  public String getString(String name) throws IllegalArgumentException {
    Object val = _p.get(name);
    if (val==null) {
      StringTokenizer st = new StringTokenizer(name, ".");
      List comps = new ArrayList();
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        for (int i=0; i<comps.size(); i++) {
          String sname = (String) comps.get(i);
          sname += "."+token;
          comps.set(i, sname);
        }
        comps.add(token);
      }
      for (int j=0; j<comps.size(); j++) {
        val = _p.get((String) comps.get(j));
        if (val!=null) break;
      }
    }
    if (val==null) return null;
    else if (val instanceof String) return (String) val;
    else throw new IllegalArgumentException("argument is not a String");
  }


  /**
   * same logic as in <CODE>getInteger(name)</CODE>, but this time it just 
	 * returns whatever object best matches the input &lt;name&gt;.
   * @param name String
   * @return Object null if no ending part of name that is separated from the
	 * rest of name by a period (".") is contained in the map.
   */
  public Object getObject(String name) {
    Object val = _p.get(name);
    if (val==null) {
      StringTokenizer st = new StringTokenizer(name, ".");
      List comps = new ArrayList();
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        for (int i=0; i<comps.size(); i++) {
          String sname = (String) comps.get(i);
          sname += "."+token;
          comps.set(i, sname);
        }
        comps.add(token);
      }
      for (int j=0; j<comps.size(); j++) {
        val = _p.get((String) comps.get(j));
        if (val!=null) break;
      }
    }
    return val;
  }
}
