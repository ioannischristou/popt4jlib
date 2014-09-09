package popt4jlib.MSSC1D;

import java.util.*;

public class Cluster {
  private TreeSet _set;  // Set<Integer ind>
  private int _min;
  private int _max;
  private double _avg;
  private double _sum;
  private double _val;
  private boolean _isDirty;

  public Cluster() {
    _set = new TreeSet();
    _min = Integer.MAX_VALUE;
    _max = Integer.MIN_VALUE;
    _avg=Double.MAX_VALUE;
    _sum=Double.MAX_VALUE;
    _val=Double.MAX_VALUE;
    _isDirty=true;
  }


  public Cluster(Set numbers) {
    _set = new TreeSet(numbers);
    _min = Integer.MAX_VALUE;
    _max = Integer.MIN_VALUE;
    _avg=Double.MAX_VALUE;
    _sum=Double.MAX_VALUE;
    _val=Double.MAX_VALUE;
    _isDirty=true;
    if (_set.size()>0) {
      Iterator it = _set.iterator();
      while (it.hasNext()) {
        int iv = ((Integer) it.next()).intValue();
        if (_min> iv) _min = iv;
        else if (_max<iv) _max = iv;
      }
    }
  }

  public void add(Integer i) {
    _set.add(i);
    int iv = i.intValue();
    if (iv<_min) _min = iv;
    if (iv>_max) _max = iv;
    _isDirty=true;
  }


  public void addSet(Set s) throws CException {
    // add Set<Integer>
    Iterator it = s.iterator();
    while (it.hasNext()) {
      Integer i = (Integer) it.next();
      add(i);
    }
  }


  public int getMin() {
    return _min;
  }


  public int getMax() {
    return _max;
  }


  public Iterator iterator() { return _set.iterator(); }


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
      else v += Math.abs(vind*vind - _avg);
    }
    _val = v;
    _isDirty = false;
    return v;
  }


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
      if (p.getMetric()==Params._L1)
        v += vind;
      else v += vind*vind;
    }
    _sum = v;
    _avg = _sum/(double) _set.size();
    return v;
  }


  private double avg(Params p) throws CException {
    try {
      _avg = evaluate(p) / _set.size();
      return _avg;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new CException("avg problem");
    }
  }
}

