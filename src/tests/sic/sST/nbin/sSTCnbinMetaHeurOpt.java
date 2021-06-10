package tests.sic.sST.nbin;

import parallel.distributed.*;
import popt4jlib.DblArray1Vector;
import popt4jlib.GA.*;
import popt4jlib.SA.*;
import popt4jlib.PS.*;
import popt4jlib.DE.*;
import utils.Messenger;
import utils.PairObjDouble;
import java.util.HashMap;


/**
 * class uses any of the following supported meta-heuristics for (s,S,T) policy
 * optimization under compound Poisson demands: GA, SA, PS, DE.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2021</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class sSTCnbinMetaHeurOpt {
	
	/**
	 * meta-heuristic optimizer for (s,S,T) policy optimization of a single-
	 * echelon system facing compound Poisson demands modeled via the Negative
	 * Binomial distribution. Invoke as:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.sic.sST.nbin.sSTCnbinMetaHeurOpt 
	 * &lt;GA|SA|PS|DE&gt;
	 * &lt;Kr&gt; &lt;Ko&gt; &lt;L&gt; &lt;&lambda;&gt; &lt;p<sub>l</sub>&gt;
	 * &lt;h&gt; &lt;p&gt; [dbglvl(0)]
	 * </CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		final String meta = args[0];
		final double Kr = Double.parseDouble(args[1]);
		final double Ko = Double.parseDouble(args[2]);
		final double L = Double.parseDouble(args[3]);
		final double lambda = Double.parseDouble(args[4]);
		final double pl = Double.parseDouble(args[5]);
		final double h = Double.parseDouble(args[6]);
		final double p = Double.parseDouble(args[7]);
		int dbglvl = 0;
		if (args.length>8) dbglvl = Integer.parseInt(args[8]);
		final Messenger mger = Messenger.getInstance();
		mger.setDebugLevel(dbglvl);
		
		ssPlusDTCnbinBoxed f = new ssPlusDTCnbinBoxed(Kr, Ko, L, lambda, pl, h, p);
		
		HashMap params = new HashMap();
		
		if ("GA".equals(meta)) {  // prepare run for DGA
			params.put("dga.pdbtexecinitedwrkcmd", new PDBTExecNoOpCmd());
			params.put("dga.pdbtexecfinishedwrkcmd", 
				         new PDBTExecReportMaxSum12TermsCmd());
			params.put("dga.pdbthost", "localhost");
			params.put("dga.pdbtport", new Integer(7891));
			params.put("dga.chromosomelength", new Integer(3));  // [s,S,T]
			params.put("dga.minallelevalue", new Double(-150.0));
			params.put("dga.maxallelevalue", new Double(150.0));
			params.put("dga.minallelevalue1", new Double(1.0));
			params.put("dga.maxallelevalue1", new Double(50.0));
			params.put("dga.minallelevalue2", new Double(0.01));  // Tmin
			params.put("dga.maxallelevalue2", new Double(20.0));  // Tmax
			params.put("dga.randomchromosomemaker", 
				         new popt4jlib.GA.DblArray1CMaker());
			params.put("dga.xoverop", new DblArray1PtXOverOp());
			params.put("dga.mutationop", new DblVarArray1AlleleMutationOp());
			params.put("dga.function", f);
			params.put("dga.numgens", new Integer(10));
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
			params.put("dsa.minallelevalue1", new Double(1.0));   // Dmin
			params.put("dsa.maxallelevalue1", new Double(50.0));  // Dmax
			params.put("dsa.minallelevalue2", new Double(0.01));  // Tmin
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
				System.out.println("DSA soln: s="+Math.round(x[0])+
					                 " S="+(Math.round(x[0])+Math.round(x[1]))+
					                 " T="+x[2]);
				System.out.println("DSA best cost found="+result.getDouble()+
					                 " in "+dur+" msecs");
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}			
		}
		else if ("PS".equals(meta)) {  // prepare run for DGA
			params.put("dpso.pdbtexecinitedwrkcmd", new PDBTExecNoOpCmd());
			params.put("dpso.pdbthost", "localhost");
			params.put("dpso.pdbtport", new Integer(7891));
			params.put("dpso.chromosomelength", new Integer(3));  // [s,S,T]
			params.put("dpso.minallelevalue", new Double(-150.0));
			params.put("dpso.maxallelevalue", new Double(150.0));
			params.put("dpso.minallelevalue1", new Double(1.0));
			params.put("dpso.maxallelevalue1", new Double(50.0));
			params.put("dpso.minallelevalue2", new Double(0.01));  // Tmin
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
				System.out.println("DPSO soln: s="+Math.round(x[0])+
					                 " S="+(Math.round(x[0])+Math.round(x[1]))+
					                 " T="+x[2]);
				System.out.println("DPSO best cost found="+result.getDouble()+
					                 " in "+dur+" msecs");
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		else if ("DE".equals(meta)) {  // prepare run for DGA
			params.put("dde.numdimensions", new Integer(3));  // [s,S,T]
			params.put("dde.numtries", new Integer(20));
			params.put("dde.numthreads", new Integer(24));
			params.put("dde.minargval", new Double(-150.0));
			params.put("dde.maxargval", new Double(150.0));
			params.put("dde.minargval1", new Double(1.0));
			params.put("dde.maxargval1", new Double(50.0));
			params.put("dde.minargval2", new Double(0.01));  // Tmin
			params.put("dde.maxargval2", new Double(20.0));  // Tmax
			try {
				DDE dde = new DDE(params);
				long start = System.currentTimeMillis();
				PairObjDouble result = dde.minimize(f);
				long dur = System.currentTimeMillis()-start;
				double[] x = ((DblArray1Vector) result.getArg()).getDblArray1();
				System.out.println("DDE soln: s="+Math.round(x[0])+
					                 " S="+(Math.round(x[0])+Math.round(x[1]))+
					                 " T="+x[2]);
				System.out.println("DDE best cost found="+result.getDouble()+
					                 " in "+dur+" msecs");
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}
