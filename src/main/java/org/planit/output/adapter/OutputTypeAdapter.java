package org.planit.output.adapter;

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
public abstract class OutputTypeAdapter {

    /**
     * the traffic assignment this output adapter is drawing from
     */
    protected final TrafficAssignment trafficAssignment;
    
	/**
	 * Return the name of a Java object class as a short string
	 * 
	 * @param object the Java object
	 * @return the name of the object
	 */
	protected String getClassName(Object object) {
		String name = object.getClass().getCanonicalName();
		String[] words = name.split("\\.");
		return words[words.length - 1];
	}

    /**
     * Constructor
     * 
     * @param trafficAssignment TrafficAssignment object which this adapter wraps
     */
    public OutputTypeAdapter(TrafficAssignment trafficAssignment) {
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
     * Return the id of this assignment run
     * 
     * @return id of this assignment run
     */
    public long getTrafficAssignmentId() {
         return trafficAssignment.getId();
    }
    
    /**
     * Returns the name of the assignment class being used
     * 
     * @return the assignment class being used
     */
    public String getAssignmentClassName() {
    	return getClassName(trafficAssignment);
    }
    
    /**
     * Returns the name of the physical cost class
     * 
     * @return the name of the physical cost class
     */
    public String getPhysicalCostClassName() {
    	return getClassName(trafficAssignment.getPhysicalCost());
    }
    
    /**
     * Return the name of the virtual cost class
     * 
     * @return the name of the virtual cost class
     */
    public String getVirtualCostClassName() {
    	return getClassName(trafficAssignment.getVirtualCost());
    }

	/**
	 * Returns the current iteration index of the simulation
	 * 
	 * @return index of the current iteration
	 */
	public int getIterationIndex() {
		return trafficAssignment.getSimulationData().getIterationIndex();
	}	
	
}