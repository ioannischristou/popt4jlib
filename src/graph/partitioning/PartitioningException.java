package graph.partitioning;

public class PartitioningException extends Exception {
  public PartitioningException(String msg) {
    System.err.println(msg);
    System.err.flush();
  }
}
