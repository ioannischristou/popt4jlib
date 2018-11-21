package utils;

import java.util.*;
import java.io.*;

/**
 * The class RareClusterEventsTestMaker produces synthetic test-sets for 
 * Quantitative Association Rule Mining algorithms such as QARMA.
 * <p>Title: popt4jlib</p>
 * <p>Description: parallel optimization library for Java</p>
 * <p>Copyright: Copyright (c) 2013-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RareClusterEventsTestMaker {
  private static Random _r = new Random();


  /**
   * The main method of the class. The input arguments are:
   * <ul>
	 * <li>args[0] -- int d, the dimensionality of the vector space
   * <li>args[1] -- string f, the filename containing the pdf data for each
   * cluster. This file has the following format:
   * line-i: num_points_to_make,mean_of_pdf_i1,sd_of_pdf_i1,
	 * mean_of_pdf_i2,sd_of_pdf_i2,...mean_of_pdf_id,sd_of_pdf_id
	 * [,miss_prob][,[mindim-]maxdim]
   * i=1...k where the last two values are optional and indicate the over-riding
	 * probability and min_dim_index(1) and max_dim_index with which values can go 
	 * missing in this cluster. The last two values are truly overriding 
	 * probabilities within the dimension indices specified by the last value.
	 * Outside this range, the default missing probabilities (specified in args[3]
	 * and args[4]) are used to compute missing values.
	 * Lines starting with # in this file are ignored.
   * <li>args[2] -- string of, the filename containing the result dataset in
	 * CSV format of the form v_1,v_2,...v_d
   * <li>args[3] -- double missingprob, optional, the probability with which a
	 * value may be missing. Default is zero (no missing values)
	 * <li>args[4] -- int maxmissingind optional, the highest dimension index for
	 * which the probability specified in args[3] applies. Default is -1 (no 
	 * missing values)
   *</ul>
   * The program produces one file containing the dataset created.
   * @param args String[]
   */
  public static void main(String[] args) throws Exception {
    final int dims = Integer.parseInt(args[0]);
    final String ifname = args[1];
    final String ofname = args[2];
		double miss_prob = 0.0;
    int max_miss_dim = -1;
		if (args.length>3) {
      try {
        miss_prob = Double.parseDouble(args[3]);
				if (args.length>4) {
					try {
						max_miss_dim = Integer.parseInt(args[4]);
					}
					catch (Exception e) {
						System.err.println("cannot parse 5-th param, expected int in [0,"+
							                 (dims-1)+"]");						
					}
				}
      }
      catch (Exception e) {
				System.err.println("cannot parse 4-th param, expected double in [0,1]");
      }
    }
		// read total num vectors to create
		int num_vecs = getTotalNumVectors(ifname);
    // read ifname
    BufferedReader br = new BufferedReader(new FileReader(ifname));
    String line=null;
    StringTokenizer st=null;
    //double tot_obj=0.0;
    int i=0;
		double[] dimsA = new double[dims];
		PrintWriter vpw = new PrintWriter(new FileWriter(ofname));
    if (br.ready()) {
      // Vector all_clusters = new Vector();
      while (true) {
        line = br.readLine();
        if (line == null || line.length() == 0) break;
				if (line.startsWith("#")) continue;  // comment line
		    // over-riding values
				double miss_prob_override = miss_prob;
				int min_miss_dim_ind_override = 0;
				int max_miss_dim_ind_override = max_miss_dim;
        i++;
        st = new StringTokenizer(line, ",");
        int card_i = Integer.parseInt(st.nextToken());
        // make the ith cluster of Documents
				//System.err.println("ClusterTestMaker: creating the "+i+"-th cluster");
        List cluster_i = new ArrayList();  // Vector<VectorIntf>
        for (int j=0; j<card_i; j++) {
          cluster_i.add(new popt4jlib.DblArray1Vector(dimsA));
        }
        // populate the cluster-i:
        int dim=0;
				while (st.hasMoreTokens()) {
          double m_dim=Double.parseDouble(st.nextToken());
					if (dim==dims) {
						miss_prob_override = m_dim;
						if (st.hasMoreTokens()) {
							String minmaxdimind = st.nextToken();
							try {
								max_miss_dim_ind_override = Integer.parseInt(minmaxdimind);
							}
							catch (NumberFormatException e) {  // it's in min-max format
								StringTokenizer st2 = new StringTokenizer(minmaxdimind,"-");
								min_miss_dim_ind_override = Integer.parseInt(st2.nextToken());
								max_miss_dim_ind_override = Integer.parseInt(st2.nextToken());
							}
						}
						break;
					}
          double sd_dim = Double.parseDouble(st.nextToken());
          // produce the value of the dim-th coordinate
          for (int j=0; j<card_i; j++) {
            double val = gaussian(m_dim, sd_dim);
            popt4jlib.VectorIntf doc_j = (popt4jlib.VectorIntf)cluster_i.get(j);
            doc_j.setCoord(dim, val);
          }
          dim++;
        }
				// 
				// write the cluster_i to file
				for (int j=0; j<card_i; j++) {
					popt4jlib.VectorIntf vj = (popt4jlib.VectorIntf) cluster_i.get(j);
					for (int k=0; k<dims; k++) {
						double rand_val = _r.nextDouble();
						boolean miss_override = rand_val < miss_prob_override;
						if (k>=min_miss_dim_ind_override && k<=max_miss_dim_ind_override && 
							  miss_override) {  // first cond is override 
							vpw.print("?");
							if (k<dims-1) vpw.print(",");
							continue;
						}
						else if ((k<min_miss_dim_ind_override || 
							        k>max_miss_dim_ind_override) &&
							   		  k<=max_miss_dim && rand_val<miss_prob) {
							// second condition is overall-applicable missing prob and index
							vpw.print("?");
							if (k<dims-1) vpw.print(",");
							continue;
						}
						if (k<dims-1) vpw.print(vj.getCoord(k)+",");
						else vpw.print(vj.getCoord(k));
					}
					vpw.println();
				}
      }  // next cluster i
			vpw.flush();
			vpw.close();
    }
  }


  private static double gaussian(double m, double sd) {
    return m+sd*_r.nextGaussian();
  }


	private static int getTotalNumVectors(String ifname) throws IOException {
		int res = 0;
		BufferedReader br = new BufferedReader(new FileReader(ifname));
		while (true) {
			String line = br.readLine();
			if (line==null) break;  // EOF
			if (line.startsWith("#")) continue;  // ignore comments
			StringTokenizer st = new StringTokenizer(line,",");
			int n = Integer.parseInt(st.nextToken());
			res += n;
		}
		br.close();
		return res;
	}
}
