/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import java.util.List;
import popt4jlib.VectorIntf;
import utils.DataFileAccessClt;

/**
 * class implements a test of remote file read-access functionality implemented
 * in the <CODE>utils.DataFileAccess[Srv,Clt]</CODE> classes. The test assumes
 * that a <CODE>utils.DataFileAccessSrv</CODE> server is running on the default
 * port (7899). The test should be run by running at least one (preferably two)
 * JVMs executing this class with appropriate arguments.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DataFileAccessTest {
	
	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; tests.DataFileAccessTest &lt;filename&gt; [hostname(localhost)] [startind(0)] [endind(0)]</CODE>
	 * @param args 
	 */
	public static void main(String[] args) {
		final int port = 7899;
		String filename = args[0];
		String host = "localhost";
		int startind = 0;
		int endind = 0;  // by default, just read the first vector in the file
		if (args.length>1) {
			host = args[1];
			if (args.length>2) {
				startind = Integer.parseInt(args[2]);
				if (args.length>3)
					endind = Integer.parseInt(args[3]);
			}
		}
		try {
			long start = System.currentTimeMillis();
			DataFileAccessClt clt = new DataFileAccessClt(host,port);
			List data = clt.readVectorsFromRemoteFile(filename, startind, endind);
			long dur = System.currentTimeMillis()-start;
			for (int i=0; i<data.size(); i++) {
				VectorIntf xi = (VectorIntf) data.get(i);
				System.out.println("xi="+xi.toString());
			}
			System.out.println("Got total of "+data.size()+" VectorIntf objects in "+dur+" msecs");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}
