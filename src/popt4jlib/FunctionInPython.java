package popt4jlib;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


/**
 * class allows a function written in Python to be invoked from popt4jlib. 
 * Particularly useful for users that know how to write Python 2.7 code but are
 * not Java programmers. The user simply needs to write their function in 
 * Python 2.7, using as argument to the function a var-arg array of floats, and 
 * return a float, and pass the name of the file in which the function is 
 * written, together with the name of the function to this class constructor.
 * As an example function, see the file PyRos.py in the top-level directory of 
 * this project that actually defines the Rosenbrock function in any number of
 * dimensions.
 * The popt4jlib uses the Jython-2.7.0.jar stand-alone library to evaluate the
 * Python function and optimize it using any of the meta-heuristic algorithms
 * implemented herein.
 * Notice that this is expected to be orders of magnitude slower than the
 * case where the function is written and compiled in Java. ALSO NOTE THAT THIS
 * IS THE ONLY CLASS THAT REQUIRES JAVA6 OR LATER (the javax.script package was
 * introduced in JDK 6).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FunctionInPython implements FunctionIntf {
	private Invocable _inv = null;
	private String _funcName = null;
	
	
	/**
	 * sole constructor.
	 * @param modulefilename String full path name of the file containing the 
	 * function to be used.
	 * @param functionname String the name of the Python function to use.
	 * @throws IOException
	 * @throws ScriptException 
	 */
	public FunctionInPython(String modulefilename, String functionname) 
	  throws IOException, ScriptException {
    //create a script engine manager
    ScriptEngineManager manager = new ScriptEngineManager();
    //create a PythonScript engine
    ScriptEngine engine = manager.getEngineByName("python");
    engine.eval(Files.newBufferedReader(Paths.get(modulefilename), 
			                                  StandardCharsets.UTF_8));
    _inv = (Invocable)engine;
		_funcName = functionname;
	}
	
	
	/**
	 * evaluate the Python function provided by file-name and function-name in the 
	 * constructor.
	 * @param x Object
	 * @param map HashMap unused
	 * @return double
	 * @throws IllegalArgumentException 
	 */
	public double eval(Object x, HashMap map) throws IllegalArgumentException {
		try {
			Object[] arg = null;
			if (x instanceof VectorIntf) {
				final int len = ((VectorIntf) x).getNumCoords();
				arg = new Object[len];
				for (int i=0; i<len; i++) arg[i] = ((VectorIntf)x).getCoord(i);
			}
			else if (x instanceof double[]) {
				final int len = ((double[])x).length;
				arg = new Object[len];				
				for (int i=0; i<len; i++) arg[i] = ((double[])x)[i];
			}
			else 
				throw new IllegalArgumentException("FunctionInPython.eval(): x is "+
				                                   "neither double[] nor VectorIntf");
			Object res = _inv.invokeFunction(_funcName, arg);
			return Double.parseDouble(res.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("FunctionInPython.eval("+x+
				                                 ",null) failed");
		}
	}
}
