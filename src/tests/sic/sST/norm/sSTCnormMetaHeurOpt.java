package tests.sic.sST.norm;

import parallel.distributed.*;
import popt4jlib.DblArray1Vector;
import popt4jlib.GA.*;
import popt4jlib.SA.*;
import popt4jlib.PS.*;
import popt4jlib.DE.*;
import popt4jlib.TS.*;
import utils.Messenger;
import utils.PairObjDouble;
import java.util.HashMap;


/**
 * class uses any of the following supported meta-heuristics for (s,S,T) policy
 * optimization under normal demands: GA, SA, PS, DE, TS.
 * <p>Notes:
 * <ul>
 * <li>2023-09-22: added the Tabu Search method in the list of meta-heuristics
 * to try.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2023</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class sSTCnormMetaHeurOpt {
	
	/**
	 * meta-heuristic optimizer for (s,S,T) policy optimization of a single-
	 * echelon system facing normal demands. Invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.sST.norm.sSTCnormMetaHeurOpt 
	 * &lt;GA|SA|PS|DE|TS&gt;
	 * &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&mu;&gt; &lt;&sigma;&gt;
	 * &lt;h&gt; &lt;p&gt; [p2(0)] [dbglvl(0)]
	 * </CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		final String meta = args[0];
		final double Kr = Double.parseDouble(args[1]);
		final double Ko = Double.parseDouble(args[2]);
		final double L = Double.parseDouble(args[3]);
		final double mi = Double.parseDouble(args[4]);
		final double sigma = Double.parseDouble(args[5]);
		final double Tmin = Math.pow(3.5*sigma/mi, 2.0);
		final double h = Double.parseDouble(args[6]);
		final double p = Double.parseDouble(args[7]);
		double p2 = 0.0;
		if (args.length>8) p2 = Double.parseDouble(args[8]);
		int dbglvl = 0;
		if (args.length>9) dbglvl = Integer.parseInt(args[9]);
		final Messenger mger = Messenger.getInstance();
		mger.setDebugLevel(dbglvl);
		
		ssPlusDTCnormBoxed f = 
			new ssPlusDTCnormBoxed(Kr, Ko, L, mi, sigma, h, p, p2);
		
		HashMap params = new HashMap();
		
		if ("GA".equals(meta)) {  // prepare run for DGA
			params.put("dga.pdbtexecinitedwrkcmd", new PDBTExecNoOpCmd());
			params.put("dga.pdbthost", "localhost");
			params.put("dga.pdbtport", new Integer(7891));
			params.put("dga.chromosomelength", new Integer(3));  // [s,S,T]
			params.put("dga.minallelevalue", new Double(-150.0));
			params.put("dga.maxallelevalue", new Double(150.0));
			params.put("dga.minallelevalue1", new Double(1.e-4));
			params.put("dga.maxallelevalue1", new Double(50.0));
			params.put("dga.minallelevalue2", new Double(Tmin));  // Tmin
			params.put("dga.maxallelevalue2", new Double(20.0));  // Tmax
			params.put("dga.randomchromosomemaker", 
				         new popt4jlib.GA.DblArray1CMaker());
			params.put("dga.xoverop", new DblArray1PtXOverOp());
			params.put("dga.mutationop", new DblVarArray1AlleleMutationOp());
			params.put("dga.function", f);
			params.put("dga.numgens", new Integer(20));
			params.put("dga.poplimit", new Integer(30));
			try {
				DGA dga = new DGA(params);
				long start = System.currentTimeMillis();
				PairObjDouble result = dga.minimize(f);
				long dur = System.currentTimeMillis()-start;
				double[] x = (double[]) result.getArg();
				System.out.println("DGA soln: s="+Math.round(x[0])+
					                 " S="+(Math.round(x[0])+Math.round(x[1]))+
					                 " T="+x[2]);
				System.out.println("DGA best cost found="+result.getDouble()+
					                 " in "+dur+" msecs");
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		else if ("SA".equals(meta)) {  // prepare run for DSA
			params.put("dsa.chromosomelength", new Integer(3));  // [s,S,T]
			params.put("dsa.minallelevalue", new Double(-150.0));
			params.put("dsa.maxallelevalue", new Double(150.0));
			params.put("dsa.minallelevalue1", new Double(1.e-4));   // Dmin
			params.put("dsa.maxallelevalue1", new Double(50.0));  // Dmax
			params.put("dsa.minallelevalue2", new Double(Tmin));  // Tmin
			params.put("dsa.maxallelevalue2", new Double(20.0));  // Tmax
			params.put("dsa.T0", new Double(100.0));
			//params.put("dsa.schedule", new ExpDecSchedule());
			params.put("dsa.randomchromosomemaker", 
				         new popt4jlib.SA.DblArray1CMaker());
			params.put("dsa.movedelta", new Double(2.0));
			params.put("dsa.movemaker", new popt4jlib.SA.DblArray1MoveMaker());
			params.put("dsa.numthreads", new Integer(24));
			params.put("dsa.numtriesperiter", new Integer(3));
			params.put("dsa.numouteriters", new Integer(20));
			params.put("dsa.function", f);
			try {
				DSA dsa = new DSA(params);
				long start = System.currentTimeMillis();
				PairObjDouble result = dsa.minimize(f);
				long dur = System.currentTimeMillis()-start;
				double[] x = (double[]) result.getArg();
				System.out.println("DSA soln: s="+x[0]+
					                 " S="+(x[0]+x[1])+
					                 " T="+x[2]);
				System.out.println("DSA best cost found="+result.getDouble()+
					                 " in "+dur+" msecs");
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}			
		}
		else if ("PS".equals(meta)) {  // prepare run for DPSO
			params.put("dpso.pdbtexecinitedwrkcmd", new PDBTExecNoOpCmd());
			params.put("dpso.pdbthost", "localhost");
			params.put("dpso.pdbtport", new Integer(7891));
			params.put("dpso.chromosomelength", new Integer(3));  // [s,S,T]
			params.put("dpso.minallelevalue", new Double(-150.0));
			params.put("dpso.maxallelevalue", new Double(150.0));
			params.put("dpso.minallelevalue1", new Double(1.e-4));
			params.put("dpso.maxallelevalue1", new Double(50.0));
			params.put("dpso.minallelevalue2", new Double(Tmin));  // Tmin
			params.put("dpso.maxallelevalue2", new Double(20.0));  // Tmax
			params.put("dpso.randomparticlemaker", 
				         new popt4jlib.PS.DblArray1CMaker());
			params.put("dpso.randomvelocitymaker",new DblArray1RandomVelocityMaker());
			params.put("dpso.vmover", new DblArray1StdVelocityMaker());
			params.put("dpso.c2vadder", new DblArray1ChromosomeVelocityAdder());
			params.put("dpso.function", f);
			params.put("dpso.numgens", new Integer(20));
			try {
				DPSO dpso = new DPSO(params);
				long start = System.currentTimeMillis();
				PairObjDouble result = dpso.minimize(f);
				long dur = System.currentTimeMillis()-start;
				double[] x = (double[]) result.getArg();
				System.out.println("DPSO soln: s="+x[0]+
					                 " S="+(x[0]+x[1])+
					                 " T="+x[2]);
				System.out.println("DPSO best cost found="+result.getDouble()+
					                 " in "+dur+" msecs");
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		else if ("DE".equals(meta)) {  // prepare run for DDE
			params.put("dde.numdimensions", new Integer(3));  // [s,S,T]
			params.put("dde.numtries", new Integer(20));
			params.put("dde.numthreads", new Integer(24));
			params.put("dde.minargval", new Double(-150.0));
			params.put("dde.maxargval", new Double(150.0));
			params.put("dde.minargval1", new Double(1.e-4));
			params.put("dde.maxargval1", new Double(50.0));
			params.put("dde.minargval2", new Double(Tmin));  // Tmin
			params.put("dde.maxargval2", new Double(20.0));  // Tmax
			try {
				DDE dde = new DDE(params);
				long start = System.currentTimeMillis();
				PairObjDouble result = dde.minimize(f);
				long dur = System.currentTimeMillis()-start;
				double[] x = ((DblArray1Vector) result.getArg()).getDblArray1();
				System.out.println("DDE soln: s="+x[0]+
					                 " S="+(x[0]+x[1])+
					                 " T="+x[2]);
				System.out.println("DDE best cost found="+result.getDouble()+
					                 " in "+dur+" msecs");
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		else if ("TS".equals(meta)) {  // prepare run for DTS
			params.put("dts.chromosomelength", new Integer(3));  // [s,S,T]
			params.put("dts.minallelevalue", new Double(-10.0));
			// above smin used to be -150
			params.put("dts.maxallelevalue", new Double(30.0));
			// above smax used to be 150
			params.put("dts.minallelevalue1", new Double(1.e-4));   // Dmin
			params.put("dts.maxallelevalue1", new Double(20.0));  // Dmax
			// above Dmax used to be 50.0
			params.put("dts.minallelevalue2", new Double(Tmin));  // Tmin
			params.put("dts.maxallelevalue2", new Double(3.0));  // Tmax
			// above Tmax used to be 20.0
			params.put("dts.randomchromosomemaker", 
				         new popt4jlib.TS.DblArray1CMaker());
			params.put("dts.movedelta", new Double(0.7));  // used to be 2.0
			params.put("dts.movemaker", new popt4jlib.TS.DblArray1MoveMaker());
			params.put("dts.numthreads", new Integer(8));  // used to be 24
			params.put("dts.nhoodmindist", new Double(1.e-2));
			params.put("dts.nhooddistcalc", new popt4jlib.DblArray1DistCalc());
			params.put("dts.numiters", new Integer(20));
			params.put("dts.nhoodsize", new Integer(10));
			params.put("dts.tabulistmaxsize", new Integer(10)); 
			params.put("dts.function", f);
			try {
				DTS dts = new DTS(params);
				long start = System.currentTimeMillis();
				PairObjDouble result = dts.minimize(f);
				long dur = System.currentTimeMillis()-start;
				double[] x = (double[]) result.getArg();
				System.out.println("DTS soln: s="+x[0]+
					                 " S="+(x[0]+x[1])+
					                 " T="+x[2]);
				System.out.println("DTS best cost found="+result.getDouble()+
					                 " in "+dur+" msecs");
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}			
		}
		
	}
}
