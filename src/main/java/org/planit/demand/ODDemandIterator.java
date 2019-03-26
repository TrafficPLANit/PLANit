package org.planit.demand;

import java.util.Iterator;

import org.planit.utils.Pair;

/**
 * Iterator over od demand entries allowing one to collect the current origin and destination zone id
 * @author markr
 */
public interface ODDemandIterator extends Iterator<Double>{
		
	public abstract int getCurrentOriginId();
	
	public abstract int getCurrentDestinationId();
	
	public abstract Pair<Integer,Integer> getCurrentODPair();
}