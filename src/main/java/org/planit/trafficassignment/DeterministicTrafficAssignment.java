package org.planit.trafficassignment;

/**
 * A deterministic (static) traffic assignment base class. This means that we
 * assume we adhere to Wardop's First Principle where every driver has perfect
 * knowledge of the cost and therefore always chooses the actual shortest path
 * when making a path choice
 * 
 * @author markr
 *
 */
public abstract class DeterministicTrafficAssignment extends TrafficAssignment {

    /**
     * Base constructor
     */
    public DeterministicTrafficAssignment() {
        super();
    }

}
