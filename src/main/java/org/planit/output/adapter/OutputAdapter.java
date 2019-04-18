package org.planit.output.adapter;

import java.util.logging.Logger;

import org.planit.demand.Demands;
import org.planit.trafficassignment.TrafficAssignment;

/**
 * OutputAdapter class which wraps around a traffic assignment class to provide selective access to its data for persistence
 * 
 * @author markr
 *
 */
public class OutputAdapter {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(OutputAdapter.class.getName());
    
    /**
     * the traffic assignment this adapter provides selective access to for persistence
     */
    protected final TrafficAssignment trafficAssignment;
    
    
    /** OutputAdapter wrapping around a certain traffic assignment instance to provide selective access to data
     * @param trafficAssignment
     */
    public OutputAdapter(TrafficAssignment trafficAssignment) {
        this.trafficAssignment = trafficAssignment;
    }

}
