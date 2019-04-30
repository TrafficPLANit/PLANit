package org.planit.output.adapter;

import java.util.logging.Logger;

import org.planit.trafficassignment.TraditionalStaticAssignment;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class relevant for link outputs
 * without exposing the internals of the traffic assignment class itself
 * 
 * @author markr
 *
 */
public class TraditionalStaticAssignmentLinkOutputAdapter extends OutputAdapter {

    private static final Logger LOGGER = Logger.getLogger(TraditionalStaticAssignmentLinkOutputAdapter.class.getName());    
        
/** 
 * Constructor
 *
 * @param trafficAssignment     TraditionalStaticAssignment object which this adapter wraps
 */
    public TraditionalStaticAssignmentLinkOutputAdapter(TraditionalStaticAssignment trafficAssignment) {
        super(trafficAssignment);
    }

/**
 * Returns the network segment flows calculated for this assignment
 * 
 * @return        array storing the calculated network segment flows
 */
    public double[] getTotalNetworkSegmentFlows() {
            return ((TraditionalStaticAssignment) trafficAssignment).getTotalNetworkSegmentFlows();
    }

/**
 * Returns whether the current assignment has converged
 * 
 * @return       true if the current assignment has converged, false otherwise
 */
    @Override
    public boolean isConverged() {
        return ((TraditionalStaticAssignment) trafficAssignment).getSimulationData().isConverged();
    }
        
}
