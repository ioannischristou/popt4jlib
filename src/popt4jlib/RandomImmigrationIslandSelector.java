/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib;

import utils.RndUtil;
import java.util.HashMap;

/**
 * selects at random an island to send immigrants.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RandomImmigrationIslandSelector implements ImmigrationIslandOpIntf {
	
	/**
	 * 
	 * @param myid int
	 * @param gen int unused
	 * @param islandPop int[]
	 * @param params HashMap must contain an entry &lt;"thread.id", int id&gt;
	 * @return int a random number in [0,islandPop.length-1] different from myid
	 */
	public int getImmigrationIsland(int myid, int gen, int[] islandPop, HashMap params) {
		final int id = ((Integer) params.get("thread.id")).intValue();
		final int islsz = islandPop.length;
		final RndUtil rndgen = RndUtil.getInstance(id);
		int j = rndgen.getRandom().nextInt(islsz);
		if (j==myid) {
			if (myid<islsz/2) ++j;
			else if (myid>0) --j;  // if myid==0 && myid>=islsz/2 ==> islsz <= 1 thus keep j=0
		}
		return j;
	}
	
}
