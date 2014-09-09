package parallel;
import java.io.Serializable;


public class LTTETest {
  public LTTETest() {
  }

  public static void main(String[] args) {
    LimitedTimeTaskExecutor lte = new LimitedTimeTaskExecutor(10);
    for (long i=2000; i<5000; i++) {
      ComputeTask t = new ComputeTask(i);
      lte.execute(t);
      System.out.println("t("+i+")="+t.getObjValue()+" (tot. threads="+lte.getNumThreads()+")");
    }
  }
}

class ComputeTask implements TaskObject {
  private final static long serialVersionUID = 1748878957227728243L;
  private double _val = Double.NaN;
  private long _n;
  private boolean _isDone=false;

  public ComputeTask(long n) { _n = n; }
  public Serializable run() {
    // a long compute intensive task
    double val=1.0;
    for (int i=0; i<_n; i++) {
      val += Math.sin(val)*i;
      if (Math.abs(Math.sin(val))<1.e-15) val=1.0;  // reset
    }
    setObjValue(val);
    setDone(true);
    return this;
  }

  public synchronized boolean isDone() { return _isDone; }
  public synchronized void copyFrom(TaskObject t) throws IllegalArgumentException {
    throw new IllegalArgumentException("copyFrom(t) method not supported");
  }
  public synchronized double getObjValue() { return _val; }
  synchronized void setObjValue(double v) { _val = v; }
  synchronized void setDone(boolean v) { _isDone = v; }

}
