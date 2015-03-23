/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import utils.DataFileAccessClt;

/**
 * class implements a test of remote file read-access functionality implemented
 * in the <CODE>utils.DataFileAccess[Srv,Clt]</CODE> classes. The test assumes
 * that a <CODE>utils.DataFileAccessSrv</CODE> server is running on the default
 * port (7899). 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DataFileAccessSrvCacheStatsTest {
	
	/**
	 * invoke as:
	 * java -cp <classpath> tests.DataFileAccessSrvCacheStatsTest <filename> [hostname(localhost)]
	 * @param args 
	 */
	public static void main(String[] args) {
		final int port = 7899;
		String filename = args[0];
		String host = "localhost";
		try {
			long start = System.currentTimeMillis();
			DataFileAccessClt clt = new DataFileAccessClt(host,port);
			String data = clt.getServerCacheStatsForFile(filename);
			long dur = System.currentTimeMillis()-start;
			System.out.println("current srv cache for file "+filename+": "+data);
			System.out.println("replied in "+dur+" msecs");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}
