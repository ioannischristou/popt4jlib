package tests;

/**
 * shows how reg-exp's work from the command-line.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RegExTest {
	/**
	 * invoke as: 
	 * <CODE>
	 * java -cp &lt;classpath&gt; tests.RegExTest [text] [regexp] [replacement]
	 * </CODE>.
	 * @param args String[] 
	 */
	public static void main(String[] args) {
		String text = "I am here+, where are though?";
		if (args.length>0) text = args[0];
		String re = "\\?";  // "\?" in the command-line without the quotes works
		if (args.length>1) re = args[1];
		String rplc = "0";
		if (args.length>2) rplc = args[2];
		String repl = text.replaceAll(re, rplc);
		System.out.println("original string="+text);
		System.out.println("replaced string="+repl);
	}
}
