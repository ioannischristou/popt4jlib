/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel.distributed;

import parallel.ConditionCounter;

/**
 * wrapper singleton class for <CODE>ConditionCounter</CODE> objects. Only a 
 * single <CODE>DConditionCounter</CODE> per JVM may exist. Needed so that 
 * DCondCounter[Incr|Decr]Request objects may get access to the single 
 * <CODE>ConditionCounter</CODE> residing on the associated 
 * <CODE>DConditionCounterSrv</CODE> server on which they execute.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DConditionCounter {
	private final static DConditionCounter _instance = new DConditionCounter();
	private final ConditionCounter _counter;
	
	
	public static DConditionCounter getInstance() {
		return _instance;
	}
	
	
	public void increment() {
		_counter.increment();
	}
	
	
	public void increment(int num) {
		_counter.add(num);
	}
	
	
	public void decrement() {
		_counter.decrement();
	}
	
	
	public void await() {
		_counter.await();
	}
	
	
	public void reset() {
		_counter.reset();
	}
	
	
	public void shutDown() {
		System.exit(0);
	}
	
	
	private DConditionCounter() {
		_counter = new ConditionCounter();
	}
	
}
