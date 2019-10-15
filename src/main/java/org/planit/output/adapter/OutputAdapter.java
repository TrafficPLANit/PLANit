package org.planit.output.adapter;

import java.util.SortedSet;
import java.util.TreeSet;

import org.planit.data.SimulationData;
import org.planit.network.transport.TransportNetwork;
import org.planit.output.property.BaseOutputProperty;
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

	/**
	 * Output properties to be included in the CSV output files
	 */
	protected SortedSet<BaseOutputProperty> outputProperties;

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
		outputProperties = new TreeSet<BaseOutputProperty>();
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
	 * Add an output property
	 * 
	 * @param outputProperty output property to be added
	 */
	public void addProperty(BaseOutputProperty outputProperty) {
		outputProperties.add(outputProperty);
	}

	/**
	 * Remove an output property
	 * 
	 * @param outputProperty the output property to be removed
	 * @return true if the property was removed successfully
	 */
	public boolean removeProperty(BaseOutputProperty outputProperty) {
		return outputProperties.remove(outputProperty);
	}

	/**
	 * Returns the Set of output properties currently being used
	 * 
	 * @return Set of output properties currently being used
	 */
	public SortedSet<BaseOutputProperty> getOutputProperties() {
		return outputProperties;
	}

	/**
	 * Removes all output properties 
	 */
	public void removeAllProperties() {
		outputProperties.clear();
	}
	
	/**
	 * Tests whether a specified output property is being used
	 * 
	 * @param baseOutputProperty the output property being tested for
	 * @return true if the output property is in use, false otherwise
	 */
	public boolean containsProperty(BaseOutputProperty outputProperty) {
		return outputProperties.contains(outputProperty);
	}
	
	/**
	 * Returns the current iteration index of the simulation
	 * 
	 * @return index of the current iteration
	 */
	public int getIterationIndex() {
		return getSimulationData().getIterationIndex();
	}

   /**
     * Returns whether the current assignment has converged
     * 
     * @return true if the current assignment has converged, false otherwise
     */
    public abstract boolean isConverged();
    
}