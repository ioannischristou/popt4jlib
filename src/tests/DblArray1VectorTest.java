/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import popt4jlib.DblArray1SparseVector;
import popt4jlib.DblArray1Vector;
import popt4jlib.GradientDescent.VecUtil;
import popt4jlib.VectorIntf;
import utils.DataMgr;

/**
 * tests <CODE>popt4jlib.DblArray1[Sparse][XXX]Vector</CODE> functionalities.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1VectorTest {
	
	/**
	 * run class as follows:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.DblArray1VectorTest 
	 * &lt;vectors_file&gt;
	 * [num_clusters(1000)]
	 * </CODE>.
	 * @param args String[]
	 */
	public static void main(String[] args) {
		try {
			Vector vecs = DataMgr.readVectorsFromFile(args[0]);
			Vector sparse_vecs = DataMgr.readSparseVectorsFromFile(args[0]);
			System.err.println("Done loading vectors");
			// 1. first test: compare DblArray1Vector and DblArray1SparseVector diffs
			//    in Euclidean space
			for (int i=0; i<vecs.size(); i++) {
				if (i % 1000000 == 0) {
					System.err.println("first test: tested "+i+" vectors");
				}
				VectorIntf vi = (VectorIntf) vecs.get(i);
				VectorIntf vsi = (VectorIntf) sparse_vecs.get(i);
				double disi = VecUtil.getEuclideanDistance(vi, vsi);
				if (Double.compare(disi,1.e-18)>0) {
					System.err.println("vi="+vi);
					System.err.println("vsi="+vsi);
					System.err.println("Euclidean distance between the two="+disi);
					throw new Error("positive distance");
				}
			}
			System.err.println("first test ok");
			// 2. second test: add two vectors together, then divide by two and see 
			//    if there are any diffs
			for (int i=0; i<vecs.size(); i++) {
				if (i % 1000000 == 0) {
					System.err.println("second test: tested "+i+" vectors");
				}
				VectorIntf vi = (VectorIntf) vecs.get(i);
				double vi_norm2 = VecUtil.norm2(vi);
				VectorIntf vsi = (VectorIntf) sparse_vecs.get(i);
				VectorIntf vsi_copy = vsi.newInstance();
				vsi_copy.addMul(1.0, vi);
				double vsi2_norm2 = VecUtil.norm2(vsi_copy);
				if (Math.abs(vsi2_norm2-2.0*vi_norm2)>1.e-12) {
					System.err.println("vi="+vi);
					System.err.println("vsi="+vsi);
					System.err.println("vsi2="+vsi_copy);
					System.err.println("vi_norm2="+vi_norm2+" vsi2_norm2="+vsi2_norm2);
					throw new Error("positive difference in norms");
				}
				vsi_copy.div(2.0);
				double disti = VecUtil.getEuclideanDistance(vi, vsi_copy);
				if (Double.compare(disti, 1.e-18)>0) {
					System.err.println("vi="+vi);
					System.err.println("vsi_copy="+vsi_copy);
					System.err.println("Euclidean distance between the two="+disti);
					throw new Error("positive distance");					
				}
			}
			System.err.println("second test ok");
			// 3. third test: compute centers for 1 times
			int n = args.length>1 ? Integer.parseInt(args[1]) : 1000;
			Random r = utils.RndUtil.getInstance().getRandom();
			for (int i=0; i<1; i++) {
				System.err.println("creating random partition among "+n+" clusters");
				int[] cinds = new int[vecs.size()];
				for (int j=0; j<cinds.length; j++) {
					cinds[j] = r.nextInt(n);
				}
				List vecs_centers = getCenters(vecs, cinds, n);
				List sparse_vecs_centers = getCenters(sparse_vecs, cinds, n);
				for (int j=0; j<vecs_centers.size(); j++) {
					VectorIntf vi = (VectorIntf) vecs_centers.get(i);
					VectorIntf svi = (VectorIntf) sparse_vecs_centers.get(i);
					if (vi!=null) {
						if (svi==null) {
							System.err.println("vi="+vi);
							System.err.println("svi=null");
							throw new Error("vi!=svi");
						}
						double dist = VecUtil.getEuclideanDistance(vi, svi);
						if (Math.abs(dist)>1.e-12) {
							System.err.println("vi="+vi);
							System.err.println("svi="+svi);
							System.err.println("||vi-svi||="+dist);
							throw new Error("try-i="+i+": different centers computed");
						}
					}
				}
				// get centers the explicit way
				VectorIntf[] s_v_centers2 = new VectorIntf[n];
				for (int j=0; j<n; j++) {
					s_v_centers2[j]=(((VectorIntf)sparse_vecs.get(0)).newCopyMultBy(0));
				}
				int cards[] = new int[n];
				for (int j=0; j<sparse_vecs.size(); j++) {
					VectorIntf svj = (VectorIntf) sparse_vecs.get(j);
					s_v_centers2[cinds[j]].addMul(1.0, svj);
					cards[cinds[j]]++;
				}
				for (int j=0; j<n; j++) {
					s_v_centers2[j].div((double) cards[j]);
					double dst = VecUtil.getEuclideanDistance(s_v_centers2[j], (VectorIntf)vecs_centers.get(j));
					if (Double.compare(Math.abs(dst), 1.e-15)>0) {
						System.err.println("s_v_centers2["+j+"]="+s_v_centers2[j]);
						System.err.println("vecs_centers.get(j)="+vecs_centers.get(j));
						System.err.println("dst="+dst);
						throw new Error("centers don't match within 1.e-15");
					}
				}
				// also, compute MSSC 
				double dense_mssc = 0.0;
				for (int j=0; j<vecs.size(); j++) {
					VectorIntf dj = (VectorIntf) vecs.get(j);
					double dist_j=Double.MAX_VALUE;
					int best_m = -1;
					for (int m=0; m<n; m++) {
						VectorIntf cm = (VectorIntf) vecs_centers.get(m);
						double dmj = VecUtil.getEuclideanDistance(cm, dj);
						dmj *= dmj;  // square it
						if (dmj<dist_j) {
							dist_j = dmj;
							best_m = m;
						}
					}
					dense_mssc += dist_j;
				}
				double sparse_mssc = 0.0;
				for (int j=0; j<sparse_vecs.size(); j++) {
					VectorIntf dj = (VectorIntf) sparse_vecs.get(j);
					double dist_j=Double.MAX_VALUE;
					int best_m = -1;
					for (int m=0; m<n; m++) {
						VectorIntf cm = (VectorIntf) s_v_centers2[m];
						double dmj = VecUtil.getEuclideanDistance(cm, dj);
						dmj *= dmj;  // square it
						if (dmj<dist_j) {
							dist_j = dmj;
							best_m = m;
						}
					}
					sparse_mssc += dist_j;
				}
				if (Double.compare(Math.abs(dense_mssc-sparse_mssc),1.e-15)>0) {
					System.err.println("sparse_mssc="+sparse_mssc);
					System.err.println("dense_mssc="+dense_mssc);
					System.exit(-1);
				}
			}
			System.err.println("third test ok");
			// 4. fourth test: set all values to zero, then to one
			final int dims = ((VectorIntf)vecs.get(0)).getNumCoords();
			DblArray1Vector zero = new DblArray1Vector(dims);
			VectorIntf one = new DblArray1SparseVector(dims);
			for (int i=0; i<one.getNumCoords(); i++) one.setCoord(i, 1.0);
			double sqrtn = VecUtil.norm2(one);
			if (Math.abs(sqrtn-Math.sqrt(dims))>1.e-12) {
				System.err.println("one="+one);
				System.err.println("||one||="+sqrtn+" != sqrt("+dims+")="+
					                 Math.sqrt(dims));
				throw new Error("sqrts unequal");
			}
			for (int i=0; i<one.getNumCoords(); i++) one.setCoord(i, 0.0);
			if (!one.isAtOrigin()) {
				System.err.println("after setting all components to zero, one="+one);
				throw new Error("error setting sparse vector to zero");
			}
			System.err.println("fourth test ok");
			// 5. final test: randomly set the values of random components in two 
			//    vectors (one dense, one sparse) and see if there are any diffs.
			VectorIntf dense0 = (VectorIntf) vecs.get(0);
			VectorIntf sparse0 = (VectorIntf) sparse_vecs.get(0);
			System.err.println("dense0="+dense0);
			System.err.println("sparse0="+sparse0);
			double diff0 = VecUtil.getEuclideanDistance(dense0, sparse0);
			System.err.println("dense-sparse-0-dist="+diff0);
			for (int i=0; i<10000; i++) {
				int dim = r.nextInt(dims);
				double v = r.nextDouble();
				dense0.setCoord(dim, v);
				sparse0.setCoord(dim, v);
				diff0 = VecUtil.getEuclideanDistance(dense0, sparse0);
				if (Double.compare(Math.abs(diff0), 1.e-18)>0) {
					System.err.println("dense="+dense0);
					System.err.println("sparse0="+sparse0);
					throw new Error("vectors don't match");
				}
			}
			System.err.println("fifth test ok");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
  /**
   * compute the cluster centers of this clustering described in the args
   * the clusterindices[] values range from [0...num_clusters-1]. Method is 
	 * essentially a copy of the 
	 * <CODE>popt4jlib.MSSC.GMeansMTClusterer.getCenters()</CODE> method.
   * @param docs List // List&lt;VectorIntf&gt;
   * @param clusterindices int[]
   * @param k int
   * @return List  // ArrayList&lt;VectorIntf&gt;
   */
  private static List getCenters(List docs, int[] clusterindices, int k) {
    final int docs_size = docs.size();
    List centers = new ArrayList();  // Vector<VectorIntf>
    for (int i=0; i<k; i++)
      centers.add(((VectorIntf) docs.get(0)).newCopyMultBy(0));
    int[] cards = new int[k];
    for (int i=0; i<k; i++) cards[i]=0;

    for (int i=0; i<docs_size; i++) {
      int ci = clusterindices[i];
      VectorIntf centeri = (VectorIntf) centers.get(ci);
      VectorIntf di = (VectorIntf) docs.get(i);
      try {
        centeri.addMul(1.0, di);
      }
      catch (parallel.ParallelException e) {  // can never get here
        e.printStackTrace();
      }
      cards[ci]++;
    }
    // divide by cards
    for (int i=0; i<k; i++) {
      VectorIntf centeri = (VectorIntf) centers.get(i);
      try {
        centeri.div((double) cards[i]);
      }
      catch (parallel.ParallelException e) {  // can never get here
        e.printStackTrace();
      }
    }
    return centers;
  }

}
