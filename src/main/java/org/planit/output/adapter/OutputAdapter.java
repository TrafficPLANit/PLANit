package org.planit.output.adapter;

import java.util.logging.Logger;

import org.planit.cost.Cost;
import org.planit.network.physical.LinkSegment;
import org.planit.network.transport.TransportNetwork;
import org.planit.trafficassignment.TrafficAssignment;

/**
 * Adapter providing access to the data of a TrafficAssignment class relevant
 * for link outputs without exposing the internals of the traffic assignment
 * class itself
 * 
 * @author markr
 *
 */
public abstract class OutputAdapter {

    private static final Logger LOGGER = Logger.getLogger(OutputAdapter.class.getName());

    /**
     * the traffic assignment this output adapter is drawing from
     */
    protected final TrafficAssignment trafficAssignment;

    /**
     * Constructor
     * 
     * @param trafficAssignment
     *            TrafficAssignment object which this adapter wraps
     */
    public OutputAdapter(TrafficAssignment trafficAssignment) {
        this.trafficAssignment = trafficAssignment;
    }

    /**
     * Return the combined transport network for this assignment
     * 
     * @return TransportNetwork used in this assignment
     */
    public TransportNetwork getTransportNetwork() {
        return trafficAssignment.getTransportNetwork();
    }

    /**
     * Return the physical cost object used in this assignment
     * 
     * @return physical cost object used in this assignment
     */
    public Cost<LinkSegment> getPhysicalCost() {
        return trafficAssignment.getPhysicalCost();
    }

    /**
     * Return the id of this assignment run
     * 
     * @return id of this assignment run
     */
    public long getTrafficAssignmentId() {
        return trafficAssignment.getId();
    }

    /**
     * Returns whether the current assignment has converged
     * 
     * @return true if the current assignment has converged, false otherwise
     */
    public abstract boolean isConverged();

}