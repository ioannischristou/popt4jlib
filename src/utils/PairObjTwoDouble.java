/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utils;

/**
 * utility class extends PairObjDouble with a second double. Used in the 
 * <CODE>tests.sic</CODE> package to return both the optimal value of a function
 * as well as a lower bound of it.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PairObjTwoDouble extends PairObjDouble {
	private double _secondVal;
	
	
	public PairObjTwoDouble(Object arg, double val, double secondval) {
		super(arg, val);
		_secondVal = secondval;
	}
	
	
	public double getSecondDouble() {
		return _secondVal;
	}
	
  /**
   * comparison based on both the value (<CODE>_val,_secondVal</CODE>) objects.
   * @param o Object
   * @return boolean
   */
  public boolean equals(Object o) {
    if (o==this) return true;
    if (o==null) return false;
    try {
      PairObjTwoDouble dd = (PairObjTwoDouble) o;
      return Double.compare(_val, dd._val)==0 && 
				     Double.compare(_secondVal, dd._secondVal)==0;
    }
    catch (ClassCastException e) {
      return false;
    }
  }


  /**
   * as in PairIntDouble, but in case of equality, compare second val as well.
   * @param o Object
   * @return int
   */
  public int compareTo(Object o) {
    if (o==this) return 0;
    PairObjTwoDouble c = (PairObjTwoDouble) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    //if (_val < c._val) return -1;
    //else if (_val == c._val) return 0;
    //else return 1;
    int comp = Double.compare(_val, c._val);
		if (comp!=0) return comp;
		else return Double.compare(_secondVal, c._secondVal);
  }
	
}

