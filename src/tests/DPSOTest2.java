/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import java.util.HashMap;
import popt4jlib.FunctionIntf;
import popt4jlib.PS.DblArray1CMaker;
import popt4jlib.PS.DblArray1ChromosomeVelocityAdder;
import popt4jlib.PS.DblArray1RandomVelocityMaker;
import popt4jlib.PS.DblArray1StdVelocityMaker;
import popt4jlib.PS.RandomChromosomeSelector;
import popt4jlib.RandomChromosomeMakerIntf;

/**
 * yet another test-driver program for the DPSO (Distributed Particle Swarm 
 * Optimization) algorithm illustrating the placement of parameters directly in
 * the params HashMap without reading from properties-file, and also using
 * the random-topology, where the guiding solution for any individual particle
 * is chosen at random among its sub-population.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DPSOTest2 {

	/**
	 * invoke as <CODE>java -cp &lt;classpath&gt; tests.DPSOTest2</CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		HashMap params = new HashMap();
		FunctionIntf my_fun = new QuadraticShiftedFunction();  // replace the QuadraticShiftedFunction with anything you like
		params.put("dpso.function", my_fun);  // this is not really needed
		params.put("dpso.numgens", new Integer(5000));  // indicates that DPSO will run for 5000 major iterations
		params.put("dpso.numthreads", new Integer(1));  // indicates it will use one island (you may call it sub-population) only 
		// where all particles will live on that island (running on one thread)
		params.put("dpso.chromosomelength", new Integer(10));  // indicates the dimensionality of the function to minimize
		params.put("dpso.maxallelevalue", new Double(10.0));  // the upper-bound on the value of any function variable
		params.put("dpso.minallelevalue", new Double(-10.0));  // the lower-bound on the value of any function variable
		params.put("dpso.numinitpop", new Integer(50));  // the number of particles on each island
		params.put("dpso.w", new Double(0.6));  // value of the ù (omega) parameter in the formula  for updating
		// particle velocity; see https://en.wikipedia.org/wiki/Particle_swarm_optimization
		params.put("dpso.fp", new Double(1.0));  // value of the ö_p parameter in the formula for updating particle velocity see link above
		params.put("dpso.fg", new Double(2.0));  // value of the ö_g parameter in the formula for updating particle velocity see link above
/*
		// params.put("dpso.neighborhooddistance", new Integer(10));  
    // all particles in each island (i.e. sub-population) form a ring. 
		// The value 10 indicates that when updating the position of a 
		// particle p, the algorithm will compute the position of the best
		// particle found among the 20 “nearest neighbors” of p: the 10 
		// particles to the left of p, and the 10 particles to the right of p,
		// and use this “neighborhood” best position as the guiding vector g
		// in the formula in the link above.
*/
		RandomChromosomeMakerIntf rpm = new DblArray1CMaker();  // this class generates new random particles
		params.put("dpso.randomparticlemaker", rpm);
		DblArray1RandomVelocityMaker rvm = new DblArray1RandomVelocityMaker();  // generates random velocity vectors
		params.put("dpso.randomvelocitymaker", rvm);
		DblArray1StdVelocityMaker nvm = new DblArray1StdVelocityMaker();  // creates new velocity vectors using the update
		// velocity formula in the link above
		params.put("dpso.vmover", nvm);
		DblArray1ChromosomeVelocityAdder nva = new DblArray1ChromosomeVelocityAdder();  // computes new particle position using 
		// update formula in link above
		params.put("dpso.c2vadder", nva);
		RandomChromosomeSelector rcs = new RandomChromosomeSelector();  // forces the topology to a random one
		params.put("dpso.topologyselector", rcs);
		
		// let’s set the random number generator
		utils.RndUtil.addExtraInstances(1);
		utils.RndUtil.getInstance().setSeed(7);

		// finally, run DPSO
		popt4jlib.PS.DPSO optimizer = new popt4jlib.PS.DPSO(params);
		try {
			utils.PairObjDouble result = optimizer.minimize(my_fun);
			// get the best objective value found
			double best_value = result.getDouble();
			// get the minimizer vector
			double[] best_arg = (double[]) result.getArg();
			// done…
			System.err.println("best value found is=" + best_value);
			System.err.print("x=[ ");
			for (int i = 0; i < best_arg.length; i++) {
				System.err.print(best_arg[i] + " ");
			}
			System.err.println("]");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
