/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib;

import java.util.Hashtable;


/**
 *
 * @author itc
 */
public interface RandomArgMakerClonableIntf extends RandomArgMakerIntf {
	public RandomArgMakerClonableIntf newInstance(Hashtable p) throws OptimizerException;
}

