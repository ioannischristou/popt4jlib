/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

/**
 *
 * @author itc
 */
public class CacheDataMemberTest {
	public static void main(String[] args) {
		int num = 1000000;
		int dim = 10;
		if (args.length>0) num = Integer.parseInt(args[0]);
		if (args.length>1) dim = Integer.parseInt(args[1]);
		
		double[] arr = null;
		double[][] tot_arr = new double[num][];
		// first approach: "pools-are-bad"
		long start = System.currentTimeMillis();
		for (int i=0; i<num; i++) {
			arr = new double[dim];
			for (int j=1; j<dim; j++) {
				arr[j] += arr[j-1]+1;
			}
			tot_arr[i] = arr;
		}
		long dur1 = System.currentTimeMillis()-start;
		// second approach: "pools matter"
		start = System.currentTimeMillis();
		arr = new double[dim];
		for (int i=0; i<num; i++) {
			// arr = new double[dim];
			for (int j=1; j<dim; j++) {
				arr[j] += arr[j-1]+1;
			}
			tot_arr[i] = arr;			
		}
		long dur2 = System.currentTimeMillis()-start;
		System.out.println("pools-are-bad.time="+dur1+" msecs. pools-matter.time="+dur2+" msecs.");
	}
}
