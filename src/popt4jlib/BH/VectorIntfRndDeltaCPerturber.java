/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib.BH;

import java.util.ArrayList;
import java.util.Collections;
import popt4jlib.OptimizerException;
import popt4jlib.VectorIntf;
import utils.LightweightParams;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


/**
 * implements the <CODE>ChromosomePerturberIntf</CODE> interface for chromosomes
 * represented as <CODE>popt4jlib.VectorIntf</CODE> objects, whereby every
 * component x_i of the argument, is perturbed by some value that is chosen 
 * uniformly at random from the interval [max(min(x_i),x_i-&delta;),
 * min(max(x_i),x_i+&delta;)].
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class VectorIntfRndDeltaCPerturber 
    implements ChromosomePerturberIntf {
	
	/**
	 * sole constructor (no-op).
	 */
	public VectorIntfRndDeltaCPerturber() {
		// no-op
	}
	
	
	/**
	 * produce a perturbation of the argument x0.
	 * @param x0 Object // VectorIntf
	 * @param params HashMap must contain the following key-value pairs:
	 * <ul>
	 * <li> &lt;"dgabh.delta",$value$&gt; mandatory, the maximum delta 
	 * perturbation that may occur for any vector component.
	 * <li> &lt;"dgabh.maxperturbations",$num$&gt; optional, the maximum number
	 * of components of the input vector that may be modified. Default is the 
	 * length of the vector.
   * <li> &lt;"[dgabh.]minallelevalue", $value$&gt; optional, the minimum value 
	 * for any allele in the chromosome. Default -Infinity.
   * <li> &lt;"[dgabh.]minallelevalue$i$", $value$&gt; optional, the minimum 
	 * value for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}. If this value is less than the global value
   * specified by the "[dgabh.]minallelevalue" key, it is ignored.
   * <li> &lt;"[dgabh.]maxallelevalue", $value$&gt; optional, the maximum value 
	 * for any allele in the chromosome. Default +Infinity.
   * <li> &lt;"[dgabh.]maxallelevalue$i$", $value$&gt; optional, the max value 
	 * for the i-th allele in the chromosome ($i$ must be in the range
   * {0,...,chromosome_length-1}. If this value is greater than the global value
   * specified by the "[dgabh.]maxallelevalue" key, it is ignored.
   * <li> &lt;"thread.id",$integer_value"&gt; mandatory, the (internal) id of 
   * thread invoking this method; this number is used so as to look-up the right
   * random-number generator associated with the current thread.
	 * </ul>
   * <p>The "local" constraints can only impose more strict constraints on the
   * variables, but cannot be used to "over-ride" a global constraint to make
   * the domain of the variable wider.</p>
	 * @return Object // VectorIntf
	 * @throws IllegalArgumentException if the params are not appropriately set
	 * @throws OptimizerException if the dgabh.delta key-value pair is missing
	 */
	public Object perturb(Object x0, HashMap params) throws OptimizerException {
		if (!params.containsKey("dgabh.delta")) 
			throw new OptimizerException("dgabh.delta key is missing");
		try {
			VectorIntf x = ((VectorIntf) x0).newInstance();
			LightweightParams p = new LightweightParams(params);
			double delta = ((Double) params.get("dgabh.delta")).doubleValue();
			int threadid = ((Integer) params.get("thread.id")).intValue();
			Random r = utils.RndUtil.getInstance(threadid).getRandom();
			int x0len = x.getNumCoords();
			int max_num_perturbations = x0len;
			if (params.containsKey("dgabh.maxperturbations")) 
				max_num_perturbations = 
					((Integer) params.get("dgabh.maxperturbations")).intValue();
			Double maxalleleD = p.getDouble("dgabh.maxallelevalue");
			double maxallele = Double.POSITIVE_INFINITY;
			if (maxalleleD!=null) maxallele = maxalleleD.doubleValue();
			Double minalleleD = p.getDouble("dgabh.minallelevalue");
			double minallele = Double.NEGATIVE_INFINITY;
			if (minalleleD!=null) minallele = minalleleD.doubleValue();
			int num_perturbations = r.nextInt(max_num_perturbations+1);
			List ord = new ArrayList(x0len);
			for (int i=0; i<x0len; i++) ord.add(new Integer(i));
			Collections.shuffle(ord, r);
			for (int j=0; j<num_perturbations; j++) {
				int i = ((Integer) ord.get(j)).intValue();
				double xi = x.getCoord(i);
				Double minaviD = p.getDouble("dgabh.minallelevalue"+i);
				double minavi = minallele;
				if (minaviD!=null && minaviD.doubleValue()>minavi) 
					minavi = minaviD.doubleValue();
				if (xi-delta>=minavi) minavi = xi-delta;
				Double maxaviD = p.getDouble("dgabh.maxallelevalue"+i);
				double maxavi = maxallele;
				if (maxaviD!=null && maxaviD.doubleValue()<maxavi) 
					maxavi = maxaviD.doubleValue();
				if (xi+delta<=maxavi) maxavi = xi+delta;
				xi = r.nextDouble()*(maxavi-minavi) + minavi;
				x.setCoord(i, xi);
			}
			return x;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException(
				"VectorIntfRndDeltaCPerturber.perturb(): illegal arguments");
		}
	}
}
