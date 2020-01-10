package utils;

/**
 * utility class extends PairObjTwoDouble with a third double. Used in the 
 * <CODE>tests.sic</CODE> package to return the optimal value of a function
 * as well as a lower bound of it and the fixed ordering costs at the optimal
 * review period.
 * Notes:
 * <ul>
 * <li> 20191227: added <CODE>hashCode()</CODE> method.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PairObjThreeDouble extends PairObjTwoDouble {
	private double _thirdVal;
	
	
	public PairObjThreeDouble(Object arg, double val, 
		                        double secondval, double thirdval) {
		super(arg, val, secondval);
		_thirdVal = thirdval;
	}
	
	
	public double getThirdDouble() {
		return _thirdVal;
	}
	
	
  /**
   * comparison based on all value (<CODE>_val,_secondVal, _thirdVal</CODE>) 
	 * objects.
   * @param o Object
   * @return boolean
   */
  public boolean equals(Object o) {
    if (o==this) return true;
    if (o==null) return false;
    try {
      PairObjThreeDouble dd = (PairObjThreeDouble) o;
      return Double.compare(_val, dd._val)==0 && 
				     Double.compare(_secondVal, dd._secondVal)==0 && 
				     Double.compare(_thirdVal, dd._thirdVal)==0;
    }
    catch (ClassCastException e) {
      return false;
    }
  }
	

	/**
	 *  computes the hash-code according to Joshua Bloch's suggestions.
	 * @return int
	 */
	public int hashCode() {
		int result = 17;
		long tmplvl = Double.doubleToLongBits(_val);
		int c = (int)(tmplvl ^ (tmplvl >>> 32));
		result = 31*result + c;
		tmplvl = Double.doubleToLongBits(_secondVal);
		c = (int) (tmplvl ^ (tmplvl >>> 32));
		result = 31*result + c;
		tmplvl = Double.doubleToLongBits(_thirdVal);
		c = (int) (tmplvl ^ (tmplvl >>> 32));
		result = 31*result + c;
		return result;		
	}


  /**
   * as in PairIntTwoDouble, but in case of equality, compare third val as well.
   * @param o Object
   * @return int
   */
  public int compareTo(Object o) {
    if (o==this) return 0;
    PairObjThreeDouble c = (PairObjThreeDouble) o;
    // this < c => return -1
    // this == c => return 0
    // this > c => return 1
    //if (_val < c._val) return -1;
    //else if (_val == c._val) return 0;
    //else return 1;
    int comp = Double.compare(_val, c._val);
		if (comp!=0) return comp;
		else {
			comp = Double.compare(_secondVal, c._secondVal);
			if (comp!=0) return comp;
			else {
				return Double.compare(_thirdVal, c._thirdVal);
			}
		}
  }
	
	
	/**
	 * return a String showing the 3 data members of this object.
	 * @return String
	 */
	public String toString() {
		String str = "(arg="+_arg+", firstVal="+_val+
			           ", secondVal="+_secondVal+
			           ", thirdVal="+_thirdVal+
			           ")";
		return str;
	}
	
}

