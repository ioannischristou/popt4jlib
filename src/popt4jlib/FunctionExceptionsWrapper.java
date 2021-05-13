package popt4jlib;

import java.util.HashMap;


/**
 * the class implements a wrapper that catches all possible exceptions during
 * function evaluation, and throws only <CODE>IllegalArgumentException</CODE>.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FunctionExceptionsWrapper implements FunctionIntf {
  // private final static long serialVersionUID = -7654946164692599332L;
	private FunctionIntf _f = null;
	
	
	/**
	 * sole constructor.
	 * @param f FunctionIntf
	 */
	public FunctionExceptionsWrapper(FunctionIntf f) {
		_f = f;
	}
	
	
	/**
	 * wrapper method simply calls the underlying function's <CODE>eval()</CODE>
	 * method, wrapped in a try-catch clause ensuring that only 
	 * <CODE>IllegalArgumentException</CODE> exceptions may be thrown.
	 * @param x Object
	 * @param params HashMap
	 * @return double
	 * @throws IllegalArgumentException if any kind of exception is thrown, it is
	 * converted to this
	 */
	public double eval(Object x, HashMap params) throws IllegalArgumentException {
		try {
			return _f.eval(x, params);
		}
		catch (Exception e) {
			String excstring = "FunctionExceptionsWrapper.eval(x,params): threw "+
				                 e.toString();
			throw new IllegalArgumentException(excstring);
		}
	}
	
}

