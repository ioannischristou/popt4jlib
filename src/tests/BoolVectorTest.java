/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import popt4jlib.BoolVector;

/**
 * Tester class for BoolVector objects, and in particular, the parallel ops in
 * the class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BoolVectorTest {
	/**
	 * invoke as 
	 * <CODE>java -cp &lt;classpath&gt; tests.BoolVectorTest &lt;bitset_size&gt; &lt;num_tries&gt;</CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		int bitset_size = Integer.parseInt(args[0]);
		int num_tries = Integer.parseInt(args[1]);
		BoolVector one = new BoolVector(bitset_size);
		BoolVector two = new BoolVector(bitset_size);
		BoolVector three = new BoolVector(bitset_size);
		utils.RndUtil rndutil = utils.RndUtil.getInstance();
		rndutil.setSeed(7);
		java.util.Random r = rndutil.getRandom();
		for (int i=0; i<bitset_size; i++) {
			one.set(i, r.nextBoolean()); 
			two.set(i, r.nextBoolean());
			three.set(i, r.nextBoolean()); 
		}
		long dur=0L;
		for (int i=0; i<num_tries; i++) {
			/*
			one.clear(); two.clear();
			for (int j=0; j<bitset_size; j++) {
				one.set(j, j%10==0);
				two.set(j, j%100==0);
			}
			*/
			long start = System.currentTimeMillis();
			one.and(two);
			two.and(three);
			three.and(one);
			dur += (System.currentTimeMillis()-start);
		}
		System.out.println("total duration for and()="+dur);
		dur=0L;
		for (int i=0; i<num_tries; i++) {
			/*
			one.clear(); two.clear();
			for (int j=0; j<bitset_size; j++) {
				one.set(j, j%10==0);
				two.set(j, j%100==0);
			}
			*/
			long start = System.currentTimeMillis();
			one.andParallel2(two);
			two.andParallel2(three);
			three.andParallel2(one);
			dur += (System.currentTimeMillis()-start);
		}
		System.out.println("total duration for andParallel2()="+dur);
	}
}
