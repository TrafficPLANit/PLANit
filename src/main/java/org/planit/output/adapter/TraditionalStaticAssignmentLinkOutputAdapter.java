package org.planit.output.adapter;

import org.planit.trafficassignment.TrafficAssignment;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class relevant for link outputs
 * without exposing the internals of the traffic assignment class itself
 * 
 * @author markr
 *
 */
public class TraditionalStaticAssignmentLinkOutputAdapter extends OutputAdapter {

    /** Constructor
     * 
     * @param trafficAssignment
     */
    public TraditionalStaticAssignmentLinkOutputAdapter(TrafficAssignment trafficAssignment) {
        super(trafficAssignment);
    }

}
