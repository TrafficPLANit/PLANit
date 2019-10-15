package org.planit.odmatrix;

import java.util.Iterator;

import org.planit.utils.Pair;

/**
 * Iterator over OD matrix entries allowing one to collect the current origin
 * and destination zone id and current value
 * 
 * @author markr
 */
public interface ODMatrixIterator extends Iterator<Double> {

    /**
     * Get the id of the origin of the current cell
     * 
     * @return id of the origin of the current cell
     */
    public abstract int getCurrentOriginId();

    /**
     * Get the id of the destination of the current cell
     * 
     * @return id of the destination of the current cell
     */
    public abstract int getCurrentDestinationId();

    /**
     * Return the origin and destination of the current cell as a Pair
     * 
     * @return Pair containing the ids of the origin and destination of the current
     *         cell
     */
    public abstract Pair<Integer, Integer> getCurrentODPair();
    
    /**
     * Returns the current cell value without advancing the iteration
     * 
     * @return the current cell value
     */
    public abstract double getCurrentValue();
}