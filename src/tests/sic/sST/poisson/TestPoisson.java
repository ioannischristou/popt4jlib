/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests.sic.sST.poisson;

import cern.jet.random.Poisson;

/**
 *
 * @author itc
 */
public class TestPoisson {
	public static void main(String[] args) {
		double v = sSTCpoisson.complpoisscdf(new Poisson2(1), 15, 10);
		System.out.println("v="+v);
		Poisson p = new Poisson2(0);
		v = p.pdf(0);
		System.out.println("v="+v);
		p.setMean(0.005);
		v = p.pdf(0);
		System.out.println("v="+v);
		
	}
}
