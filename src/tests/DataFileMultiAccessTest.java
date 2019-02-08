/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import java.util.Random;
import java.util.List;
import utils.DataFileAccessClt;

/**
 * class implements a test of remote file read-access functionality implemented
 * in the <CODE>utils.DataFileAccess[Srv,Clt]</CODE> classes. The test assumes
 * that a <CODE>utils.DataFileAccessSrv</CODE> server is running on the default
 * port (7899). The test should be run by running at least one (preferably two)
 * JVMs executing this class with appropriate arguments.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2019</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DataFileMultiAccessTest {
	
	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; tests.DataFileMultiAccessTest &lt;filename&gt; [hostname(localhost)] [startind(0)] [endind(0)] [numiters]</CODE>
	 * @param args 
	 */
	public static void main(String[] args) {
		final int port = 7899;
		String filename = args[0];
		String host = "localhost";
		int startind = 0;
		int endind = 0;  // by default, just read the first vector in the file
		int numiters = Integer.MAX_VALUE;
		if (args.length>1) {
			host = args[1];
			if (args.length>2) {
				startind = Integer.parseInt(args[2]);
				if (args.length>3) {
					endind = Integer.parseInt(args[3]);
					if (args.length>4) {
						numiters = Integer.parseInt(args[4]);
					}
				}
			}
		}
		try {
			Random r = utils.RndUtil.getInstance().getRandom();
			DataFileAccessClt clt = new DataFileAccessClt(host,port);
			for (int i=0; i<numiters; i++) {
				Thread.sleep(100);  // sleep 0.1 seconds
				long start = System.currentTimeMillis();
				int st = startind + r.nextInt(endind+1-startind);
				int en = st + r.nextInt(endind+1-st);
				List data = clt.readVectorsFromRemoteFile(filename, st, en);
				long dur = System.currentTimeMillis()-start;
				System.out.println("Got total of "+data.size()+" VectorIntf objects (["+st+","+en+"]) in "+dur+" msecs");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
}
