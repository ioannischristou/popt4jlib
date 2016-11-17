package popt4jlib.GradientDescent;

import popt4jlib.*;
import java.util.*;

/**
 * interface for local-optimization methods (methods seeking a local minimum
 * in the neighborhood of a "starting point" solution).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface LocalOptimizerIntf extends OptimizerIntf {
  public LocalOptimizerIntf newInstance();
  public HashMap getParams();
  public void setParams(HashMap params) throws OptimizerException;
}

