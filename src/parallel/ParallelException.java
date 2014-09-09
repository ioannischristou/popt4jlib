package parallel;

public class ParallelException extends Exception {
  public ParallelException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}
