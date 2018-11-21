package tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import popt4jlib.DblArray1SparseVector;
import popt4jlib.DblArray1Vector;
import popt4jlib.GradientDescent.VecUtil;
import popt4jlib.VectorIntf;

/**
 * tests <CODE>popt4jlib.DblArray1SparseVector</CODE> and in particular, the
 * <CODE>popt4jlib.GradientDescent.VecUtil.getEuclideanDistance(x,y)</CODE> 
 * methods.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1SparseVectorTest {
	
	/**
	 * run class as follows:
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.DblArray1VectorTest 
	 * [num_tests(100)] [num_dims(100)] [num_non_zeros(10)]
	 * </CODE>.
	 * @param args String[]
	 */
	public static void main(String[] args) {
		try {
			int num = 100;
			if (args.length>0) num = Integer.parseInt(args[0]);
			
			int dim = 100;
			if (args.length>1) dim = Integer.parseInt(args[1]);
			
			int nonzeros = 10;
			if (args.length>2) nonzeros = Integer.parseInt(args[2]);
			
			Random r = utils.RndUtil.getInstance().getRandom();
			long sparse_nanos = 0;
			long dense_nanos = 0;
			// create a pair of sparse vectors, compute their euclidean distance.
			// do the same for these vectors, but for their dense equivalent.
			for (int i=0; i<num; i++) {
				DblArray1SparseVector x = new DblArray1SparseVector(dim);
				DblArray1SparseVector y = new DblArray1SparseVector(dim);
				for (int j=0; j<nonzeros; j++) {
					int xpos = r.nextInt(dim);
					x.setCoord(xpos, r.nextDouble());
					int ypos = r.nextInt(dim);
					y.setCoord(ypos, r.nextDouble());					
				}
				long start = System.nanoTime();
				double dxy = VecUtil.getEuclideanDistance(x, y);
				long dur = System.nanoTime()-start;
				sparse_nanos += dur;
				DblArray1Vector xv = new DblArray1Vector(x.getDblArray1());
				DblArray1Vector yv = new DblArray1Vector(y.getDblArray1());
				start = System.nanoTime();
				double dxvyv = VecUtil.getEuclideanDistance(xv, yv);
				dur = System.nanoTime()-start;
				dense_nanos += dur;
				if (Double.compare(dxy, dxvyv)!=0) {
					System.err.println("x="+x);
					System.err.println("y="+y);
					System.err.println("xv="+xv);
					System.err.println("yv="+yv);
					System.err.println("dxy="+dxy+" but dxvyv="+dxvyv);
					throw new Error("euclidean distance computation not a match...");
				}
			}
			System.err.println("test ok. sparse_nanos="+sparse_nanos+
				                 " dense_nanos="+dense_nanos);
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
