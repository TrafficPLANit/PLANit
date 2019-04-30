package org.planit.output.adapter;

import java.util.Set;
import java.util.logging.Logger;

import org.planit.cost.Cost;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.network.transport.TransportNetwork;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

/**
 * Adapter providing access to the data of a TrafficAssignment class relevant for link outputs
 * without exposing the internals of the traffic assignment class itself
 * 
 * @author markr
 *
 */
public abstract class OutputAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(OutputAdapter.class.getName());    
    
    protected final TrafficAssignment trafficAssignment;
   
/**
 * Constructor
 * 
 * @param trafficAssignment         TrafficAssignment object which this adapter wraps
 */
    public OutputAdapter(TrafficAssignment trafficAssignment) {
         this.trafficAssignment = trafficAssignment;
    }

/**
 * Return the combined transport network for this assignment
 * 
 * @return             TransportNetwork used in this assignment      
 */
    public TransportNetwork getTransportNetwork() {
            return trafficAssignment.getTransportNetwork();
    }

/**
 * Return the physical cost object used in this assignment
 * 
 * @return      physical cost object used in this assignment
 */
    public Cost<LinkSegment> getPhysicalCost() {
          return trafficAssignment.getPhysicalCost();
    }

/**
 * Return the total network segment costs calculated for this assignment
 * 
 * @param modes                      Set of modes for the current assignment
 * @return                                  array storing the calculated network segment costs
 * @throws PlanItException       thrown if there is an error
 */
    public double[] getTotalNetworkSegmentCosts(Set<Mode> modes) throws PlanItException {
          return trafficAssignment.getTotalNetworkSegmentCosts(modes);
    }

/**
 * Return the id of this assignment run
 * 
 * @return       id of this assignment run
 */
    public long getRunId() {
           return trafficAssignment.getId();
    }
        
/**
 * Returns whether the current assignment has converged
 * 
 * @return       true if the current assignment has converged, false otherwise
 */
    public abstract boolean isConverged();
        
}
