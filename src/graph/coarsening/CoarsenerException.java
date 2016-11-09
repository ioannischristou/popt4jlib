package graph.coarsening;

public class CoarsenerException extends Exception{
  public CoarsenerException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}
