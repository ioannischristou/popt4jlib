package popt4jlib.MSSC1D;

import java.util.*;

/**
 * the Cluster class represents a set of integers that together belong to a 
 * cluster, and provides methods for presenting 1-order statistics about this
 * set.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class Cluster {
  private TreeSet _set;  // Set<Integer ind>
  private int _min;
  private int _max;
  private double _avg;
  private double _sum;
  private double _val;
  private boolean _isDirty;

	
	/**
	 * public no-arg constructor.
	 */
  public Cluster() {
    _set = new TreeSet();
    _min = Integer.MAX_VALUE;
    _max = Integer.MIN_VALUE;
    _avg=Double.MAX_VALUE;
    _sum=Double.MAX_VALUE;
    _val=Double.MAX_VALUE;
    _isDirty=true;
  }


	/**
	 * public 1-arg constructor.
	 * @param numbers Set // Set&lt;Integet&gt;
	 */
  public Cluster(Set numbers) {
    _set = new TreeSet(numbers);
    _min = Integer.MAX_VALUE;
    _max = Integer.MIN_VALUE;
    _avg=Double.MAX_VALUE;
    _sum=Double.MAX_VALUE;
    _val=Double.MAX_VALUE;
    _isDirty=true;
    if (_set.size()>0) {
			/* as set is a TreeSet, computation below is useless
      Iterator it = _set.iterator();
      while (it.hasNext()) {
        int iv = ((Integer) it.next()).intValue();
        if (_min> iv) _min = iv;
        else if (_max<iv) _max = iv;
      }
			*/
			_min = ((Integer)_set.first()).intValue();
			_max = ((Integer)_set.last()).intValue();
    }
  }

	
	/**
	 * adds the argument to the cluster.
	 * @param i Integer
	 */
  public void add(Integer i) {
    _set.add(i);
    int iv = i.intValue();
    if (iv<_min) _min = iv;
    if (iv>_max) _max = iv;
    _isDirty=true;
  }


	/**
	 * adds all numbers in the set to the cluster.
	 * @param s Set  // Set&lt;Integer&gt;
	 * @throws CException 
	 */
  public void addSet(Set s) throws CException {
    // add Set<Integer>
    Iterator it = s.iterator();
    while (it.hasNext()) {
      Integer i = (Integer) it.next();
      add(i);
    }
  }


	/**
	 * return the minimum value held in this cluster.
	 * @return int
	 */
  public int getMin() {
    return _min;
  }


	/**
	 * return the maximum value held in this cluster.
	 * @return int
	 */
  public int getMax() {
    return _max;
  }


	/**
	 * return an iterator to the numbers in this cluster.
	 * @return Iterator
	 */
  public Iterator iterator() { return _set.iterator(); }


	/**
	 * check if this cluster is feasible according to the sum-of-intra-distance 
	 * values from the center of the cluster constraint whose threshold value is
	 * found in the input <CODE>Params</CODE> argument.
	 * @param p Params
	 * @return true iff this cluster is feasible for the input constraint.
	 */
  public boolean isFeasible(Params p) {
    boolean f = false;
    try {
      f = (avg(p) <= p.getP());
    }
    catch (CException e) {
      // no-op
    }
    return f;
  }


	/**
	 * evaluate the MSSC value of this cluster according to the distance metric
	 * specified in the input <CODE>Params</CODE> object.
	 * @param p Params
	 * @return double
	 */
  public double evaluate(Params p) {
    if (_isDirty==false) return _val;  // use cache
    sum(p);
    // _avg = _sum / (double) _set.size();
    Iterator it = _set.iterator();
    double v = 0.0;
    while (it.hasNext()) {
      int ind = ( (Integer) it.next()).intValue();
      double vind = p.getSequenceValueAt(ind);
      if (p.getMetric()==Params._L1)
        v += Math.abs(vind - _avg);
      else v += (vind - _avg)*(vind - _avg);
    }
    _val = v;
    _isDirty = false;
    return v;
  }


	/**
	 * method acts as the <CODE>evaluate(p)</CODE> method, but first also checks
	 * for feasibility of the cluster, and if found infeasible, returns 
	 * <CODE>Double.POSITIVE_INFINITY</CODE>.
	 * @param p Params
	 * @return double
	 */
  public double evaluateWF(Params p) {
    if (isFeasible(p)==false) return Double.POSITIVE_INFINITY;
    else return evaluate(p);
  }


  private double sum(Params p) {
    if (_isDirty==false) return _sum;  // use cache
    double v = 0.0;
    Iterator it = _set.iterator();
    while (it.hasNext()) {
      int ind = ( (Integer) it.next()).intValue();
      double vind = p.getSequenceValueAt(ind);
      v += vind;
    }
    _sum = v;
    _avg = _sum/(double) _set.size();
    return v;
  }


  private double avg(Params p) throws CException {
    try {
			sum(p);
      return _avg;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new CException("avg problem");
    }
  }
}

