package popt4jlib.PS;

import java.util.HashMap;
import java.util.List;
import utils.RndUtil;

/**
 * class implements a random selection procedure: a random individual is 
 * selected from the island sub-population, and returned as "guiding" solution.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RandomChromosomeSelector implements ChromosomeSelectorIntf {
	/**
	 * return a random individual (other than i unless i is the only member in the 
	 * population).
	 * @param individuals List  // List&lt;DPSOIndividual&gt;
	 * @param i int
	 * @param gen int unused
	 * @param params HashMap must contain an entry &lt;"thread.id",Integer val&gt; 
	 * @return DPSOIndividual
	 */
	public DPSOIndividual getBestIndividual(List individuals, int i, int gen, HashMap params) {
		final int indsz = individuals.size();
		final int id = ((Integer) params.get("thread.id")).intValue();
		final RndUtil rndgen = RndUtil.getInstance(id);
		int j = rndgen.getRandom().nextInt(indsz);
		if (j==i) {
			if (i<indsz/2) ++j;
			else if (i>0) --j;  // if i==0 && i>=indsz/2 ==> indsz <= 1 thus keep j=0
		}
		DPSOIndividual ret = (DPSOIndividual) individuals.get(j);
		return ret;
	}
}

