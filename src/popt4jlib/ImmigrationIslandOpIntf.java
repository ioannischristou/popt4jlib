/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib;

import java.util.HashMap;


/**
 * interface that may be used in island models of population-based optimization
 * meta-heuristics, allowing for the customization of the island migration 
 * topologies.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface ImmigrationIslandOpIntf {
	
	/**
	 * get the id of the island to which the island with id=myid should send 
	 * immigrants.
	 * @param myid int
	 * @param gen int
	 * @param islandsPop int[]
	 * @param params HashMap
	 * @return int should be a number in [0, islandsPop.length-1] or -1 to 
	 * indicate no migration from island with id=myid.
	 */
	public int getImmigrationIsland(int myid, int gen, int[] islandsPop, HashMap params);
}
