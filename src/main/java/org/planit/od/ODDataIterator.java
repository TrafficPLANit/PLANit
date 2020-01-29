package org.planit.od;

import java.util.Iterator;

import org.planit.utils.network.virtual.Zone;

/**
 * Defines methods for an iterator which moves through a matrix of origin-destination cells
 * 
 * @author gman6028
 *
 * @param <T> the type of data stored for each origin-destination cell
 */
public interface ODDataIterator<T> extends Iterator<T>{

	/**
	 * Returns the origin zone object for the current cell
	 * 
	 * @return the origin zone object at the current cell
	 */
    public Zone getCurrentOrigin();
    
    /**
     * Returns the destination zone object for the current cell
     * 
     * @return the destination zone object for the current cell
     */
    public Zone getCurrentDestination();
    
    /**
     * Returns the value at the current cell
     * 
     * @return the value at the current cell
     */
    public T getCurrentValue();
}