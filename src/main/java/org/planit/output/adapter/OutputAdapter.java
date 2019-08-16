package org.planit.output.adapter;

import java.util.logging.Logger;

import org.planit.data.SimulationData;
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
     * Return the id of this assignment run
     * 
     * @return id of this assignment run
     */
    public long getTrafficAssignmentId() {
        return trafficAssignment.getId();
    }
    
    /**
     * Returns the simulation data for the current iteration
     * 
     * @return the simulation data for the current iteration
     */
    public SimulationData getSimulationData() {
    	return trafficAssignment.getSimulationData();
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
     * Returns whether the current assignment has converged
     * 
     * @return true if the current assignment has converged, false otherwise
     */
    public abstract boolean isConverged();

}