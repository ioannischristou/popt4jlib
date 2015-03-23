/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib;

import java.util.Hashtable;


/**
 * interface specifying that RandomArgMakerIntf objects can be cloned. Useful
 * for speeding up operations when a single object of this type will be used in
 * a thread (and different threads will have different such objects), to create
 * optimization problem arguments. It may also have the ability to return 
 * the current argument (implying state must be kept or be derivable somehow).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface RandomArgMakerClonableIntf extends RandomArgMakerIntf {
	/**
	 * returns a cloned object of this object.
	 * @param p Hashtable 
	 * @return RandomArgMakerClonableIntf the cloned object
	 * @throws OptimizerException 
	 */
	public RandomArgMakerClonableIntf newInstance(Hashtable p) throws OptimizerException;
	/**
	 * return the current argument.
	 * @return Object
	 */
	public Object getCurrentArgument() throws UnsupportedOperationException;
}

