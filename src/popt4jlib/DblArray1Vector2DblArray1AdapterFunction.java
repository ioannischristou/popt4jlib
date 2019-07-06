package popt4jlib;
import java.util.HashMap;

/**
 * The class is a wrapper class for FunctionIntf objects, that require 
 * <CODE>double[]</CODE> arguments. This function accepts as argument 
 * <CODE>DblArray1Vector</CODE> objects instead, and calls the underlying 
 * function to be evaluated with the underlying vector's data array.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1Vector2DblArray1AdapterFunction implements FunctionIntf {
	private FunctionIntf _f;
	
	
	/**
	 * single constructor.
	 * @param f FunctionIntf
	 */
	public DblArray1Vector2DblArray1AdapterFunction(FunctionIntf f) {
		_f = f;
	}
	
	
	/**
	 * calls the underlying function's <CODE>eval()</CODE> method, on the arg's
	 * underlying data array.
	 * @param x Object must be DblArray1Vector
	 * @param params HashMap
	 * @return double the value of the evaluation
	 */
	public double eval(Object x, HashMap params) {
		double[] xarg = ((DblArray1Vector) x).get_x();
		return _f.eval(xarg, params);
	}
}
