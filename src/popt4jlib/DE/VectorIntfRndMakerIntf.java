/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.DE;

import java.util.Random;
import popt4jlib.OptimizerException;
import popt4jlib.VectorIntf;

/**
 * interface for initializing DE (sub-)populations.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface VectorIntfRndMakerIntf {
	/**
	 * creates a new random vector that will form a member of the initial 
	 * population of a DE run. Parameters such as the dimensionality of the vector
	 * generated must be provided by the implementing classes (such parameters may
	 * be for example specified in the constructors of the implementing classes).
	 * @return VectorIntf
	 * @throws OptimizerException 
	 */
	public VectorIntf createNewRandomVector() throws OptimizerException;
	
	
	/**
	 * same as <CODE>createNewRandomVector()</CODE> but uses the particular random
	 * object to generate the next random vector.
	 * @param r Random
	 * @return VectorIntf
	 * @throws OptimizerException 
	 */
	public VectorIntf createNewRandomVector(Random r) throws OptimizerException;
	
}
