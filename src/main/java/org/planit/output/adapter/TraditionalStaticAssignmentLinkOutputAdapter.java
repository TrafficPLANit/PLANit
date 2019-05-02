package org.planit.output.adapter;

import java.util.Set;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.userclass.Mode;

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
  * Return the total network segment costs calculated for this assignment
  * 
  * @param modes                      Set of modes for the current assignment
  * @return                                  array storing the calculated network segment costs
  * @throws PlanItException       thrown if there is an error
  */
//    public double[] getTotalNetworkSegmentCosts(Set<Mode> modes) throws PlanItException {
//         return ((TraditionalStaticAssignment) trafficAssignment).getTotalNetworkSegmentCosts(modes);
//    }

    public double[] getNetworkSegmentCosts(Mode mode) throws PlanItException {
        return ((TraditionalStaticAssignment) trafficAssignment).getNetworkSegmentCosts(mode); 
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
